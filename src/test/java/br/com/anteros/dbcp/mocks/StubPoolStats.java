package br.com.anteros.dbcp.mocks;

import br.com.anteros.dbcp.metrics.PoolStats;

public class StubPoolStats extends PoolStats
{

   public StubPoolStats(long timeoutMs)
   {
      super(timeoutMs);
   }

   @Override
   protected void update()
   {
      // Do nothing
   }


}
