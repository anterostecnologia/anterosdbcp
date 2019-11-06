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

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import static br.com.anteros.dbcp.metrics.prometheus.PrometheusMetricsTrackerFactory.RegistrationStatus.REGISTERED;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.anteros.dbcp.metrics.IMetricsTracker;
import br.com.anteros.dbcp.metrics.MetricsTrackerFactory;
import br.com.anteros.dbcp.metrics.PoolStats;

/**
 * <pre>{@code
 * AnterosDBCPConfig config = new AnterosDBCPConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
 * }</pre>
 * or
 * <pre>{@code
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(new CollectorRegistry()));
 * }</pre>
 *
 * Note: the internal {@see io.prometheus.client.Summary} requires heavy locks. Consider using
 * {@see PrometheusHistogramMetricsTrackerFactory} if performance plays a role and you don't need the summary per se.
 */
public class PrometheusMetricsTrackerFactory implements MetricsTrackerFactory
{

   private final static Map<CollectorRegistry, RegistrationStatus> registrationStatuses = new ConcurrentHashMap<>();

   private final AnterosDBCPCollector collector = new AnterosDBCPCollector();

   private final CollectorRegistry collectorRegistry;

   public enum RegistrationStatus
   {
      REGISTERED;
   }

   /**
    * Default Constructor. The AnterosDBCP metrics are registered to the default
    * collector registry ({@code CollectorRegistry.defaultRegistry}).
    */
   public PrometheusMetricsTrackerFactory()
   {
      this(CollectorRegistry.defaultRegistry);
   }

   /**
    * Constructor that allows to pass in a {@link CollectorRegistry} to which the
    * AnterosDBCP metrics are registered.
    */
   public PrometheusMetricsTrackerFactory(CollectorRegistry collectorRegistry)
   {
      this.collectorRegistry = collectorRegistry;
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats)
   {
      registerCollector(this.collector, this.collectorRegistry);
      this.collector.add(poolName, poolStats);
      return new PrometheusMetricsTracker(poolName, this.collectorRegistry, this.collector);
   }

   private void registerCollector(Collector collector, CollectorRegistry collectorRegistry)
   {
      if (registrationStatuses.putIfAbsent(collectorRegistry, REGISTERED) == null) {
         collector.register(collectorRegistry);
      }
   }
}
