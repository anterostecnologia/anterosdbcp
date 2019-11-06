package br.com.anteros.dbcp.pool;

import static br.com.anteros.dbcp.pool.TestElf.getPool;
import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPConfig;
import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPDataSource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;

/**
 * Tests for {@link AnterosDBCPDataSource#isRunning()}.
 */
public class TestIsRunning
{
    @Test
    public void testRunningNormally()
    {
        try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(basicConfig()))
        {
            assertTrue(ds.isRunning());
        }
    }


    @Test
    public void testNoPool()
    {
        try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource())
        {
            assertNull("Pool should not be initialized.", getPool(ds));
            assertFalse(ds.isRunning());
        }
    }


    @Test
    public void testSuspendAndResume()
    {
        try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(basicConfig()))
        {
            ds.getAnterosDBCPPoolMXBean().suspendPool();
            assertFalse(ds.isRunning());

            ds.getAnterosDBCPPoolMXBean().resumePool();
            assertTrue(ds.isRunning());
        }
    }


    @Test
    public void testShutdown()
    {
        try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(basicConfig()))
        {
            ds.close();
            assertFalse(ds.isRunning());
        }
    }


    private AnterosDBCPConfig basicConfig()
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
        config.setAllowPoolSuspension(true);

        return config;
    }
}
