/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

package br.com.anteros.dbcp.metrics.prometheus;

import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PrometheusHistogramMetricsTrackerTest {

   private CollectorRegistry collectorRegistry;

   private static final String POOL_LABEL_NAME = "pool";

   @Before
   public void setupCollectorRegistry(){
      this.collectorRegistry = new CollectorRegistry();
   }

   @Test
   public void recordConnectionTimeout() throws Exception {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(collectorRegistry));
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(250);

      String[] labelNames = {POOL_LABEL_NAME};
      String[] labelValues = {config.getPoolName()};

      try (AnterosDBCPDataSource hikariDataSource = new AnterosDBCPDataSource(config)) {
         try (Connection connection1 = hikariDataSource.getConnection();
              Connection connection2 = hikariDataSource.getConnection()) {
            try (Connection connection3 = hikariDataSource.getConnection()) {
            } catch (SQLTransientConnectionException ignored) {
            }
         }

         Double total = collectorRegistry.getSampleValue(
            "hikaricp_connection_timeout_total",
            labelNames,
            labelValues
         );
         assertThat(total, is(1.0));
      }
   }

   @Test
   public void connectionAcquisitionMetrics() {
      checkSummaryMetricFamily("hikaricp_connection_acquired_nanos");
   }

   @Test
   public void connectionUsageMetrics() {
      checkSummaryMetricFamily("hikaricp_connection_usage_millis");
   }

   @Test
   public void connectionCreationMetrics() {
      checkSummaryMetricFamily("hikaricp_connection_creation_millis");
   }

   @Test
   public void testMultiplePoolName() throws Exception {
      String[] labelNames = {POOL_LABEL_NAME};

      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(collectorRegistry));
      config.setPoolName("first");
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(250);
      String[] labelValues1 = {config.getPoolName()};

      try (AnterosDBCPDataSource ignored = new AnterosDBCPDataSource(config)) {
         assertThat(collectorRegistry.getSampleValue(
            "hikaricp_connection_timeout_total",
            labelNames,
            labelValues1), is(0.0));

         CollectorRegistry collectorRegistry2 = new CollectorRegistry();
         AnterosDBCPConfig config2 = newAnterosDBCPConfig();
         config2.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(collectorRegistry2));
         config2.setPoolName("second");
         config2.setJdbcUrl("jdbc:h2:mem:");
         config2.setMaximumPoolSize(4);
         config2.setConnectionTimeout(250);
         String[] labelValues2 = {config2.getPoolName()};

         try (AnterosDBCPDataSource ignored2 = new AnterosDBCPDataSource(config2)) {
            assertThat(collectorRegistry2.getSampleValue(
               "hikaricp_connection_timeout_total",
               labelNames,
               labelValues2), is(0.0));
         }
      }
   }

   private void checkSummaryMetricFamily(String metricName) {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(collectorRegistry));
      config.setJdbcUrl("jdbc:h2:mem:");

      try (AnterosDBCPDataSource ignored = new AnterosDBCPDataSource(config)) {
         Double count = collectorRegistry.getSampleValue(
            metricName + "_count",
            new String[]{POOL_LABEL_NAME},
            new String[]{config.getPoolName()}
         );
         assertNotNull(count);

         Double sum = collectorRegistry.getSampleValue(
            metricName + "_sum",
            new String[]{POOL_LABEL_NAME},
            new String[]{config.getPoolName()}
         );
         assertNotNull(sum);
      }
   }
}
