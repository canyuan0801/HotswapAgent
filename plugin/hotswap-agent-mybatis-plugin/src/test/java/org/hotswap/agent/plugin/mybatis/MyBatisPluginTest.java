
package org.hotswap.agent.plugin.mybatis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.hotswap.agent.util.test.WaitHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MyBatisPluginTest {

  private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setup() throws Exception {

        File f = Resources.getResourceAsFile("org/hotswap/agent/plugin/mybatis/Mapper1.xml");
        Files.copy(f.toPath(), f.toPath().getParent().resolve("Mapper.xml"));
        try (Reader reader = Resources.getResourceAsReader("org/hotswap/agent/plugin/mybatis/mybatis-config.xml")) {
          sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }


        runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "org/hotswap/agent/plugin/mybatis/CreateDB.sql");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        File tmp = Resources.getResourceAsFile("org/hotswap/agent/plugin/mybatis/Mapper.xml");
        tmp.delete();
    }

    protected static void runScript(DataSource ds, String resource) throws IOException, SQLException {
        try (Connection connection = ds.getConnection()) {
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setAutoCommit(true);
            runner.setStopOnError(false);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runScript(runner, resource);
        }
    }

    private static void runScript(ScriptRunner runner, String resource)
            throws IOException, SQLException {
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            runner.runScript(reader);
        }
    }

    @Test
    public void testUserFromAnnotation() throws Exception {
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
          Mapper mapper = sqlSession.getMapper(Mapper.class);
          User user = mapper.getUserXML("User1");
          assertEquals("User1", user.getName1());
      }
      swapMapper("org/hotswap/agent/plugin/mybatis/Mapper2.xml");
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
          Mapper mapper = sqlSession.getMapper(Mapper.class);
          User user = mapper.getUserXML("User1");
          assertEquals("User2", user.getName1());
      }
    }

    protected static void swapMapper(String mapperNew) throws Exception {
        MyBatisRefreshCommands.reloadFlag = true;
        File f = Resources.getResourceAsFile(mapperNew);
        Files.copy(f.toPath(), f.toPath().getParent().resolve("Mapper.xml"), StandardCopyOption.REPLACE_EXISTING);
        assertTrue(WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return !MyBatisRefreshCommands.reloadFlag;
            }
        }, 4000 ));


        Thread.sleep(100);
    }
}
