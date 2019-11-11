Rápido, simples, confiável. O AnterosDBCP é um conjunto de conexões JDBC prontas para produção, com "zero sobrecarga". Com aproximadamente 130 KB, a biblioteca é muito leve.  

&nbsp;&nbsp;&nbsp;<sup>**"Simplicidade é pré-requisito para confiabilidade."**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</sup>

----------------------------------------------------

_Java 8 thru 11 maven artifact:_
```xml
    <dependency>
        <groupId>br.com.anteros</groupId>
        <artifactId>Anteros-DBCP</artifactId>
        <version>1.0.0</version>
    </dependency>
```

Ou [faça o download daqui](http://search.maven.org/#search%7Cga%7C1%7Ca:Anteros-DBCP).

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
para uma conexão do pool. Se esse tempo for excedido sem que uma conexão se torne
disponível, uma SQLException será lançada. O tempo limite de conexão aceitável mais baixo é de 250 ms.
*Default: 30000 (30 seconds)*

&#8986;``idleTimeout``<br/>
Essa propriedade controla a quantidade máxima de tempo que uma conexão pode ficar ociosa no
pool. **Esta configuração só se aplica quando `` minimumIdle`` é definido como menor que `` maximumPoolSize``.**
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

Essa propriedade controla se o pool "falhará rapidamente" se o pool não puder ser semeado com
uma conexão inicial com sucesso. Qualquer número positivo é considerado o número de
milissegundos para tentar adquirir uma conexão inicial; o encadeamento do aplicativo será
bloqueado durante esse período. Se uma conexão não puder ser adquirida antes que esse tempo limite ocorra,
uma exceção será lançada. Esse tempo limite é aplicado *após* o `` connectionTimeout``
período. Se o valor for zero (0), o AnterosDBCP tentará obter e validar uma conexão.
Se uma conexão for obtida, mas falhar na validação, uma exceção será lançada e o pool
não foi iniciado. No entanto, se uma conexão não puder ser obtida, o pool será iniciado, mas mais tarde
os esforços para obter uma conexão podem falhar. Um valor menor que zero ignorará qualquer valor inicial
tentativa de conexão e o pool iniciará imediatamente ao tentar obter conexões
no fundo. Conseqüentemente, os esforços posteriores para obter uma conexão podem falhar.
*Default: 1*

&#10062;``isolateInternalQueries``<br/>
This property determines whether AnterosDBCP isolates internal pool queries, such as the
connection alive test, in their own transaction.  Since these are typically read-only
queries, it is rarely necessary to encapsulate them in their own transaction.  This
property only applies if ``autoCommit`` is disabled.
*Default: false*

&#10062;``allowPoolSuspension``<br/>
Esta propriedade controla se o conjunto pode ser suspenso e retomado através do JMX. Isto é
útil para determinados cenários de automação de failover. Quando o pool está suspenso, as chamadas para
`` getConnection () `` *não* expirará o tempo limite e será mantido até que o pool seja reiniciado.
*Default: false*

&#10062;``readOnly``<br/>
Essa propriedade controla se *Conexões* obtidas do pool estão no modo somente leitura por
padrão. Observe que alguns bancos de dados não suportam o conceito de modo somente leitura, enquanto outros fornecem
otimizações de consulta quando o *Connection* está definido como somente leitura. Se você precisa desta propriedade
ou não, dependerá amplamente do seu aplicativo e banco de dados.
*Default: false*

&#10062;``registerMbeans``<br/>
Esta propriedade controla se o JMX Management Beans ("MBeans") está ou não registrado.
*Default: false*

&#128288;``catalog``<br/>
Essa propriedade define o *catálogo* padrão para bancos de dados que suportam o conceito de catálogos.
Se essa propriedade não for especificada, o catálogo padrão definido pelo driver JDBC será usado.
*Default: driver default*

&#128288;``connectionInitSql``<br/>
Essa propriedade define uma instrução SQL que será executada após cada nova criação de conexão
antes de adicioná-lo ao pool. Se esse SQL não for válido ou gerar uma exceção, será
tratado como uma falha de conexão e a lógica de nova tentativa padrão será seguida.
*Default: none*

&#128288;``driverClassName``<br/>
O AnterosDBCP tentará resolver um driver através do DriverManager com base apenas no `` jdbcUrl``,
mas para alguns drivers mais antigos, o `` driverClassName`` também deve ser especificado. Omita esta propriedade, a menos que
você recebe uma mensagem de erro óbvia indicando que o driver não foi encontrado.
*Default: none*

&#128288;``transactionIsolation``<br/>
Esta propriedade controla o nível de isolamento de transação padrão das conexões retornadas do pool. Se essa propriedade não for especificada, o nível de isolamento da transação padrão definido
pelo driver JDBC é usado. Use essa propriedade apenas se você tiver requisitos de isolamento específicos que sejam
comum para todas as consultas. O valor dessa propriedade é o nome constante do `` Connection``
classe como `` TRANSACTION_READ_COMMITTED``, `` TRANSACTION_REPEATABLE_READ``, etc.
*Default: driver default*

&#8986;``validationTimeout``<br/>
Essa propriedade controla a quantidade máxima de tempo que uma conexão será testada quanto à vitalidade.
Este valor deve ser menor que o `` connectionTimeout``. O tempo limite de validação aceitável mais baixo é de 250 ms.
*Default: 5000*

&#8986;``leakDetectionThreshold``<br/>
Essa propriedade controla a quantidade de tempo que uma conexão pode ficar fora do pool antes de um
mensagem é registrada indicando um possível vazamento de conexão. Um valor 0 significa detecção de vazamento
está desabilitado. O menor valor aceitável para permitir a detecção de vazamentos é 2000 (2 segundos).
*Default: 0*

&#10145;``dataSource``<br/>
Esta propriedade está disponível apenas via configuração programática ou contêiner de IoC. Está Propriedade
permite que você defina diretamente a instância do `` DataSource`` a ser agrupada pelo pool, em vez de
fazendo com que o AnterosDBCP o construa via reflexão. Isso pode ser útil em algumas injeção de dependência
estruturas. Quando essa propriedade é especificada, a propriedade `` dataSourceClassName`` e todos
As propriedades específicas do DataSource serão ignoradas.
*Default: none*

&#128288;``schema``<br/>
Esta propriedade define o *esquema* padrão para bancos de dados que suportam o conceito de esquemas.
Se essa propriedade não for especificada, o esquema padrão definido pelo driver JDBC será usado.
*Default: driver default*

&#10145;``threadFactory``<br/>
Esta propriedade está disponível apenas via configuração programática ou contêiner de IoC. Está Propriedade
permite que você defina a instância do `` java.util.concurrent.ThreadFactory`` que será usado
para criar todos os threads usados ​​pelo pool. É necessário em alguns ambientes de execução restritos
onde os threads podem ser criados apenas através de um `` ThreadFactory`` fornecido pelo contêiner do aplicativo.
*Default: none*

&#10145;``scheduledExecutor``<br/>
Propriedade permite definir a instância do `` java.util.concurrent.ScheduledExecutorService`` que será
ser usado para várias tarefas agendadas internamente. Se fornecer ao AnterosDBCP um `` ScheduledThreadPoolExecutor``
Por exemplo, é recomendável que `` setRemoveOnCancelPolicy (true) `` seja usado.
*Default: none*

----------------------------------------------------

#### Cache de Instrução


Muitos pools de conexão, incluindo Apache DBCP, Vibur, c3p0 e outros, oferecem cache `` PreparedStatement``.
O AnterosDBCP não. Por quê?

Na camada do conjunto de conexões, `` PreparedStatements`` só pode ser armazenado em cache *por conexão*. Se sua aplicação
possui 250 consultas comumente executadas e um conjunto de 20 conexões nas quais você está solicitando que o banco de dados mantenha
5000 planos de execução de consultas - e da mesma forma o pool deve armazenar em cache tantos `` PreparedStatements`` e seus
gráfico relacionado de objetos.

Os principais drivers JDBC do banco de dados já possuem um cache de instruções que pode ser configurado, incluindo PostgreSQL,
Oracle, Derby, MySQL, DB2 e muitos outros. Os drivers JDBC estão em uma posição única para explorar dados específicos do banco de dados.
recursos e quase todas as implementações de armazenamento em cache são capazes de compartilhar planos de execução *entre conexões*.
Isso significa que, em vez de 5000 instruções na memória e planos de execução associados, suas 250 consultas executados normalmente resultam em exatamente 250 planos de execução no banco de dados. Implementações inteligentes nem retêm
Objetos `` PreparedStatement`` na memória no nível do driver, mas apenas anexam novas instâncias aos IDs de plano existentes.

O uso de um cache de instruções na camada de pool é um [antipadrão] (https://en.wikipedia.org/wiki/Anti-pattern),
e afetará negativamente o desempenho do aplicativo em comparação com os caches fornecidos pelo driver.

#### LTexto da Instrução / Log de Consulta Lento

Como o cache de instruções, a maioria dos principais fornecedores de bancos de dados suporta o log de instruções através de
propriedades de seu próprio driver. Isso inclui Oracle, MySQL, Derby, MSSQL e outros. Alguns
até mesmo suporte ao log de consultas lento. Para os poucos bancos de dados que não o suportam, várias opções estão disponíveis.




----------------------------------------------------

### Inicialização


Você pode usar o ``AnterosDBCPConfig`` class assim:<sup>1</sup>:
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

ou instanciar diretamente ``AnterosDBCPDataSource`` :
```java
AnterosDBCPDataSource ds = new AnterosDBCPDataSource();
ds.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
ds.setUsername("bart");
ds.setPassword("51mp50n");
...
```
ou com arquivo de propriedades:
```java
// Examines both filesystem and classpath for .properties file
AnterosDBCPConfig config = new AnterosDBCPConfig("/some/path/hikari.properties");
AnterosDBCPDataSource ds = new AnterosDBCPDataSource(config);
```
Arquivo de Propriedade de Exemplo:
```ini
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.user=test
dataSource.password=test
dataSource.databaseName=mydb
dataSource.portNumber=5432
dataSource.serverName=localhost
```
or ``java.util.Properties``:
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

Há também uma propriedade System disponível, `` hikaricp.configurationFile``, que pode ser usada para especificar o
localização de um arquivo de propriedades. Se você pretende usar esta opção, construa um `` AnterosDBCPConfig`` ou `` AnterosDBCPDataSource``
instância usando o construtor padrão e o arquivo de propriedades será carregado.


### Nomes populares da classe DataSource

Recomendamos o uso de `` dataSourceClassName`` em vez de `` jdbcUrl``, mas ambos são aceitáveis. Diremos novamente: *é aceitável*.

& # 9888; & nbsp; *Nota: Para usuários de configuração automática do Spring Boot, é necessário usar a configuração baseada em `` jdbcUrl``.*

& # 9888; & nbsp; O MySQL DataSource é conhecido por estar quebrado com relação ao suporte de tempo limite da rede. Use a configuração `` jdbcUrl``.

Aqui está uma lista de classes JDBC *DataSource* para bancos de dados populares:

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
### Requerimentos

 &#8658; Java 8+<br/>
 &#8658; slf4j library<br/>


[license]:LICENÇA
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
