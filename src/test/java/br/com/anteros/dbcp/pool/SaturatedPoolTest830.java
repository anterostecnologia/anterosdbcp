/*
 * Copyright (C) 2017 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.anteros.dbcp.pool;

import static br.com.anteros.dbcp.pool.TestElf.getPool;
import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPConfig;
import static br.com.anteros.dbcp.pool.TestElf.setSlf4jLogLevel;
import static br.com.anteros.dbcp.util.ClockSource.currentTime;
import static br.com.anteros.dbcp.util.ClockSource.elapsedMillis;
import static br.com.anteros.dbcp.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.junit.Test;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;
import br.com.anteros.dbcp.mocks.StubConnection;
import br.com.anteros.dbcp.mocks.StubStatement;

/**
 * @author Brett Wooldridge
 */
public class SaturatedPoolTest830
{
   private static final Logger LOGGER = LoggerProvider.getInstance().getLogger(SaturatedPoolTest830.class.getName());
   private static final int MAX_POOL_SIZE = 10;

   @Test
   public void saturatedPoolTest() throws Exception {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(MAX_POOL_SIZE);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTimeout(1000);
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      StubConnection.slowCreate = true;
      StubStatement.setSimulatedQueryTime(1000);
      setSlf4jLogLevel(AnterosDBCPPool.class, Level.DEBUG);
      System.setProperty("br.com.anteros.dbcp.housekeeping.periodMs", "5000");

      final long start = currentTime();

      try (final AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
         ThreadPoolExecutor threadPool = new ThreadPoolExecutor( 50 /*core*/, 50 /*max*/, 2 /*keepalive*/, SECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());
         threadPool.allowCoreThreadTimeOut(true);

         AtomicInteger windowIndex = new AtomicInteger();
         boolean[] failureWindow = new boolean[100];
         Arrays.fill(failureWindow, true);

         // Initial saturation
         for (int i = 0; i < 50; i++) {
            threadPool.execute(() -> {
               try (Connection conn = ds.getConnection();
                    Statement stmt = conn.createStatement()) {
                  stmt.execute("SELECT bogus FROM imaginary");
               }
               catch (SQLException e) {
                  LOGGER.info(e.getMessage());
               }
            });
         }

         long sleep = 80;
outer:   while (true) {
            quietlySleep(sleep);

            if (elapsedMillis(start) > SECONDS.toMillis(12) && sleep < 100) {
               sleep = 100;
               LOGGER.warn("Switching to 100ms sleep");
            }
            else if (elapsedMillis(start) > SECONDS.toMillis(6) && sleep < 90) {
               sleep = 90;
               LOGGER.warn("Switching to 90ms sleep");
            }

            threadPool.execute(() -> {
               int ndx = windowIndex.incrementAndGet() % failureWindow.length;

               try (Connection conn = ds.getConnection();
                    Statement stmt = conn.createStatement()) {
                  stmt.execute("SELECT bogus FROM imaginary");
                  failureWindow[ndx] = false;
               }
               catch (SQLException e) {
                  LOGGER.info(e.getMessage());
                  failureWindow[ndx] = true;
               }
            });

            for (int i = 0; i < failureWindow.length; i++) {
               if (failureWindow[i]) {
                  if (elapsedMillis(start) % (SECONDS.toMillis(1) - sleep) < sleep) {
                     LOGGER.info("Active threads {}, submissions per second {}, waiting threads {}",
                                 threadPool.getActiveCount(),
                                 SECONDS.toMillis(1) / sleep,
                                 getPool(ds).getThreadsAwaitingConnection());
                  }
                  continue outer;
               }
            }

            LOGGER.info("Timeouts have subsided.");
            LOGGER.info("Active threads {}, submissions per second {}, waiting threads {}",
                        threadPool.getActiveCount(),
                        SECONDS.toMillis(1) / sleep,
                        getPool(ds).getThreadsAwaitingConnection());
            break;
         }

         LOGGER.info("Waiting for completion of {} active tasks.", threadPool.getActiveCount());
         while (getPool(ds).getActiveConnections() > 0) {
            quietlySleep(50);
         }

         assertEquals("Rate not in balance at 10req/s", SECONDS.toMillis(1) / sleep, 10L);
      }
      finally {
         StubStatement.setSimulatedQueryTime(0);
         StubConnection.slowCreate = false;
         System.clearProperty("br.com.anteros.dbcp.housekeeping.periodMs");
         setSlf4jLogLevel(AnterosDBCPPool.class, Level.INFO);
      }
   }
}
