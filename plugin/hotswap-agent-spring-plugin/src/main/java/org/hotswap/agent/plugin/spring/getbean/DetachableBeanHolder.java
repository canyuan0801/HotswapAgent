
package org.hotswap.agent.plugin.spring.getbean;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hotswap.agent.logging.AgentLogger;


public class DetachableBeanHolder implements Serializable {

    private static final long serialVersionUID = -7443802320153815102L;

    private Object bean;
    private Object beanFactory;
    private Class<?>[] paramClasses;
    private Object[] paramValues;
    private static List<WeakReference<DetachableBeanHolder>> beanProxies =
            Collections.synchronizedList(new ArrayList<WeakReference<DetachableBeanHolder>>());
    private static AgentLogger LOGGER = AgentLogger.getLogger(DetachableBeanHolder.class);


    public DetachableBeanHolder(Object bean, Object beanFactry, Class<?>[] paramClasses, Object[] paramValues) {
        if (bean == null) {
            LOGGER.error("Bean is null. The param value: {}", Arrays.toString(paramValues));
        }
        this.bean = bean;
        this.beanFactory = beanFactry;
        this.paramClasses = paramClasses;
        this.paramValues = paramValues;
        beanProxies.add(new WeakReference<DetachableBeanHolder>(this));
    }


    public static void detachBeans() {
        int i = 0;
        synchronized (beanProxies) {
            while (i < beanProxies.size()) {
                DetachableBeanHolder beanHolder = beanProxies.get(i).get();
                if (beanHolder != null) {
                    beanHolder.detach();
                    i++;
                } else {
                    beanProxies.remove(i);
                }
            }
        }
        if (i > 0) {
            LOGGER.info("{} Spring proxies reset", i);
        } else {
            LOGGER.debug("No spring proxies reset");
        }
    }


    public void detach() {
        bean = null;
    }



    public void setTarget(Object bean) {
        this.bean = bean;
    }


    public Object getTarget() {
        return bean;
    }


    public Object getBean() throws IllegalAccessException, InvocationTargetException {
        Object beanCopy = bean;
        if (beanCopy == null) {
            Method[] methods = beanFactory.getClass().getMethods();
            for (Method factoryMethod : methods) {
                if (ProxyReplacer.FACTORY_METHOD_NAME.equals(factoryMethod.getName())
                        && Arrays.equals(factoryMethod.getParameterTypes(), paramClasses)) {

                    Object freshBean = factoryMethod.invoke(beanFactory, paramValues);






                    if (freshBean instanceof SpringHotswapAgentProxy) {
                        freshBean = ((SpringHotswapAgentProxy) freshBean).$$ha$getTarget();
                    }
                    bean = freshBean;
                    beanCopy = bean;
                    if (beanCopy == null) {
                        LOGGER.debug("Bean of '{}' not loaded, {} ", bean.getClass().getName(), paramValues);
                        break;
                    }
                    LOGGER.info("Bean '{}' loaded", bean.getClass().getName());
                    break;
                }
            }
        }
        return beanCopy;
    }

    protected boolean isBeanLoaded(){
        return bean != null;
    }
}