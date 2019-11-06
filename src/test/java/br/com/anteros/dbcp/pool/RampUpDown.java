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

import static br.com.anteros.dbcp.pool.TestElf.getPool;
import static br.com.anteros.dbcp.pool.TestElf.newAnterosDBCPConfig;
import static br.com.anteros.dbcp.util.UtilityElf.quietlySleep;
import static org.junit.Assert.assertSame;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import br.com.anteros.dbcp.AnterosDBCPConfig;
import br.com.anteros.dbcp.AnterosDBCPDataSource;

public class RampUpDown
{
    @Test
    public void rampUpDownTest() throws SQLException
    {
        AnterosDBCPConfig config = newAnterosDBCPConfig();
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(60);
        config.setInitializationFailTimeout(0);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("br.com.anteros.dbcp.mocks.StubDataSource");

        System.setProperty("br.com.anteros.dbcp.housekeeping.periodMs", "250");

        try (AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config)) {

           ds.setIdleTimeout(1000);
           AnterosDBCPPool pool = getPool(ds);

           // wait two housekeeping periods so we don't fail if this part of test runs too quickly
           quietlySleep(500);

           Assert.assertSame("Total connections not as expected", 5, pool.getTotalConnections());

           Connection[] connections = new Connection[ds.getMaximumPoolSize()];
           for (int i = 0; i < connections.length; i++)
           {
               connections[i] = ds.getConnection();
           }

           assertSame("Total connections not as expected", 60, pool.getTotalConnections());

           for (Connection connection : connections)
           {
               connection.close();
           }

           quietlySleep(500);

           assertSame("Total connections not as expected", 5, pool.getTotalConnections());
        }
    }
}
