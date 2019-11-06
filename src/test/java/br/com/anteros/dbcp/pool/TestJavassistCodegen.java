package br.com.anteros.dbcp.pool;

import br.com.anteros.dbcp.mocks.StubConnection;
import br.com.anteros.dbcp.pool.TestElf.FauxWebClassLoader;
import br.com.anteros.dbcp.util.JavassistProxyFactory;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Stream;

public class TestJavassistCodegen {
   @Test
   public void testCodegen() throws Exception {
      String tmp = System.getProperty("java.io.tmpdir");
      JavassistProxyFactory.main(tmp + (tmp.endsWith("/") ? "" : "/"));

      Path base = Paths.get(tmp, "target/classes/br/com/anteros/dbcp/pool".split("/"));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("AnterosDBCPProxyConnection.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("AnterosDBCPProxyStatement.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("AnterosDBCPProxyCallableStatement.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("AnterosDBCPProxyPreparedStatement.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("AnterosDBCPProxyResultSet.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("ProxyFactory.class")));

      FauxWebClassLoader fauxClassLoader = new FauxWebClassLoader();
      Class<?> proxyFactoryClass = fauxClassLoader.loadClass("br.com.anteros.dbcp.pool.ProxyFactory");

      Connection connection = new StubConnection();

      Class<?> fastListClass = fauxClassLoader.loadClass("br.com.anteros.dbcp.util.FastList");
      Object fastList = fastListClass.getConstructor(Class.class).newInstance(Statement.class);

      Object proxyConnection = getMethod(proxyFactoryClass, "getProxyConnection")
         .invoke(null,
            null /*poolEntry*/,
            connection,
            fastList,
            null /*leakTask*/,
            0L /*now*/,
            Boolean.FALSE /*isReadOnly*/,
            Boolean.FALSE /*isAutoCommit*/);
      Assert.assertNotNull(proxyConnection);

      Object proxyStatement = getMethod(proxyConnection.getClass(), "createStatement", 0)
         .invoke(proxyConnection);
      Assert.assertNotNull(proxyStatement);
   }

   private Method getMethod(Class<?> clazz, String methodName, Integer... parameterCount)
   {
      return Stream.of(clazz.getDeclaredMethods())
          .filter(method -> method.getName().equals(methodName))
          .filter(method -> (parameterCount.length == 0 || parameterCount[0] == method.getParameterCount()))
          .peek(method -> method.setAccessible(true))
          .findFirst()
          .orElseThrow(RuntimeException::new);
   }
}
