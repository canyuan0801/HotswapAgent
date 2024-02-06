
package org.hotswap.agent.plugin.weld_jakarta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import org.hotswap.agent.plugin.hotswapper.HotSwapper;
import org.hotswap.agent.plugin.weld_jakarta.command.BeanClassRefreshAgent;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.DependentHello1;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.HelloProducer1;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.HelloService;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.HelloServiceDependant;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.HelloServiceImpl1;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.InterceptedBean;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.ProxyHello1;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.ProxyHosting;
import org.hotswap.agent.plugin.weld_jakarta.testBeans.SessionBean1;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.DependentHello2;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.HelloProducer2;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.HelloProducer3;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.HelloServiceImpl2;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.InterceptedBean2;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.ProxyHello2;
import org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap.SessionBean2;
import org.hotswap.agent.util.ReflectionHelper;
import org.hotswap.agent.util.test.WaitHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


@ApplicationScoped
@RunWith(WeldJUnit4Runner.class)
public class WeldJakartaPluginTest {

    public <T> T getBeanInstance(Class<T> beanClass) {
        BeanManager beanManager = CDI.current().getBeanManager();
        Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(beanClass));
        T result = beanManager.getContext(bean.getScope()).get(bean, beanManager.createCreationalContext(bean));
        return result;
    }

    public static <T> T getReference(Class<T> beanClass) {
        BeanManager beanManager = CDI.current().getBeanManager();
        Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(beanClass));
        if (bean != null) {
            return (T) beanManager.getReference(bean, beanClass, beanManager.createCreationalContext(bean));
        }
        return null;
    }

    
    @Test
    public void basicTest() {
        assertEquals("HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(HelloService.class).hello());
        assertEquals("DependentHello1.hello():HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(DependentHello1.class).hello());
    }

    
    @Test
    @Ignore
    public void hotswapServiceTest() throws Exception {

        HelloServiceImpl1 bean = getBeanInstance(HelloServiceImpl1.class);
        assertEquals("HelloServiceImpl1.hello():HelloProducer1.hello()", bean.hello());

        swapClasses(HelloServiceImpl1.class, HelloServiceImpl2.class.getName());
        assertEquals("null:HelloProducer2.hello()", bean.hello());

        
        HelloServiceImpl1.class.getMethod("initName", new Class[0]).invoke(bean, new Object[0]);
        assertEquals("HelloServiceImpl2.hello(initialized):HelloProducer2.hello()", getBeanInstance(HelloServiceImpl1.class).hello());
        
        assertEquals("HelloServiceImpl2.hello(initialized):HelloProducer2.hello()", getBeanInstance(HelloService.class).hello());

        
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl1.class.getName());
        assertEquals("HelloServiceImpl1.hello():HelloProducer1.hello()", bean.hello());

    }

    
    @Test
    @Ignore
    public void hotswapSeviceAddMethodTest() throws Exception {
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl2.class.getName());

        String helloNewMethodIfaceVal = (String) ReflectionHelper.invoke(getBeanInstance(HelloService.class),
                HelloServiceImpl1.class, "helloNewMethod", new Class[]{});
        assertEquals("HelloServiceImpl2.helloNewMethod()", helloNewMethodIfaceVal);

        String helloNewMethodImplVal = (String) ReflectionHelper.invoke(getBeanInstance(HelloServiceImpl1.class),
                HelloServiceImpl1.class, "helloNewMethod", new Class[]{});
        assertEquals("HelloServiceImpl2.helloNewMethod()", helloNewMethodImplVal);

        
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl1.class.getName());
        assertEquals("HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(HelloServiceImpl1.class).hello());
    }

    @Test
    @Ignore
    public void hotswapRepositoryTest() throws Exception {
        HelloServiceDependant bean = getBeanInstance(HelloServiceDependant.class);
        assertEquals("HelloServiceDependant.hello():HelloProducer1.hello()", bean.hello());

        swapClasses(HelloProducer1.class, HelloProducer2.class.getName());
        assertEquals("HelloServiceDependant.hello():HelloProducer2.hello()", bean.hello());

        swapClasses(HelloProducer1.class, HelloProducer3.class.getName());
        try{
            assertEquals("HelloProducer3.hello():HelloProducer2.hello()", bean.hello());
        } catch (NullPointerException npe){
            System.out.println("INFO: dependant beans are not reinjected now.");
        }
        assertEquals("HelloServiceDependant.hello():HelloProducer3.hello():HelloProducer2.hello()",
                getBeanInstance(HelloServiceDependant.class).hello());

        
        swapClasses(HelloProducer1.class, HelloProducer1.class.getName());
        assertEquals("HelloServiceDependant.hello():HelloProducer1.hello()", bean.hello());
    }

    @Test
    @Ignore
    public void hotswapRepositoryNewMethodTest() throws Exception {
        assertEquals("HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(HelloServiceImpl1.class).hello());
        swapClasses(HelloProducer1.class, HelloProducer3.class.getName());
        String helloNewMethodImplVal = (String) ReflectionHelper.invoke(getBeanInstance(HelloProducer1.class),
                HelloProducer1.class, "helloNewMethod", new Class[]{});
        assertEquals("HelloProducer3.helloNewMethod()", helloNewMethodImplVal);

        
        swapClasses(HelloProducer1.class, HelloProducer1.class.getName());
        assertEquals("HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(HelloServiceImpl1.class).hello());
    }

    @Test
    @Ignore
    public void hotswapPrototypeTest() throws Exception {
        assertEquals("DependentHello1.hello():HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(DependentHello1.class).hello());

        
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl2.class.getName());

        assertEquals("DependentHello1.hello():null:HelloProducer2.hello()", getBeanInstance(DependentHello1.class).hello());
        HelloServiceImpl1.class.getMethod("initName", new Class[0]).invoke(getBeanInstance(HelloServiceImpl1.class), new Object[0]);
        assertEquals("DependentHello1.hello():HelloServiceImpl2.hello(initialized):HelloProducer2.hello()", getBeanInstance(DependentHello1.class).hello());

        
        swapClasses(DependentHello1.class, DependentHello2.class.getName());
        assertEquals("DependentHello2.hello():HelloProducer1.hello()", getBeanInstance(DependentHello1.class).hello());

        
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl1.class.getName());
        swapClasses(DependentHello1.class, DependentHello1.class.getName());
        assertEquals("DependentHello1.hello():HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(DependentHello1.class).hello());
    }


    @Test
    @Ignore
    public void hotswapPrototypeTestNotFailWhenHoldingInstanceBecauseSingletonInjectionPointWasReinitialize() throws Exception {
        DependentHello1 dependentBeanInstance = getBeanInstance(DependentHello1.class);
        assertEquals("DependentHello1.hello():HelloServiceImpl1.hello():HelloProducer1.hello()", dependentBeanInstance.hello());

        
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl2.class.getName());
        ReflectionHelper.invoke(getBeanInstance(HelloService.class),
                HelloServiceImpl1.class, "initName", new Class[]{});
        assertEquals("DependentHello1.hello():HelloServiceImpl2.hello(initialized):HelloProducer2.hello()", dependentBeanInstance.hello());

        
        swapClasses(HelloServiceImpl1.class, HelloServiceImpl1.class.getName());
        assertEquals("DependentHello1.hello():HelloServiceImpl1.hello():HelloProducer1.hello()", getBeanInstance(DependentHello1.class).hello());
    }


    
    @Test
    @Ignore
    public void newBeanClassIsManagedBeanReRunTestOnlyAfterMvnClean() throws Exception {
        try {
            WeldJakartaPlugin.isTestEnvironment = true;
            Collection<BeanClassRefreshAgent> instances = BeanClassRefreshAgent.getInstances();
            for (BeanClassRefreshAgent instance : instances) {
                
                Class newClass = HotSwapper.newClass("NewClass", instance.getBdaId(), getClass().getClassLoader());
                URL resource = newClass.getClassLoader().getResource("NewClass.class");
                Thread.sleep(1000); 
                Object bean = getBeanInstance(newClass);
                assertNotNull(bean);
                break;
            }
        } finally {
            WeldJakartaPlugin.isTestEnvironment = false;
        }
    }

    @Test
    @Ignore
    public void proxyTest() throws Exception {
        ProxyHosting proxyHosting = getBeanInstance(ProxyHosting.class);
        assertEquals("ProxyHello1.hello()", proxyHosting.hello());
        swapClasses(ProxyHello1.class, ProxyHello2.class.getName());
        assertEquals("ProxyHello2.hello()", proxyHosting.hello());

        Object proxy = proxyHosting.proxy;
        String hello2 = (String) ReflectionHelper.invoke(proxy, ProxyHello1.class, "hello2", new Class[]{}, null);
        assertEquals("ProxyHello2.hello2()", hello2);

        
        swapClasses(ProxyHello1.class, ProxyHello1.class.getName());
        assertEquals("ProxyHello1.hello()", proxyHosting.hello());
    }

    @Test
    @Ignore
    public void sessionBeanTest() throws Exception {
        SessionBean1 sessionBean = getBeanInstance(SessionBean1.class);
        assertEquals("SessionBean1.hello():ProxyHello1.hello()", sessionBean.hello());
        swapClasses(SessionBean1.class, SessionBean2.class.getName());

        assertEquals("SessionBean2.hello():ProxyHello2.hello():ProxyHello1.hello()", sessionBean.hello());

        
        swapClasses(SessionBean1.class, SessionBean1.class.getName());
        assertEquals("SessionBean1.hello():ProxyHello1.hello()", sessionBean.hello());
    }

    @Test
    @Ignore
    public void interceptedBeanTest() throws Exception {
        InterceptedBean interceptedBean = getReference(InterceptedBean.class);
        assertEquals("TestInterceptor:InterceptedBean.hello()", interceptedBean.hello());
        swapClasses(InterceptedBean.class, InterceptedBean2.class.getName());

        assertEquals("InterceptedBean2.hello():TestInterceptor:InterceptedBean2.hello2()", interceptedBean.hello());

        
        swapClasses(InterceptedBean.class, InterceptedBean.class.getName());
        String s = interceptedBean.hello();
        System.out.println(s);
        assertEquals("TestInterceptor:InterceptedBean.hello()", s);
    }

    private void swapClasses(Class original, String swap) throws Exception {
        BeanClassRefreshAgent.reloadFlag = true;
        HotSwapper.swapClasses(original, swap);
        assertTrue(WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return !BeanClassRefreshAgent.reloadFlag;
            }
        }));

        
        Thread.sleep(100);
    }
}
