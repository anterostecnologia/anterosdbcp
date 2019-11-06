package br.com.anteros.dbcp.metrics.micrometer;

import br.com.anteros.dbcp.metrics.IMetricsTracker;
import br.com.anteros.dbcp.metrics.MetricsTrackerFactory;
import br.com.anteros.dbcp.metrics.PoolStats;
import io.micrometer.core.instrument.MeterRegistry;

public class MicrometerMetricsTrackerFactory implements MetricsTrackerFactory
{

   private final MeterRegistry registry;

   public MicrometerMetricsTrackerFactory(MeterRegistry registry)
   {
      this.registry = registry;
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats)
   {
      return new MicrometerMetricsTracker(poolName, poolStats, registry);
   }
}
