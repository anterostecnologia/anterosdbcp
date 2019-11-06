package br.com.anteros.dbcp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Test;

import br.com.anteros.dbcp.mocks.TestObject;

public class PropertyElfTest
{
   @Test
   public void setTargetFromProperties() throws Exception
   {
      Properties properties = new Properties();
      properties.setProperty("string", "aString");
      properties.setProperty("testObject", "br.com.anteros.dbcp.mocks.TestObject");
      TestObject testObject = new TestObject();
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertEquals("aString", testObject.getString());
      assertEquals(br.com.anteros.dbcp.mocks.TestObject.class, testObject.getTestObject().getClass());
      assertNotSame(testObject, testObject.getTestObject());
   }

   @Test
   public void setTargetFromPropertiesNotAClass() throws Exception
   {
      Properties properties = new Properties();
      properties.setProperty("string", "aString");
      properties.setProperty("testObject", "it is not a class");
      TestObject testObject = new TestObject();
      try {
         PropertyElf.setTargetFromProperties(testObject, properties);
         fail("Could never come here");
      }
      catch (RuntimeException e) {
         assertEquals("argument type mismatch", e.getCause().getMessage());
      }
   }
}
