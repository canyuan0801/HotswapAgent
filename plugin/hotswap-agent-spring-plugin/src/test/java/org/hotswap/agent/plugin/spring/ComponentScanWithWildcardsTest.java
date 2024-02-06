
package org.hotswap.agent.plugin.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.LoaderClassPath;
import org.hotswap.agent.plugin.hotswapper.HotSwapper;
import org.hotswap.agent.plugin.spring.reload.BeanFactoryAssistant;
import org.hotswap.agent.plugin.spring.wildcardstest.beans.hotswap.BeanServiceImpl2;
import org.hotswap.agent.plugin.spring.wildcardstest.beans.hotswap.NewHelloServiceImpl;
import org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans.BeanService;
import org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans.BeanServiceImpl;
import org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans.NewHelloService;
import org.hotswap.agent.util.test.WaitHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:wildcardsApplicationContext.xml"})
public class ComponentScanWithWildcardsTest {

    @Autowired
    AutowireCapableBeanFactory factory;

    @Autowired
    private AbstractApplicationContext context;

    private volatile int reloadTimes = 1;

    @Before
    public void before() {
        BaseTestUtil.configMaxReloadTimes();
        System.out.println("ComponentScanWithWildcardsTest.before" + context.getBeanFactory());
        reloadTimes = 1;
        BeanFactoryAssistant.getBeanFactoryAssistant(context.getBeanFactory()).reset();
    }

    @Test
    public void testHotswapService() throws Exception {
        BeanServiceImpl bean = factory.getBean(BeanServiceImpl.class);
        try {

            assertEquals("Hello from Repository ServiceWithAspect", bean.hello());


            swapClasses(BeanServiceImpl.class, BeanServiceImpl2.class.getName());


            assertEquals("Hello from ChangedRepository Service2WithAspect", bean.hello());
            assertEquals("Hello from ChangedRepository Service2WithAspect", factory.getBean(BeanService.class).hello());
        } finally {
            swapClasses(BeanServiceImpl.class, BeanServiceImpl.class.getName());
            bean = factory.getBean(BeanServiceImpl.class);
            assertEquals("Hello from Repository ServiceWithAspect", bean.hello());
        }
    }

    @Test
    public void testNewServiceClassFile() throws Exception {
        File file = null;
        try {

            assertNoBean(NewHelloService.class);


            file = copyClassFileAndWaitForReload(NewHelloServiceImpl.class,
                    "org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans.NewHelloServiceImpl");


            assertEquals("Hello from Repository NewHelloServiceWithAspect",
                    factory.getBean(NewHelloService.class).hello());
        } finally {
            if (file != null && file.exists()) {

                System.gc();

                if (!file.delete()) {
                    System.err.println("Warning - unable to delete class file " + file.getAbsolutePath() +
                            ". Run maven clean project or delete the file manually before running subsequent test runs.");
                }
            }
        }
    }

    private void assertNoBean(Class<?> clazz) {
        try {
            factory.getBean(clazz);
            fail();
        } catch (NoSuchBeanDefinitionException e) {

        }
    }

    private void swapClasses(Class<?> original, String swap) throws Exception {
        HotSwapper.swapClasses(original, swap);
        waitForReload(8000);
    }

    public File copyClassFileAndWaitForReload(Class<?> clazz, String newName) throws Exception {
        File file = copyClassFile(clazz, newName);
        try {
            waitForReload(8000);
        } catch (Throwable e) {
            file.delete();
            throw e;
        }
        return file;
    }

    private void waitForReload(int timeout) throws InterruptedException {
        int rt = reloadTimes++;
        assertTrue(WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return BaseTestUtil.finishReloading(context.getBeanFactory(), rt);
            }
        }, timeout));


        Thread.sleep(100);
    }

    public File copyClassFile(Class<?> clazz, String newName) throws Exception {
        String directoryName = clazz.getClassLoader().getResource("").getPath();
        ClassPool classPool = new ClassPool();
        classPool.appendClassPath(new LoaderClassPath(clazz.getClassLoader()));
        CtClass ctClass = classPool.getAndRename(clazz.getName(), newName);
        ctClass.writeFile(directoryName);
        File file = new File(directoryName + File.separatorChar + newName.replace('.', File.separatorChar) + ".class");
        assertTrue(file.exists());
        return file;
    }

}
