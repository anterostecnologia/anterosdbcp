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

package br.com.anteros.dbcp;


import static br.com.anteros.dbcp.pool.AnterosDBCPPool.POOL_NORMAL;

import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.dbcp.metrics.MetricsTrackerFactory;
import br.com.anteros.dbcp.pool.AnterosDBCPPool;
import br.com.anteros.dbcp.pool.AnterosDBCPPool.PoolInitializationException;

/**
 * The AnterosDBCPCP pooled DataSource.
 *
 * @author Brett Wooldridge
 */
public class AnterosDBCPDataSource extends AnterosDBCPConfig implements DataSource, Closeable
{
   private static final Logger LOGGER = LoggerProvider.getInstance().getLogger(AnterosDBCPDataSource.class.getName());

   private final AtomicBoolean isShutdown = new AtomicBoolean();

   private final AnterosDBCPPool fastPathPool; 
   private volatile AnterosDBCPPool pool;

   /**
    * Default constructor.  Setters are used to configure the pool.  Using
    * this constructor vs. {@link #AnterosDBCPDataSource(AnterosDBCPConfig)} will
    * result in {@link #getConnection()} performance that is slightly lower
    * due to lazy initialization checks.
    *
    * The first call to {@link #getConnection()} starts the pool.  Once the pool
    * is started, the configuration is "sealed" and no further configuration
    * changes are possible -- except via {@link AnterosDBCPConfigMXBean} methods.
    */
   public AnterosDBCPDataSource()
   {
      super();
      fastPathPool = null;
   }

   /**
    * Construct a AnterosDBCPDataSource with the specified configuration.  The
    * {@link AnterosDBCPConfig} is copied and the pool is started by invoking this
    * constructor.
    *
    * The {@link AnterosDBCPConfig} can be modified without affecting the AnterosDBCPDataSource
    * and used to initialize another AnterosDBCPDataSource instance.
    *
    * @param configuration a AnterosDBCPConfig instance
    */
   public AnterosDBCPDataSource(AnterosDBCPConfig configuration)
   {
      configuration.validate();
      configuration.copyStateTo(this);

      LOGGER.info("{} - Starting...", configuration.getPoolName());
      pool = fastPathPool = new AnterosDBCPPool(this);
      LOGGER.info("{} - Start completed.", configuration.getPoolName());

      this.seal();
   }

   // ***********************************************************************
   //                          DataSource methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public Connection getConnection() throws SQLException
   {
      if (isClosed()) {
         throw new SQLException("AnterosDBCPDataSource " + this + " has been closed.");
      }

      if (fastPathPool != null) {
         return fastPathPool.getConnection();
      }

      // See http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
      AnterosDBCPPool result = pool;
      if (result == null) {
         synchronized (this) {
            result = pool;
            if (result == null) {
               validate();
               LOGGER.info("{} - Starting...", getPoolName());
               try {
                  pool = result = new AnterosDBCPPool(this);
                  this.seal();
               }
               catch (PoolInitializationException pie) {
                  if (pie.getCause() instanceof SQLException) {
                     throw (SQLException) pie.getCause();
                  }
                  else {
                     throw pie;
                  }
               }
               LOGGER.info("{} - Start completed.", getPoolName());
            }
         }
      }

      return result.getConnection();
   }

   /** {@inheritDoc} */
   @Override
   public Connection getConnection(String username, String password) throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   /** {@inheritDoc} */
   @Override
   public PrintWriter getLogWriter() throws SQLException
   {
      AnterosDBCPPool p = pool;
      return (p != null ? p.getUnwrappedDataSource().getLogWriter() : null);
   }

   /** {@inheritDoc} */
   @Override
   public void setLogWriter(PrintWriter out) throws SQLException
   {
      AnterosDBCPPool p = pool;
      if (p != null) {
         p.getUnwrappedDataSource().setLogWriter(out);
      }
   }

   /** {@inheritDoc} */
   @Override
   public void setLoginTimeout(int seconds) throws SQLException
   {
      AnterosDBCPPool p = pool;
      if (p != null) {
         p.getUnwrappedDataSource().setLoginTimeout(seconds);
      }
   }

   /** {@inheritDoc} */
   @Override
   public int getLoginTimeout() throws SQLException
   {
      AnterosDBCPPool p = pool;
      return (p != null ? p.getUnwrappedDataSource().getLoginTimeout() : 0);
   }

   /** {@inheritDoc} */
   @Override
   public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
   {
      throw new SQLFeatureNotSupportedException();
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(this)) {
         return (T) this;
      }

      AnterosDBCPPool p = pool;
      if (p != null) {
         final DataSource unwrappedDataSource = p.getUnwrappedDataSource();
         if (iface.isInstance(unwrappedDataSource)) {
            return (T) unwrappedDataSource;
         }

         if (unwrappedDataSource != null) {
            return unwrappedDataSource.unwrap(iface);
         }
      }

      throw new SQLException("Wrapped DataSource is not an instance of " + iface);
   }

   /** {@inheritDoc} */
   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      if (iface.isInstance(this)) {
         return true;
      }

      AnterosDBCPPool p = pool;
      if (p != null) {
         final DataSource unwrappedDataSource = p.getUnwrappedDataSource();
         if (iface.isInstance(unwrappedDataSource)) {
            return true;
         }

         if (unwrappedDataSource != null) {
            return unwrappedDataSource.isWrapperFor(iface);
         }
      }

      return false;
   }

   // ***********************************************************************
   //                        AnterosDBCPConfigMXBean methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public void setMetricRegistry(Object metricRegistry)
   {
      boolean isAlreadySet = getMetricRegistry() != null;
      super.setMetricRegistry(metricRegistry);

      AnterosDBCPPool p = pool;
      if (p != null) {
         if (isAlreadySet) {
            throw new IllegalStateException("MetricRegistry can only be set one time");
         }
         else {
            p.setMetricRegistry(super.getMetricRegistry());
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public void setMetricsTrackerFactory(MetricsTrackerFactory metricsTrackerFactory)
   {
      boolean isAlreadySet = getMetricsTrackerFactory() != null;
      super.setMetricsTrackerFactory(metricsTrackerFactory);

      AnterosDBCPPool p = pool;
      if (p != null) {
         if (isAlreadySet) {
            throw new IllegalStateException("MetricsTrackerFactory can only be set one time");
         }
         else {
            p.setMetricsTrackerFactory(super.getMetricsTrackerFactory());
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public void setHealthCheckRegistry(Object healthCheckRegistry)
   {
      boolean isAlreadySet = getHealthCheckRegistry() != null;
      super.setHealthCheckRegistry(healthCheckRegistry);

      AnterosDBCPPool p = pool;
      if (p != null) {
         if (isAlreadySet) {
            throw new IllegalStateException("HealthCheckRegistry can only be set one time");
         }
         else {
            p.setHealthCheckRegistry(super.getHealthCheckRegistry());
         }
      }
   }

   // ***********************************************************************
   //                        AnterosDBCPCP-specific methods
   // ***********************************************************************

   /**
    * Returns {@code true} if the pool as been started and is not suspended or shutdown.
    *
    * @return {@code true} if the pool as been started and is not suspended or shutdown.
    */
   public boolean isRunning()
   {
      return pool != null && pool.poolState == POOL_NORMAL;
   }

   /**
    * Get the {@code AnterosDBCPPoolMXBean} for this AnterosDBCPDataSource instance.  If this method is called on
    * a {@code AnterosDBCPDataSource} that has been constructed without a {@code AnterosDBCPConfig} instance,
    * and before an initial call to {@code #getConnection()}, the return value will be {@code null}.
    *
    * @return the {@code AnterosDBCPPoolMXBean} instance, or {@code null}.
    */
   public AnterosDBCPPoolMXBean getAnterosDBCPPoolMXBean()
   {
      return pool;
   }

   /**
    * Get the {@code AnterosDBCPConfigMXBean} for this AnterosDBCPDataSource instance.
    *
    * @return the {@code AnterosDBCPConfigMXBean} instance.
    */
   public AnterosDBCPConfigMXBean getAnterosDBCPConfigMXBean()
   {
      return this;
   }

   /**
    * Evict a connection from the pool.  If the connection has already been closed (returned to the pool)
    * this may result in a "soft" eviction; the connection will be evicted sometime in the future if it is
    * currently in use.  If the connection has not been closed, the eviction is immediate.
    *
    * @param connection the connection to evict from the pool
    */
   public void evictConnection(Connection connection)
   {
      AnterosDBCPPool p;
      if (!isClosed() && (p = pool) != null && connection.getClass().getName().startsWith("br.com.anteros.dbcp")) {
         p.evictConnection(connection);
      }
   }

   /**
    * Shutdown the DataSource and its associated pool.
    */
   @Override
   public void close()
   {
      if (isShutdown.getAndSet(true)) {
         return;
      }

      AnterosDBCPPool p = pool;
      if (p != null) {
         try {
            LOGGER.info("{} - Shutdown initiated...", getPoolName());
            p.shutdown();
            LOGGER.info("{} - Shutdown completed.", getPoolName());
         }
         catch (InterruptedException e) {
            LOGGER.warn("{} - Interrupted during closing", getPoolName(), e);
            Thread.currentThread().interrupt();
         }
      }
   }

   /**
    * Determine whether the AnterosDBCPDataSource has been closed.
    *
    * @return true if the AnterosDBCPDataSource has been closed, false otherwise
    */
   public boolean isClosed()
   {
      return isShutdown.get();
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return "AnterosDBCPDataSource (" + pool + ")";
   }
}
