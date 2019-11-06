Rápido, simples, confiável. O AnterosDBCP é um conjunto de conexões JDBC prontas para produção, com "zero sobrecarga". Com aproximadamente 130 KB, a biblioteca é muito leve.  

&nbsp;&nbsp;&nbsp;<sup>**"Simplicidade é pré-requisito para confiabilidade."**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</sup>

----------------------------------------------------

_Java 8 thru 11 maven artifact:_
```xml
    <dependency>
        <groupId>br.com.anteros</groupId>
        <artifactId>AnterosDBCP</artifactId>
        <version>1.0</version>
    </dependency>
```

Ou [faça o download daqui](http://search.maven.org/#search%7Cga%7C1%7Cbr.com.anteros.dbcp).

------------------------------
#### Configuração

O AnterosDBCP vem com padrões que apresentam bom desempenho na maioria das implantações sem ajustes adicionais. **Cada propriedade é opcional, exceto os itens "essenciais" marcados abaixo.**

<sup>&#128206;</sup>&nbsp;*O AnterosDBCP usa milissegundos para todos os valores de tempo.*

&#128680;&nbsp;O AnterosDBCP conta com temporizadores precisos para desempenho e confiabilidade. É *imperativo* que seu servidor esteja sincronizado com uma fonte de tempo, como um servidor NTP. *Especialmente* se o seu servidor estiver executando em uma máquina virtual. Por quê? [Leia mais aqui] (https://dba.stackexchange.com/a/171020). **Não confie nas configurações do hypervisor para "sincronizar" o relógio da máquina virtual. Configure a sincronização da fonte de tempo dentro da máquina virtual.** Se você solicitar suporte em um problema causado por falta de sincronização de tempo, será ridicularizado publicamente no Twitter.

##### Essenciais

&#128288;``dataSourceClassName``<br/>
Este é o nome da classe `` DataSource`` fornecida pelo driver JDBC. Consulte o
documentação para o seu driver JDBC específico para obter esse nome de classe ou consulte a [tabela] (https://github.com/brettwooldridge/AnterosDBCP#popular-datasource-class-names) abaixo.
Nota As fontes de dados XA não são suportadas. XA requer um gerenciador de transações real como
[bitronix] (https://github.com/bitronix/btm). Observe que você não precisa dessa propriedade se estiver usando
`` jdbcUrl`` para a configuração do driver JDBC baseado no DriverManager "old-school".
*Default: none*

*- or -*

&#128288;``jdbcUrl``<br/>
Esta propriedade instrui o AnterosDBCP a usar a configuração "DriverManager". Achamos que o DataSource
A configuração (acima) é superior por vários motivos (veja abaixo), mas em muitas implantações há
pouca diferença significativa. **Ao usar esta propriedade com drivers "antigos", também será necessário definir
a propriedade `` driverClassName``, mas tente primeiro sem.** Observe que, se essa propriedade for usada, você poderá
ainda use as propriedades * DataSource * para configurar seu driver e é de fato recomendado sobre os parâmetros do driver
especificado no próprio URL.
*Default: none*

***

&#128288;``username``<br/>
Essa propriedade define o nome de usuário de autenticação padrão usado ao obter *Connections* de
o driver subjacente. Observe que para DataSources isso funciona de uma maneira muito determinística,
chamando `` DataSource.getConnection (*nome de usuário*, senha) `` no DataSource subjacente. Contudo,
para configurações baseadas em driver, cada driver é diferente. No caso de Driver-based, AnterosDBCP
usará essa propriedade `` username`` para definir uma propriedade `` user`` no `` Properties`` passado para o
chamada `` DriverManager.getConnection (jdbcUrl, props) do driver ``. Se não é isso que você precisa,
pule esse método completamente e chame `` addDataSourceProperty ("nome de usuário", ...) ``, por exemplo.
*Default: none*

&#128288;``password``<br/>
Essa propriedade define a senha de autenticação padrão usada ao obter *Connections* de
o driver subjacente. Observe que para DataSources isso funciona de uma maneira muito determinística,
chamando `` DataSource.getConnection (nome de usuário, *senha*) `` no DataSource subjacente. Contudo,
para configurações baseadas em driver, cada driver é diferente. No caso de Driver-based, o AnterosDBCP
usará essa propriedade `` password`` para definir uma propriedade `` password`` no `` Properties`` passado para o
chamada `` DriverManager.getConnection (jdbcUrl, props) do driver ``. Se não é isso que você precisa,
pule esse método completamente e chame `` addDataSourceProperty ("pass", ...) ``, por exemplo.
*Default: none*

##### Frequently used

&#9989;``autoCommit``<br/>
This property controls the default auto-commit behavior of connections returned from the pool.
It is a boolean value.
*Default: true*

&#8986;``connectionTimeout``<br/>
This property controls the maximum number of milliseconds that a client (that's you) will wait
for a connection from the pool.  If this time is exceeded without a connection becoming
available, a SQLException will be thrown.  Lowest acceptable connection timeout is 250 ms.
*Default: 30000 (30 seconds)*

&#8986;``idleTimeout``<br/>
This property controls the maximum amount of time that a connection is allowed to sit idle in the
pool.  **This setting only applies when ``minimumIdle`` is defined to be less than ``maximumPoolSize``.**
Idle connections will *not* be retired once the pool reaches ``minimumIdle`` connections.  Whether a
connection is retired as idle or not is subject to a maximum variation of +30 seconds, and average 
variation of +15 seconds.  A connection will never be retired as idle *before* this timeout.  A value
of 0 means that idle connections are never removed from the pool.  The minimum allowed value is 10000ms
(10 seconds).
*Default: 600000 (10 minutes)*

&#8986;``maxLifetime``<br/>
This property controls the maximum lifetime of a connection in the pool.  An in-use connection will
never be retired, only when it is closed will it then be removed.  On a connection-by-connection
basis, minor negative attenuation is applied to avoid mass-extinction in the pool.  **We strongly recommend
setting this value, and it should be several seconds shorter than any database or infrastructure imposed
connection time limit.**  A value of 0 indicates no maximum lifetime (infinite lifetime), subject of
course to the ``idleTimeout`` setting.
*Default: 1800000 (30 minutes)*

&#128288;``connectionTestQuery``<br/>
**If your driver supports JDBC4 we strongly recommend not setting this property.** This is for 
"legacy" drivers that do not support the JDBC4 ``Connection.isValid() API``.  This is the query that
will be executed just before a connection is given to you from the pool to validate that the 
connection to the database is still alive. *Again, try running the pool without this property,
AnterosDBCP will log an error if your driver is not JDBC4 compliant to let you know.*
*Default: none*

&#128290;``minimumIdle``<br/>
This property controls the minimum number of *idle connections* that AnterosDBCP tries to maintain
in the pool.  If the idle connections dip below this value and total connections in the pool are less than ``maximumPoolSize``,
AnterosDBCP will make a best effort to add additional connections quickly and efficiently.
However, for maximum performance and responsiveness to spike demands,
we recommend *not* setting this value and instead allowing AnterosDBCP to act as a *fixed size* connection pool.
*Default: same as maximumPoolSize*

&#128290;``maximumPoolSize``<br/>
This property controls the maximum size that the pool is allowed to reach, including both
idle and in-use connections.  Basically this value will determine the maximum number of
actual connections to the database backend.  A reasonable value for this is best determined
by your execution environment.  When the pool reaches this size, and no idle connections are
available, calls to getConnection() will block for up to ``connectionTimeout`` milliseconds
before timing out. 
*Default: 10*

&#128200;``metricRegistry``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to specify an instance of a *Codahale/Dropwizard* ``MetricRegistry`` to be used by the
pool to record various metrics.  
*Default: none*

&#128200;``healthCheckRegistry``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to specify an instance of a *Codahale/Dropwizard* ``HealthCheckRegistry`` to be used by the
pool to report current health information.  
wiki page for details.
*Default: none*

&#128288;``poolName``<br/>
This property represents a user-defined name for the connection pool and appears mainly
in logging and JMX management consoles to identify pools and pool configurations.
*Default: auto-generated*

##### Infrequently used

&#8986;``initializationFailTimeout``<br/>
This property controls whether the pool will "fail fast" if the pool cannot be seeded with
an initial connection successfully.  Any positive number is taken to be the number of 
milliseconds to attempt to acquire an initial connection; the application thread will be 
blocked during this period.  If a connection cannot be acquired before this timeout occurs,
an exception will be thrown.  This timeout is applied *after* the ``connectionTimeout``
period.  If the value is zero (0), AnterosDBCP will attempt to obtain and validate a connection.
If a connection is obtained, but fails validation, an exception will be thrown and the pool
not started.  However, if a connection cannot be obtained, the pool will start, but later 
efforts to obtain a connection may fail.  A value less than zero will bypass any initial
connection attempt, and the pool will start immediately while trying to obtain connections
in the background.  Consequently, later efforts to obtain a connection may fail.
*Default: 1*

&#10062;``isolateInternalQueries``<br/>
This property determines whether AnterosDBCP isolates internal pool queries, such as the
connection alive test, in their own transaction.  Since these are typically read-only
queries, it is rarely necessary to encapsulate them in their own transaction.  This
property only applies if ``autoCommit`` is disabled.
*Default: false*

&#10062;``allowPoolSuspension``<br/>
This property controls whether the pool can be suspended and resumed through JMX.  This is
useful for certain failover automation scenarios.  When the pool is suspended, calls to
``getConnection()`` will *not* timeout and will be held until the pool is resumed.
*Default: false*

&#10062;``readOnly``<br/>
This property controls whether *Connections* obtained from the pool are in read-only mode by
default.  Note some databases do not support the concept of read-only mode, while others provide
query optimizations when the *Connection* is set to read-only.  Whether you need this property
or not will depend largely on your application and database. 
*Default: false*

&#10062;``registerMbeans``<br/>
This property controls whether or not JMX Management Beans ("MBeans") are registered or not.
*Default: false*

&#128288;``catalog``<br/>
This property sets the default *catalog* for databases that support the concept of catalogs.
If this property is not specified, the default catalog defined by the JDBC driver is used.
*Default: driver default*

&#128288;``connectionInitSql``<br/>
This property sets a SQL statement that will be executed after every new connection creation
before adding it to the pool. If this SQL is not valid or throws an exception, it will be
treated as a connection failure and the standard retry logic will be followed.
*Default: none*

&#128288;``driverClassName``<br/>
AnterosDBCP will attempt to resolve a driver through the DriverManager based solely on the ``jdbcUrl``,
but for some older drivers the ``driverClassName`` must also be specified.  Omit this property unless
you get an obvious error message indicating that the driver was not found.
*Default: none*

&#128288;``transactionIsolation``<br/>
This property controls the default transaction isolation level of connections returned from
the pool.  If this property is not specified, the default transaction isolation level defined
by the JDBC driver is used.  Only use this property if you have specific isolation requirements that are
common for all queries.  The value of this property is the constant name from the ``Connection``
class such as ``TRANSACTION_READ_COMMITTED``, ``TRANSACTION_REPEATABLE_READ``, etc.
*Default: driver default*

&#8986;``validationTimeout``<br/>
This property controls the maximum amount of time that a connection will be tested for aliveness.
This value must be less than the ``connectionTimeout``.  Lowest acceptable validation timeout is 250 ms.
*Default: 5000*

&#8986;``leakDetectionThreshold``<br/>
This property controls the amount of time that a connection can be out of the pool before a
message is logged indicating a possible connection leak.  A value of 0 means leak detection
is disabled.  Lowest acceptable value for enabling leak detection is 2000 (2 seconds).
*Default: 0*

&#10145;``dataSource``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to directly set the instance of the ``DataSource`` to be wrapped by the pool, rather than
having AnterosDBCP construct it via reflection.  This can be useful in some dependency injection
frameworks. When this property is specified, the ``dataSourceClassName`` property and all
DataSource-specific properties will be ignored.
*Default: none*

&#128288;``schema``<br/>
This property sets the default *schema* for databases that support the concept of schemas.
If this property is not specified, the default schema defined by the JDBC driver is used.
*Default: driver default*

&#10145;``threadFactory``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to set the instance of the ``java.util.concurrent.ThreadFactory`` that will be used
for creating all threads used by the pool. It is needed in some restricted execution environments
where threads can only be created through a ``ThreadFactory`` provided by the application container.
*Default: none*

&#10145;``scheduledExecutor``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to set the instance of the ``java.util.concurrent.ScheduledExecutorService`` that will
be used for various internally scheduled tasks.  If supplying AnterosDBCP with a ``ScheduledThreadPoolExecutor``
instance, it is recommended that ``setRemoveOnCancelPolicy(true)`` is used.
*Default: none*

----------------------------------------------------

#### Missing Knobs

AnterosDBCP has plenty of "knobs" to turn as you can see above, but comparatively less than some other pools.
This is a design philosophy.  The AnterosDBCP design aesthetic is Minimalism.  In keeping with the
*simple is better* or *less is more* design philosophy, some configuration axis are intentionally left out.

#### Statement Cache

Many connection pools, including Apache DBCP, Vibur, c3p0 and others offer ``PreparedStatement`` caching.
AnterosDBCP does not.  Why?

At the connection pool layer ``PreparedStatements`` can only be cached *per connection*.  If your application
has 250 commonly executed queries and a pool of 20 connections you are asking your database to hold on to
5000 query execution plans -- and similarly the pool must cache this many ``PreparedStatements`` and their
related graph of objects.

Most major database JDBC drivers already have a Statement cache that can be configured, including PostgreSQL,
Oracle, Derby, MySQL, DB2, and many others.  JDBC drivers are in a unique position to exploit database specific
features, and nearly all of the caching implementations are capable of sharing execution plans *across connections*.
This means that instead of 5000 statements in memory and associated execution plans, your 250 commonly executed
queries result in exactly 250 execution plans in the database.  Clever implementations do not even retain
``PreparedStatement`` objects in memory at the driver-level but instead merely attach new instances to existing plan IDs.

Using a statement cache at the pooling layer is an [anti-pattern](https://en.wikipedia.org/wiki/Anti-pattern),
and will negatively impact your application performance compared to driver-provided caches.

#### Log Statement Text / Slow Query Logging

Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL, and others.  Some
even support slow query logging.  For those few databases that do not support it, several options are available.




----------------------------------------------------

### Initialization

You can use the ``AnterosDBCPConfig`` class like so<sup>1</sup>:
```java
AnterosDBCPConfig config = new AnterosDBCPConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
config.setUsername("bart");
config.setPassword("51mp50n");
config.addDataSourceProperty("cachePrepStmts", "true");
config.addDataSourceProperty("prepStmtCacheSize", "250");
config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
```
&nbsp;<sup><sup>1</sup> MySQL-specific example, DO NOT COPY VERBATIM.</sup>

or directly instantiate a ``AnterosDBCPDataSource`` like so:
```java
AnterosDBCPDataSource ds = new AnterosDBCPDataSource();
ds.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
ds.setUsername("bart");
ds.setPassword("51mp50n");
...
```
or property file based:
```java
// Examines both filesystem and classpath for .properties file
AnterosDBCPConfig config = new AnterosDBCPConfig("/some/path/hikari.properties");
AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
```
Example property file:
```ini
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.user=test
dataSource.password=test
dataSource.databaseName=mydb
dataSource.portNumber=5432
dataSource.serverName=localhost
```
or ``java.util.Properties`` based:
```java
Properties props = new Properties();
props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
props.setProperty("dataSource.user", "test");
props.setProperty("dataSource.password", "test");
props.setProperty("dataSource.databaseName", "mydb");
props.put("dataSource.logWriter", new PrintWriter(System.out));

AnterosDBCPConfig config = new AnterosDBCPConfig(props);
AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
```

There is also a System property available, ``hikaricp.configurationFile``, that can be used to specify the
location of a properties file.  If you intend to use this option, construct a ``AnterosDBCPConfig`` or ``AnterosDBCPDataSource``
instance using the default constructor and the properties file will be loaded.


### Popular DataSource Class Names

We recommended using ``dataSourceClassName`` instead of ``jdbcUrl``, but either is acceptable.  We'll say that again, *either is acceptable*.

&#9888;&nbsp;*Note: Spring Boot auto-configuration users, you need to use ``jdbcUrl``-based configuration.*

&#9888;&nbsp;The MySQL DataSource is known to be broken with respect to network timeout support. Use ``jdbcUrl`` configuration instead.

Here is a list of JDBC *DataSource* classes for popular databases:

| Database         | Driver       | *DataSource* class |
|:---------------- |:------------ |:-------------------|
| Apache Derby     | Derby        | org.apache.derby.jdbc.ClientDataSource |
| Firebird         | Jaybird      | org.firebirdsql.ds.FBSimpleDataSource |
| H2               | H2           | org.h2.jdbcx.JdbcDataSource |
| HSQLDB           | HSQLDB       | org.hsqldb.jdbc.JDBCDataSource |
| IBM DB2          | IBM JCC      | com.ibm.db2.jcc.DB2SimpleDataSource |
| IBM Informix     | IBM Informix | com.informix.jdbcx.IfxDataSource |
| MS SQL Server    | Microsoft    | com.microsoft.sqlserver.jdbc.SQLServerDataSource |
| ~~MySQL~~        | Connector/J  | ~~com.mysql.jdbc.jdbc2.optional.MysqlDataSource~~ |
| MariaDB          | MariaDB      | org.mariadb.jdbc.MariaDbDataSource |
| Oracle           | Oracle       | oracle.jdbc.pool.OracleDataSource |
| OrientDB         | OrientDB     | com.orientechnologies.orient.jdbc.OrientDataSource |
| PostgreSQL       | pgjdbc-ng    | com.impossibl.postgres.jdbc.PGDataSource |
| PostgreSQL       | PostgreSQL   | org.postgresql.ds.PGSimpleDataSource |
| SAP MaxDB        | SAP          | com.sap.dbtech.jdbc.DriverSapDB |
| SQLite           | xerial       | org.sqlite.SQLiteDataSource |
| SyBase           | jConnect     | com.sybase.jdbc4.jdbc.SybDataSource |



----------------------------------------------------
### Requirements

 &#8658; Java 8+ (Java 6/7 artifacts are in maintenance mode)<br/>
 &#8658; slf4j library<br/>


[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
