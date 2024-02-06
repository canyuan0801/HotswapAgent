
package org.hotswap.agent.plugin.spring;

import org.hotswap.agent.plugin.spring.reload.SpringChangedAgent;
import org.hotswap.agent.plugin.spring.testBeans.*;
import org.hotswap.agent.util.spring.io.resource.ClassPathResource;
import org.hotswap.agent.util.spring.io.resource.Resource;
import org.hotswap.agent.util.test.WaitHelper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class SpringPlugin2Test {

    @Rule
    public ClassSwappingRule swappingRule = new ClassSwappingRule();
    private static AbstractApplicationContext xmlApplicationContext;
    private static Resource xmlContext = new ClassPathResource("xmlContext.xml");
    private static Resource xmlContextWithRepo = new ClassPathResource("xmlContextWithRepository.xml");
    private static Resource xmlContextWithChangedRepo = new ClassPathResource("xmlContextWithChangedRepository.xml");


    @Before
    public void before() throws IOException {
        BaseTestUtil.configMaxReloadTimes();
        if (xmlApplicationContext == null) {
            writeRepositoryToXml();
            xmlApplicationContext = new ClassPathXmlApplicationContext("xmlContext.xml");
        }
        swappingRule.setBeanFactory(xmlApplicationContext.getBeanFactory());
        System.out.println("SpringPlugin2Test.before." + xmlApplicationContext.getBeanFactory());

    }

    @After
    public void after() throws IOException {

    }

    private void writeRepositoryToXml() throws IOException {
        Files.copy(xmlContextWithRepo.getFile().toPath(), xmlContext.getFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeChangedRepositoryToXml() throws IOException {
        Files.copy(xmlContextWithChangedRepo.getFile().toPath(), xmlContext.getFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void swapXmlTest() throws IOException {
        System.out.println("SpringPlugin2Test.swapXmlTest");
        byte[] content = Files.readAllBytes(xmlContext.getFile().toPath());
        try {
            BeanService beanService = xmlApplicationContext.getBean("beanService", BeanService.class);
            Assert.assertEquals(beanService.hello(), "Hello from Repository ServiceWithAspect");

            writeChangedRepositoryToXml();
            assertTrue(WaitHelper.waitForCommand(new WaitHelper.Command() {
                @Override
                public boolean result() throws Exception {
                    return BaseTestUtil.finishReloading(xmlApplicationContext.getBeanFactory(), 1);
                }
            }, 10000));

            Assert.assertEquals(beanService.hello(), "Hello from ChangedRepository ServiceWithAspect");

            writeRepositoryToXml();
            assertTrue(WaitHelper.waitForCommand(new WaitHelper.Command() {
                @Override
                public boolean result() throws Exception {
                    return BaseTestUtil.finishReloading(xmlApplicationContext.getBeanFactory(), 2);
                }
            }, 10000));
            Assert.assertEquals(beanService.hello(), "Hello from Repository ServiceWithAspect");
        } finally {
            Files.write(xmlContext.getFile().toPath(), content);
        }
    }

}
