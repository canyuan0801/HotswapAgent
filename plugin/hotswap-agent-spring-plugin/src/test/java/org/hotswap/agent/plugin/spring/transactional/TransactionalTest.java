package org.hotswap.agent.plugin.spring.transactional;

import org.hotswap.agent.plugin.spring.BaseTestUtil;
import org.hotswap.agent.plugin.spring.ClassSwappingRule;
import org.hotswap.agent.plugin.spring.reload.SpringChangedAgent;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = TransactionalApplication.class)})
public class TransactionalTest {
    @Rule
    public ClassSwappingRule swappingRule = new ClassSwappingRule();

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentTransactionalService1 studentTransactionalService;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Before
    public void before() {
        BaseTestUtil.configMaxReloadTimes();
        swappingRule.setBeanFactory(beanFactory);
        SpringChangedAgent.getInstance((DefaultListableBeanFactory) beanFactory);
    }

    @After
    public void after() {
        SpringChangedAgent.destroyBeanFactory((DefaultListableBeanFactory) beanFactory);
    }

    @Test
    @Ignore
    public void transactionalTest() throws Exception {
        System.out.println("TransactionalTest.transactionalTest." + beanFactory);

        studentService.createTable();


        String name1 = "name1";
        Assert.assertEquals(1, studentService.insertOriginalData(name1));


        String name2 = "name2";
        try {
            studentTransactionalService.changeName(name1, name2, new IOException());
        } catch (Exception ignored) {
        }
        Assert.assertEquals(name1, studentService.findName(name1));


        int reloadTimes = 1;
        swappingRule.swapClasses(StudentTransactionalService1.class, StudentTransactionalService2.class, reloadTimes++);


        try {
            studentTransactionalService.changeName(name1, name2, new IOException());
        } catch (Exception ignored) {
        }
        Assert.assertEquals(name2, studentService.findName(name2));
    }

}
