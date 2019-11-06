package br.com.anteros.dbcp.metrics.prometheus;

import br.com.anteros.dbcp.metrics.PoolStats;
import br.com.anteros.dbcp.mocks.StubPoolStats;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrometheusMetricsTrackerFactoryTest
{

   @After
   public void clearCollectorRegistry()
   {
      CollectorRegistry.defaultRegistry.clear();
   }

   @Test
   public void registersToProvidedCollectorRegistry()
   {
      CollectorRegistry collectorRegistry = new CollectorRegistry();
      PrometheusMetricsTrackerFactory factory = new PrometheusMetricsTrackerFactory(collectorRegistry);
      factory.create("testpool-1", new StubPoolStats(0));
      assertAnterosDBCPMetricsAreNotPresent(CollectorRegistry.defaultRegistry);
      assertAnterosDBCPMetricsArePresent(collectorRegistry);
   }

   @Test
   public void registersToDefaultCollectorRegistry()
   {
      PrometheusMetricsTrackerFactory factory = new PrometheusMetricsTrackerFactory();
      factory.create("testpool-2", new StubPoolStats(0));
      assertAnterosDBCPMetricsArePresent(CollectorRegistry.defaultRegistry);
   }

   private void assertAnterosDBCPMetricsArePresent(CollectorRegistry collectorRegistry)
   {
      List<String> registeredMetrics = toMetricNames(collectorRegistry.metricFamilySamples());
      assertTrue(registeredMetrics.contains("hikaricp_active_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_idle_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_pending_threads"));
      assertTrue(registeredMetrics.contains("hikaricp_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_max_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_min_connections"));
   }

   private void assertAnterosDBCPMetricsAreNotPresent(CollectorRegistry collectorRegistry)
   {
      List<String> registeredMetrics = toMetricNames(collectorRegistry.metricFamilySamples());
      assertFalse(registeredMetrics.contains("hikaricp_active_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_idle_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_pending_threads"));
      assertFalse(registeredMetrics.contains("hikaricp_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_max_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_min_connections"));
   }

   private List<String> toMetricNames(Enumeration<Collector.MetricFamilySamples> enumeration)
   {
      List<String> list = new ArrayList<>();
      while (enumeration.hasMoreElements()) {
         list.add(enumeration.nextElement().name);
      }
      return list;
   }
}
