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
Essa propriedade controla o comportamento de confirmação automática padrão das conexões retornadas do pool.
É um valor booleano.
*Default: true*

&#8986;``connectionTimeout``<br/>
Essa propriedade controla o número máximo de milissegundos que um cliente (que é você) aguardará
para uma conexão da piscina. Se esse tempo for excedido sem que uma conexão se torne
disponível, uma SQLException será lançada. O tempo limite de conexão aceitável mais baixo é de 250 ms.
*Default: 30000 (30 seconds)*

&#8986;``idleTimeout``<br/>
Essa propriedade controla a quantidade máxima de tempo que uma conexão pode ficar ociosa no
piscina. **Esta configuração só se aplica quando `` minimumIdle`` é definido como menor que `` maximumPoolSize``.**
As conexões inativas *não* serão desativadas quando o pool atingir as conexões `` minimumIdle``. Se um
a conexão é desativada ou ociosa está sujeita a uma variação máxima de +30 segundos e a média
variação de +15 segundos. Uma conexão nunca será desativada como inativa *antes* nesse tempo limite. Um valor
de 0 significa que as conexões inativas nunca são removidas do pool. O valor mínimo permitido é 10000ms
(10 segundos).
*Default: 600000 (10 minutes)*

&#8986;``maxLifetime``<br/>
Esta propriedade controla a vida útil máxima de uma conexão no pool. Uma conexão em uso será
nunca será aposentado, somente quando estiver fechado será removido. Em uma conexão por conexão
Com base nisso, uma atenuação negativa menor é aplicada para evitar a extinção em massa no pool. **Recomendamos vivamente
definir esse valor e deve ser vários segundos mais curto que qualquer banco de dados ou infraestrutura imposta
limite de tempo de conexão.** Um valor 0 indica que não há vida útil máxima (vida útil infinita), sujeita a
curso para a configuração `` idleTimeout``.
*Default: 1800000 (30 minutes)*

&#128288;``connectionTestQuery``<br/>
**Se o seu driver suportar JDBC4, é altamente recomendável não configurar esta propriedade.** Isto é para
Drivers "legados" que não suportam a JDBC4 `` Connection.isValid () API``. Esta é a consulta que
será executado imediatamente antes de uma conexão ser fornecida a partir do pool para validar que o
a conexão com o banco de dados ainda está ativa. *Novamente, tente executar o pool sem essa propriedade,
O AnterosDBCP registrará um erro se o seu driver não for compatível com JDBC4 para que você saiba.*
*Default: none*

&#128290;``minimumIdle``<br/>

Esta propriedade controla o número mínimo de *conexões inativas* que o AnterosDBCP tenta manter
no pool. Se as conexões inativas estiverem abaixo desse valor e o total de conexões no pool for menor que `` maximumPoolSize``,
O AnterosDBCP fará o possível para adicionar conexões adicionais de maneira rápida e eficiente.
No entanto, para obter o máximo desempenho e capacidade de resposta às demandas,
recomendamos *não* definir esse valor e permitir que o AnterosDBCP atue como um conjunto de conexões *de tamanho fixo*.
*Default: same as maximumPoolSize*

&#128290;``maximumPoolSize``<br/>
Essa propriedade controla o tamanho máximo que o pool pode atingir, incluindo ambos
conexões inativas e em uso. Basicamente, esse valor determinará o número máximo de
conexões reais com o back-end do banco de dados. Um valor razoável para isso é melhor determinado
pelo seu ambiente de execução. Quando o pool atinge esse tamanho e não há conexões inativas
disponível, as chamadas para getConnection () serão bloqueadas por até milissegundos `` connectionTimeout``
antes de atingir o tempo limite.
*Default: 10*

&#128200;``metricRegistry``<br/>
Esta propriedade está disponível apenas via configuração programática ou contêiner de IoC. Está Propriedade
permite especificar uma instância de um *Codahale / Dropwizard* `` MetricRegistry`` a ser usado pelo
pool para registrar várias métricas.
*Default: none*

&#128200;``healthCheckRegistry``<br/>
Esta propriedade está disponível apenas via configuração programática ou contêiner de IoC. Está Propriedade
permite especificar uma instância de um *Codahale/Dropwizard* `` HealthCheckRegistry`` a ser usado pelo
pool para relatar informações atuais sobre saúde.
*Default: none*

&#128288;``poolName``<br/>
Essa propriedade representa um nome definido pelo usuário para o conjunto de conexões e aparece principalmente
nos consoles de log e gerenciamento JMX para identificar conjuntos e configurações de conjuntos.
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
