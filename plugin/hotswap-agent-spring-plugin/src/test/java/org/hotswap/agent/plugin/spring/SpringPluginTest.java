
package org.hotswap.agent.plugin.spring;

import org.hotswap.agent.plugin.hotswapper.HotSwapper;
import org.hotswap.agent.plugin.spring.reload.BeanFactoryAssistant;
import org.hotswap.agent.plugin.spring.testBeans.*;
import org.hotswap.agent.plugin.spring.testBeans.iabpp.BeanService;
import org.hotswap.agent.plugin.spring.testBeans.iabpp.BeanServiceImplNoAspect;
import org.hotswap.agent.plugin.spring.testBeansHotswap.BeanPrototype2;
import org.hotswap.agent.plugin.spring.testBeansHotswap.BeanRepository2;
import org.hotswap.agent.plugin.spring.testBeansHotswap.BeanServiceImpl2;
import org.hotswap.agent.plugin.spring.testBeansHotswap.BeanServiceImpl2NoAspect;
import org.hotswap.agent.plugin.spring.testBeansHotswap.Pojo2;
import org.hotswap.agent.util.ReflectionHelper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class SpringPluginTest {

    @Autowired
    AbstractApplicationContext applicationContext;

    @Rule
    public ClassSwappingRule swappingRule = new ClassSwappingRule();

    volatile int reloadTimes = 1;

    @Before
    public void before() {
        BaseTestUtil.configMaxReloadTimes();
        System.out.println("SpringPluginTest.before." + applicationContext.getBeanFactory());
        swappingRule.setBeanFactory(applicationContext.getBeanFactory());
        reloadTimes = 1;
        BeanFactoryAssistant.getBeanFactoryAssistant(applicationContext.getBeanFactory()).reset();

    }

    @After
    public void after() {
        System.out.println("SpringPluginTest.after." + applicationContext.getBeanFactory());

    }


    @Test
    public void basicTest() {
        assertEquals("Hello from Repository ServiceWithAspect", applicationContext.getBean(org.hotswap.agent.plugin.spring.testBeans.BeanService.class).hello());
        assertEquals("Hello from Repository ServiceWithAspect Prototype",
                applicationContext.getBean(BeanPrototype.class).hello());
    }


    @Test
    public void hotswapServiceTest() throws Exception {
        BeanServiceImpl bean = applicationContext.getBean(BeanServiceImpl.class);
        assertEquals("Hello from Repository ServiceWithAspect", bean.hello());
        swapClasses(BeanServiceImpl.class, BeanServiceImpl2.class);
        assertEquals("Hello from ChangedRepository Service2WithAspect", bean.hello());

        assertEquals("Hello from ChangedRepository Service2WithAspect",
                applicationContext.getBean(org.hotswap.agent.plugin.spring.testBeans.BeanService.class).hello());

        swapClasses(BeanServiceImpl.class, BeanServiceImpl.class);
    }


    @Test
    public void hotswapServiceAddMethodTest() throws Exception {
        printDetail();
        swapClasses(BeanServiceImpl.class, BeanServiceImpl2.class);
        printDetail();


        String helloNewMethodIfaceVal = (String) ReflectionHelper.invoke(applicationContext.getBean(org.hotswap.agent.plugin.spring.testBeans.BeanService.class),
                BeanServiceImpl.class, "helloNewMethod", new Class[]{});
        assertEquals("Hello from helloNewMethod Service2", helloNewMethodIfaceVal);


        String helloNewMethodImplVal = (String) ReflectionHelper.invoke(
                applicationContext.getBean(BeanServiceImpl.class), BeanServiceImpl.class, "helloNewMethod",
                new Class[]{});
        assertEquals("Hello from helloNewMethod Service2", helloNewMethodImplVal);
        swapClasses(BeanServiceImpl.class, BeanServiceImpl.class);
        printDetail();
    }


    @Test
    public void hotswapServiceAddFieldWithInject() throws Exception {
        assertEquals("injected:true", applicationContext.getBean(org.hotswap.agent.plugin.spring.testBeans.BeanService.class).isInjectFieldInjected());
        swapClasses(BeanServiceImpl.class, BeanServiceImpl2.class);
        assertEquals("injectedChanged:true", applicationContext.getBean(org.hotswap.agent.plugin.spring.testBeans.BeanService.class).isInjectFieldInjected());

        swapClasses(BeanServiceImpl.class, BeanServiceImpl.class);
    }

    @Test
    public void hotswapRepositoryTest() throws Exception {
        BeanServiceImpl bean = applicationContext.getBean(BeanServiceImpl.class);
        assertEquals("Hello from Repository ServiceWithAspect", bean.hello());
        swapClasses(BeanRepository.class, BeanRepository2.class);
        assertEquals("Hello from ChangedRepository2 ServiceWithAspect", bean.hello());

        swapClasses(BeanServiceImpl.class, BeanServiceImpl.class);
    }

    @Test
    public void hotswapRepositoryNewMethodTest() throws Exception {
        assertEquals("Hello from Repository ServiceWithAspect",
                applicationContext.getBean(BeanServiceImpl.class).hello());

        swapClasses(BeanRepository.class, BeanRepository2.class);

        String helloNewMethodImplVal = (String) ReflectionHelper.invoke(
                applicationContext.getBean("beanRepository", BeanRepository.class), BeanRepository.class,
                "helloNewMethod", new Class[]{});
        assertEquals("Repository new method", helloNewMethodImplVal);
    }

    @Test
    public void hotswapPrototypeTestNewInstance() throws Exception {
        BeanServiceImpl c1 = applicationContext.getBean("beanServiceImpl", BeanServiceImpl.class);
        BeanServiceImpl c2 = applicationContext.getBean(BeanServiceImpl.class);
        System.out.println("xxxxxxxxxx:" + c1 + ", " + c1.hello());
        System.out.println("xxxxxxxxxx:" + c2 + ", " + c2.hello());
        assertEquals("Hello from Repository ServiceWithAspect Prototype",
                applicationContext.getBean(BeanPrototype.class).hello());


        swapClasses(BeanServiceImpl.class, BeanServiceImpl2.class);
        assertEquals("Hello from ChangedRepository Service2WithAspect Prototype",
                applicationContext.getBean(BeanPrototype.class).hello());



        swapClasses(BeanPrototype.class, BeanPrototype2.class);
        assertEquals("Hello from Repository Prototype2", applicationContext.getBean(BeanPrototype.class).hello());

        swapClasses(BeanServiceImpl.class, BeanServiceImpl.class);
        swapClasses(BeanPrototype.class, BeanPrototype.class);
    }

    @Test
    public void hotswapPrototypeTestExistingInstance() throws Exception {
        BeanPrototype beanPrototypeInstance = applicationContext.getBean(BeanPrototype.class);
        assertEquals("Hello from Repository ServiceWithAspect Prototype", beanPrototypeInstance.hello());
        System.out.println("xxxxxxxxxx###1:" + beanPrototypeInstance + ", " + beanPrototypeInstance.hello());

        swapClasses(BeanServiceImpl.class, BeanServiceImpl2.class);
        assertEquals("Hello from ChangedRepository Service2WithAspect Prototype", beanPrototypeInstance.hello());
        System.out.println("xxxxxxxxxx###2:" + beanPrototypeInstance + ", " + beanPrototypeInstance.hello());

        swapClasses(BeanServiceImpl.class, BeanServiceImpl.class);
        assertEquals("Hello from Repository ServiceWithAspect Prototype", beanPrototypeInstance.hello());
    }

    @Test
    public void hotswapBeanCreatedByIABPP() throws Exception {
        BeanService beanService = applicationContext.getBean(BeanService.class);
        assertEquals("Hello from Repository Service", beanService.hello());

        swapClasses(BeanServiceImplNoAspect.class, BeanServiceImpl2NoAspect.class);
        assertEquals("Hello from ChangedRepository Service2", beanService.hello());


        swapClasses(BeanServiceImplNoAspect.class, BeanServiceImplNoAspect.class);
        assertEquals("Hello from Repository Service", beanService.hello());
    }

    @Test
    public void pojoTest() throws Exception {
        Pojo pojo = new Pojo();
        assertEquals(0, applicationContext.getBeanNamesForType(Pojo.class).length);

        HotSwapper.swapClasses(Pojo.class, Pojo2.class.getName());
        Thread.sleep(8000);
        assertEquals(0, applicationContext.getBeanNamesForType(Pojo.class).length);
    }

    private void swapClasses(Class<?> original, Class<?> swap) throws Exception {
        swappingRule.swapClasses(original, swap, reloadTimes++);
    }

    @Ignore
    @Test
    public void beanLookupTest() {
        BeanLookup beanLookup = applicationContext.getBean(BeanLookup.class);
        String hello = beanLookup.getBeanPrototype().hello();
        Assert.assertEquals(hello, "Hello from Repository ServiceWithAspect Prototype");
    }

    private void printDetail() {
        BeanServiceImpl c1 = applicationContext.getBean(BeanServiceImpl.class);
        BeanServiceImpl c2 = applicationContext.getBean(BeanServiceImpl.class);
        System.out.println("xxxxxxxxxx:" + c1 + ", " + c1.hello());
        System.out.println("xxxxxxxxxx:" + c2 + ", " + c2.hello());
    }
}
