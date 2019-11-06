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

package br.com.anteros.dbcp.pool;

import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPConfig;
import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPDataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;
import br.com.anteros.dbcp.util.UtilityElf;

public class ConnectionStateTest
{
   @Test
   public void testAutoCommit() throws SQLException
   {
      try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource()) {
         ds.setAutoCommit(true);
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setConnectionTestQuery("VALUES 1");
         ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");
         ds.addDataSourceProperty("user", "bar");
         ds.addDataSourceProperty("password", "secret");
         ds.addDataSourceProperty("url", "baf");
         ds.addDataSourceProperty("loginTimeout", "10");

         try (Connection connection = ds.getConnection()) {
            Connection unwrap = connection.unwrap(Connection.class);
            connection.setAutoCommit(false);
            connection.close();

            assertTrue(unwrap.getAutoCommit());
         }
      }
   }

   @Test
   public void testTransactionIsolation() throws SQLException
   {
      try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource()) {
         ds.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setConnectionTestQuery("VALUES 1");
         ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

         try (Connection connection = ds.getConnection()) {
            Connection unwrap = connection.unwrap(Connection.class);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            connection.close();

            assertEquals(Connection.TRANSACTION_READ_COMMITTED, unwrap.getTransactionIsolation());
         }
      }
   }

   @Test
   public void testIsolation() throws Exception
   {
      AnterosDBCPConfig config = newAnterosDBCPConfig();
      config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");
      config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
      config.validate();

      int transactionIsolation = UtilityElf.getTransactionIsolation(config.getTransactionIsolation());
      assertSame(Connection.TRANSACTION_REPEATABLE_READ, transactionIsolation);
   }

   @Test
   public void testReadOnly() throws Exception
   {
      try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource()) {
         ds.setCatalog("test");
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setConnectionTestQuery("VALUES 1");
         ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

         try (Connection connection = ds.getConnection()) {
            Connection unwrap = connection.unwrap(Connection.class);
            connection.setReadOnly(true);
            connection.close();

            assertFalse(unwrap.isReadOnly());
         }
      }
   }

   @Test
   public void testCatalog() throws SQLException
   {
      try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource()) {
         ds.setCatalog("test");
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setConnectionTestQuery("VALUES 1");
         ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

         try (Connection connection = ds.getConnection()) {
            Connection unwrap = connection.unwrap(Connection.class);
            connection.setCatalog("other");
            connection.close();

            assertEquals("test", unwrap.getCatalog());
         }
      }
   }

   @Test
   public void testCommitTracking() throws SQLException
   {
      try (AnterosDBCPDataSource ds = newAnterosDBCPDataSource()) {
         ds.setAutoCommit(false);
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setConnectionTestQuery("VALUES 1");
         ds.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

         try (Connection connection = ds.getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute("SELECT something");
            assertTrue(TestElf.getConnectionCommitDirtyState(connection));

            connection.commit();
            assertFalse(TestElf.getConnectionCommitDirtyState(connection));

            statement.execute("SELECT something", Statement.NO_GENERATED_KEYS);
            assertTrue(TestElf.getConnectionCommitDirtyState(connection));

            connection.rollback();
            assertFalse(TestElf.getConnectionCommitDirtyState(connection));

            ResultSet resultSet = statement.executeQuery("SELECT something");
            assertTrue(TestElf.getConnectionCommitDirtyState(connection));

            connection.rollback(null);
            assertFalse(TestElf.getConnectionCommitDirtyState(connection));

            resultSet.updateRow();
            assertTrue(TestElf.getConnectionCommitDirtyState(connection));
         }
      }
   }
}
