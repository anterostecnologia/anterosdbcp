package br.com.anteros.dbcp.datasource;

import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPConfig;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;

import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;

public class TestSealedConfig
{
   @Test(expected = IllegalStateException.class)
   public void testSealed1() throws SQLException
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");
         fail("Exception should have been thrown");
      }
   }

   @Test(expected = IllegalStateException.class)
   public void testSealed2() throws SQLException
   {
      AnterosDBCPDataSource ds = new AnterosDBCPDataSource();
      ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource closeable = ds) {
         try (Connection connection = ds.getConnection()) {
            ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");
            fail("Exception should have been thrown");
         }
      }
   }

   @Test(expected = IllegalStateException.class)
   public void testSealed3() throws SQLException
   {
      AnterosDBCPDataSource ds = new AnterosDBCPDataSource();
      ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource closeable = ds) {
         try (Connection connection = ds.getConnection()) {
            ds.setAutoCommit(false);
            fail("Exception should have been thrown");
         }
      }
   }

   @Test
   public void testSealedAccessibleMethods() throws SQLException
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

      try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {
         ds.setConnectionTimeout(5000);
         ds.setValidationTimeout(5000);
         ds.setIdleTimeout(30000);
         ds.setLeakDetectionThreshold(60000);
         ds.setMaxLifetime(1800000);
         ds.setMinimumIdle(5);
         ds.setMaximumPoolSize(8);
         ds.setPassword("password");
         ds.setUsername("username");
      }
   }
}
