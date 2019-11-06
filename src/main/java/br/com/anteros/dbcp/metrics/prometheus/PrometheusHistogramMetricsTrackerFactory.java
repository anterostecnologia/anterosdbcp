/*
 * Copyright (C) 2016 Brett Wooldridge
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

import br.com.anteros.dbcp.metrics.IMetricsTracker;
import br.com.anteros.dbcp.metrics.MetricsTrackerFactory;
import br.com.anteros.dbcp.metrics.PoolStats;
import io.prometheus.client.CollectorRegistry;

/**
 * <pre>{@code
 * AnterosDBCPConfig config = new AnterosDBCPConfig();
 * config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory());
 * }</pre>
 */
public class PrometheusHistogramMetricsTrackerFactory implements MetricsTrackerFactory {

   private AnterosDBCPCollector collector;

   private CollectorRegistry collectorRegistry;

   /**
    * Default Constructor. The AnterosDBCP metrics are registered to the default
    * collector registry ({@code CollectorRegistry.defaultRegistry}).
    */
   public PrometheusHistogramMetricsTrackerFactory() {
      this.collectorRegistry = CollectorRegistry.defaultRegistry;
   }

   /**
    * Constructor that allows to pass in a {@link CollectorRegistry} to which the
    * AnterosDBCP metrics are registered.
    */
   public PrometheusHistogramMetricsTrackerFactory(CollectorRegistry collectorRegistry) {
      this.collectorRegistry = collectorRegistry;
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats) {
      getCollector().add(poolName, poolStats);
      return new PrometheusHistogramMetricsTracker(poolName, this.collectorRegistry);
   }

   /**
    * initialize and register collector if it isn't initialized yet
    */
   private AnterosDBCPCollector getCollector() {
      if (collector == null) {
         collector = new AnterosDBCPCollector().register(this.collectorRegistry);
      }
      return collector;
   }
}
