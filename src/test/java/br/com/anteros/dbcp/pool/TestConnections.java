/*
 * Copyright (C) 2013 Brett Wooldridge
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

import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;
import br.com.anteros.dbcp.AnterosDBCPPoolMXBean;
import br.com.anteros.dbcp.mocks.StubConnection;
import br.com.anteros.dbcp.mocks.StubDataSource;
import br.com.anteros.dbcp.mocks.StubStatement;
import br.com.anteros.dbcp.pool.AnterosDBCPPool.PoolInitializationException;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static br.com.anteros.dbcp.pool.TestElf.*;
import static br.com.anteros.dbcp.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

/**
 * @author Brett Wooldridge
 */
public class TestConnections
{
   @Before
   public void before()
   {
      setSlf4jTargetStream(AnterosDBCPPool.class, System.err);
      setSlf4jLogLevel(AnterosDBCPPool.class, Level.DEBUG);
      setSlf4jLogLevel(PoolBase.class, Level.DEBUG);
   }

   @After
   public void after()
   {
      System.getProperties().remove("br.com.anteros.dbcp.housekeeping.periodMs");
      setSlf4jLogLevel(AnterosDBCPPool.class, Level.WARN);
      setSlf4jLogLevel(PoolBase.class, Level.WARN);
   }

   @Test
   public void testCreate() throws SQLException
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setConnectionInitSql("SELECT 1");
      config.setReadOnly(true);
      config.setConnectionTimeout(2500);
      config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(30));
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         ds.setLoginTimeout(10);
         assertSame(10, ds.getLoginTimeout());

         AnterosDBCPPool pool = getPool(ds);
         ds.getConnection().close();
         assertSame("Total connections not as expected", 1, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 1, pool.getIdleConnections());

         try (Connection connection = ds.getConnection();
              PreparedStatement statement = connection.prepareStatement("SELECT * FROM device WHERE device_id=?")) {

            assertNotNull(connection);
            assertNotNull(statement);

            assertSame("Total connections not as expected", 1, pool.getTotalConnections());
            assertSame("Idle connections not as expected", 0, pool.getIdleConnections());

            statement.setInt(1, 0);

            try (ResultSet resultSet = statement.executeQuery()) {
               assertNotNull(resultSet);

               assertFalse(resultSet.next());
            }
         }

         assertSame("Total connections not as expected", 1, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 1, pool.getIdleConnections());
      }
   }

   @Test
   public void testMaxLifetime() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      System.setProperty("br.com.anteros.dbcp.housekeeping.periodMs", "100");

      setConfigUnitTest(true);
      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         System.clearProperty("br.com.anteros.dbcp.housekeeping.periodMs");

         getUnsealedConfig(ds).setMaxLifetime(700);

         AnterosDBCPPool pool = getPool(ds);

         assertSame("Total connections not as expected", 0, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 0, pool.getIdleConnections());

         Connection unwrap;
         Connection unwrap2;
         try (Connection connection = ds.getConnection()) {
            unwrap = connection.unwrap(Connection.class);
            assertNotNull(connection);

            assertSame("Second total connections not as expected", 1, pool.getTotalConnections());
            assertSame("Second idle connections not as expected", 0, pool.getIdleConnections());
         }

         assertSame("Idle connections not as expected", 1, pool.getIdleConnections());

         try (Connection connection = ds.getConnection()) {
            unwrap2 = connection.unwrap(Connection.class);
            assertSame(unwrap, unwrap2);
            assertSame("Second total connections not as expected", 1, pool.getTotalConnections());
            assertSame("Second idle connections not as expected", 0, pool.getIdleConnections());
         }

         quietlySleep(TimeUnit.SECONDS.toMillis(2));

         try (Connection connection = ds.getConnection()) {
            unwrap2 = connection.unwrap(Connection.class);
            assertNotSame("Expected a different connection", unwrap, unwrap2);
         }

         assertSame("Post total connections not as expected", 1, pool.getTotalConnections());
         assertSame("Post idle connections not as expected", 1, pool.getIdleConnections());
      }
      finally {
         setConfigUnitTest(false);
      }
   }

   @Test
   public void testMaxLifetime2() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      System.setProperty("br.com.anteros.dbcp.housekeeping.periodMs", "100");

      setConfigUnitTest(true);
      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         getUnsealedConfig(ds).setMaxLifetime(700);

         AnterosDBCPPool pool = getPool(ds);
         assertSame("Total connections not as expected", 0, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 0, pool.getIdleConnections());

         Connection unwrap;
         Connection unwrap2;
         try (Connection connection = ds.getConnection()) {
            unwrap = connection.unwrap(Connection.class);
            assertNotNull(connection);

            assertSame("Second total connections not as expected", 1, pool.getTotalConnections());
            assertSame("Second idle connections not as expected", 0, pool.getIdleConnections());
         }

         assertSame("Idle connections not as expected", 1, pool.getIdleConnections());

         try (Connection connection = ds.getConnection()) {
            unwrap2 = connection.unwrap(Connection.class);
            assertSame(unwrap, unwrap2);
            assertSame("Second total connections not as expected", 1, pool.getTotalConnections());
            assertSame("Second idle connections not as expected", 0, pool.getIdleConnections());
         }

         quietlySleep(800);

         try (Connection connection = ds.getConnection()) {
            unwrap2 = connection.unwrap(Connection.class);
            assertNotSame("Expected a different connection", unwrap, unwrap2);
         }

         assertSame("Post total connections not as expected", 1, pool.getTotalConnections());
         assertSame("Post idle connections not as expected", 1, pool.getIdleConnections());
      }
      finally {
         setConfigUnitTest(false);
      }
   }

   @Test
   public void testDoubleClose() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
         Connection connection = ds.getConnection()) {
         connection.close();

         // should no-op
         connection.abort(null);

         assertTrue("Connection should have closed", connection.isClosed());
         assertFalse("Connection should have closed", connection.isValid(5));
         assertTrue("Expected to contain ClosedConnection, but was " + connection, connection.toString().contains("ClosedConnection"));
      }
   }

   @Test
   public void testEviction() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         Connection connection = ds.getConnection();

         AnterosDBCPPool pool = getPool(ds);
         assertEquals(1, pool.getTotalConnections());
         ds.evictConnection(connection);
         assertEquals(0, pool.getTotalConnections());
      }
   }

   @Test
   public void testEvictAllRefill() throws Exception {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      System.setProperty("br.com.anteros.dbcp.housekeeping.periodMs", "100");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         AnterosDBCPPoolMXBean poolMXBean = ds.getAnterosDBCPPoolMXBean();

         while (poolMXBean.getIdleConnections() < 5) { // wait until the pool fills
            quietlySleep(100);
         }

         // Get and evict all the idle connections
         for (int i = 0; i < 5; i++) {
            final Connection conn = ds.getConnection();
            ds.evictConnection(conn);
         }

         assertTrue("Expected idle connections to be less than idle", poolMXBean.getIdleConnections() < 5);

         // Wait a bit
         quietlySleep(SECONDS.toMillis(2));

         int count = 0;
         while (poolMXBean.getIdleConnections() < 5 && count++ < 20) {
            quietlySleep(100);
         }

         // Assert that the pool as returned to 5 connections
         assertEquals("After eviction, refill did not reach expected 5 connections.", 5, poolMXBean.getIdleConnections());
      }
      finally {
         System.clearProperty("br.com.anteros.dbcp.housekeeping.periodMs");
      }
   }

   @Test
   public void testBackfill() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(4);
      config.setConnectionTimeout(1000);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      StubConnection.slowCreate = true;
      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {

         AnterosDBCPPool pool = getPool(ds);
         quietlySleep(1250);

         assertSame("Total connections not as expected", 1, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 1, pool.getIdleConnections());

         // This will take the pool down to zero
         try (Connection connection = ds.getConnection()) {
            assertNotNull(connection);

            assertSame("Total connections not as expected", 1, pool.getTotalConnections());
            assertSame("Idle connections not as expected", 0, pool.getIdleConnections());

            PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
            assertNotNull(statement);

            ResultSet resultSet = statement.executeQuery();
            assertNotNull(resultSet);

            try {
               statement.getMaxFieldSize();
               fail();
            }
            catch (Exception e) {
               assertSame(SQLException.class, e.getClass());
            }

            pool.logPoolState("testBackfill() before close...");

            // The connection will be ejected from the pool here
         }

         assertSame("Total connections not as expected", 0, pool.getTotalConnections());

         pool.logPoolState("testBackfill() after close...");

         quietlySleep(1250);

         assertSame("Total connections not as expected", 1, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 1, pool.getIdleConnections());
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void testMaximumPoolLimit() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(4);
      config.setConnectionTimeout(20000);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      final AtomicReference<Exception> ref = new AtomicReference<>();

      StubConnection.count.set(0); // reset counter

      try (final AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {

         final AnterosDBCPPool pool = getPool(ds);

         Thread[] threads = new Thread[20];
         for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
               try {
                  pool.logPoolState("Before acquire ");
                  try (Connection ignored = ds.getConnection()) {
                     pool.logPoolState("After  acquire ");
                     quietlySleep(500);
                  }
               }
               catch (Exception e) {
                  ref.set(e);
               }
            });
         }

         for (Thread thread : threads) {
            thread.start();
         }

         for (Thread thread : threads) {
            thread.join();
         }

         pool.logPoolState("before check ");
         assertNull((ref.get() != null ? ref.get().toString() : ""), ref.get());
         assertSame("StubConnection count not as expected", 4, StubConnection.count.get());
      }
   }

   @Test
   @SuppressWarnings("EmptyTryBlock")
   public void testOldDriver() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      StubConnection.oldDriver = true;
      StubStatement.oldDriver = true;
      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         quietlySleep(500);

         try (Connection ignored = ds.getConnection()) {
            // close
         }

         quietlySleep(500);
         try (Connection ignored = ds.getConnection()) {
            // close
         }
      }
      finally {
         StubConnection.oldDriver = false;
         StubStatement.oldDriver = false;
      }
   }

   @Test
   public void testSuspendResume() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(3);
      config.setConnectionTimeout(2500);
      config.setAllowPoolSuspension(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (final AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         AnterosDBCPPool pool = getPool(ds);
         while (pool.getTotalConnections() < 3) {
            quietlySleep(50);
         }

         Thread t = new Thread(() -> {
            try {
               ds.getConnection();
               ds.getConnection();
            }
            catch (Exception e) {
               fail();
            }
         });

         try (Connection ignored = ds.getConnection()) {
            assertEquals(2, pool.getIdleConnections());

            pool.suspendPool();
            t.start();

            quietlySleep(500);
            assertEquals(2, pool.getIdleConnections());
         }
         assertEquals(3, pool.getIdleConnections());
         pool.resumePool();
         quietlySleep(500);
         assertEquals(1, pool.getIdleConnections());
      }
   }

   @Test
   public void testSuspendResumeWithThrow() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(3);
      config.setConnectionTimeout(2500);
      config.setAllowPoolSuspension(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      System.setProperty("br.com.anteros.dbcp.throwIfSuspended", "true");
      try (final AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         AnterosDBCPPool pool = getPool(ds);
         while (pool.getTotalConnections() < 3) {
            quietlySleep(50);
         }

         AtomicReference<Exception> exception = new AtomicReference<>();
         Thread t = new Thread(() -> {
            try {
               ds.getConnection();
               ds.getConnection();
            }
            catch (Exception e) {
               exception.set(e);
            }
         });

         try (Connection ignored = ds.getConnection()) {
            assertEquals(2, pool.getIdleConnections());

            pool.suspendPool();
            t.start();

            quietlySleep(500);
            assertEquals(SQLTransientException.class, exception.get().getClass());
            assertEquals(2, pool.getIdleConnections());
         }

         assertEquals(3, pool.getIdleConnections());
         pool.resumePool();

         try (Connection ignored = ds.getConnection()) {
            assertEquals(2, pool.getIdleConnections());
         }
      }
      finally {
         System.getProperties().remove("br.com.anteros.dbcp.throwIfSuspended");
      }
   }

   @Test
   public void testInitializationFailure1()
   {
      StubDataSource stubDataSource = new StubDataSource();
      stubDataSource.setThrowException(new SQLException("Connection refused"));

      try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource()) {
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setConnectionTimeout(2500);
         ds.setConnectionTestQuery("VALUES 1");
         ds.setDataSource(stubDataSource);

         try (Connection ignored = ds.getConnection()) {
            fail("Initialization should have failed");
         }
         catch (SQLException e) {
            // passed
         }
      }
   }

   @Test
   public void testInitializationFailure2() throws SQLException
   {
      StubDataSource stubDataSource = new StubDataSource();
      stubDataSource.setThrowException(new SQLException("Connection refused"));

      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSource(stubDataSource);

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
           Connection ignored = ds.getConnection()) {
         fail("Initialization should have failed");
      }
      catch (PoolInitializationException e) {
         // passed
      }
   }

   @Test
   public void testInvalidConnectionTestQuery()
   {
      class BadConnection extends StubConnection {
         /** {@inheritDoc} */
         @Override
         public Statement createStatement() throws SQLException
         {
            throw new SQLException("Simulated exception in createStatement()");
         }
      }

      StubDataSource stubDataSource = new StubDataSource() {
         /** {@inheritDoc} */
         @Override
         public Connection getConnection()
         {
            return new BadConnection();
         }
      };

      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(3));
      config.setConnectionTestQuery("VALUES 1");
      config.setInitializationFailTimeout(TimeUnit.SECONDS.toMillis(2));
      config.setDataSource(stubDataSource);

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         try (Connection ignored = ds.getConnection()) {
            fail("getConnection() should have failed");
         }
         catch (SQLException e) {
            assertSame("Simulated exception in createStatement()", e.getNextException().getMessage());
         }
      }
      catch (PoolInitializationException e) {
         assertSame("Simulated exception in createStatement()", e.getCause().getMessage());
      }

      config.setInitializationFailTimeout(0);
      try (AnterosDBCPDataSource ignored = new AnterosDBCPDataSource(config)) {
         fail("Initialization should have failed");
      }
      catch (PoolInitializationException e) {
         // passed
      }
   }

   @Test
   public void testDataSourceRaisesErrorWhileInitializationTestQuery() throws SQLException
   {
      StubDataSourceWithErrorSwitch stubDataSource = new StubDataSourceWithErrorSwitch();
      stubDataSource.setErrorOnConnection(true);

      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSource(stubDataSource);

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
         Connection ignored = ds.getConnection()) {
         fail("Initialization should have failed");
      }
      catch (PoolInitializationException e) {
         // passed
      }
   }

   @Test
   public void testDataSourceRaisesErrorAfterInitializationTestQuery()
   {
      StubDataSourceWithErrorSwitch stubDataSource = new StubDataSourceWithErrorSwitch();

      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(3));
      config.setConnectionTestQuery("VALUES 1");
      config.setInitializationFailTimeout(TimeUnit.SECONDS.toMillis(2));
      config.setDataSource(stubDataSource);

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         // this will make datasource throws Error, which will become uncaught
         stubDataSource.setErrorOnConnection(true);
         try (Connection ignored = ds.getConnection()) {
            fail("SQLException should occur!");
         } catch (SQLException e) {
            // request will get timed-out
            assertTrue(e.getMessage().contains("request timed out"));
         }
      }
   }

   @Test
   public void testPopulationSlowAcquisition() throws InterruptedException, SQLException
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMaximumPoolSize(20);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      System.setProperty("br.com.anteros.dbcp.housekeeping.periodMs", "1000");

      StubConnection.slowCreate = true;
      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         System.clearProperty("br.com.anteros.dbcp.housekeeping.periodMs");

         getUnsealedConfig(ds).setIdleTimeout(3000);

         SECONDS.sleep(2);

         AnterosDBCPPool pool = getPool(ds);
         assertSame("Total connections not as expected", 2, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 2, pool.getIdleConnections());

         try (Connection connection = ds.getConnection()) {
            assertNotNull(connection);

            SECONDS.sleep(20);

            assertSame("Second total connections not as expected", 20, pool.getTotalConnections());
            assertSame("Second idle connections not as expected", 19, pool.getIdleConnections());
         }

         assertSame("Idle connections not as expected", 20, pool.getIdleConnections());

         SECONDS.sleep(5);

         assertSame("Third total connections not as expected", 20, pool.getTotalConnections());
         assertSame("Third idle connections not as expected", 20, pool.getIdleConnections());
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   @SuppressWarnings("EmptyTryBlock")
   public void testMinimumIdleZero() throws SQLException
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000L);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
           Connection ignored = ds.getConnection()) {
         // passed
      }
      catch (SQLTransientConnectionException sqle) {
         fail("Failed to obtain connection");
      }
   }

   class StubDataSourceWithErrorSwitch extends StubDataSource {
      private boolean errorOnConnection = false;

      /** {@inheritDoc} */
      @Override
      public Connection getConnection() throws SQLException {
         if (!errorOnConnection) {
            return new StubConnection();
         }

         throw new RuntimeException("Bad thing happens on datasource.");
      }

      public void setErrorOnConnection(boolean errorOnConnection) {
         this.errorOnConnection = errorOnConnection;
      }
   }

}
