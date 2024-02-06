
package org.hotswap.agent.plugin.spring.getbean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class HotswapSpringInvocationHandler extends DetachableBeanHolder implements InvocationHandler {

    private static final long serialVersionUID = 8037007940960065166L;


    public HotswapSpringInvocationHandler(Object bean, Object beanFactry, Class<?>[] paramClasses, Object[] paramValues) {
        super(bean, beanFactry, paramClasses, paramValues);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getWrappedObject")
                && method.getDeclaringClass().getName().equals("org.springframework.core.InfrastructureProxy")) {
            for (Class<?> beanInterface : getBean().getClass().getInterfaces()) {
                if (beanInterface.getName().equals("org.springframework.core.InfrastructureProxy")) {
                    return doInvoke(method, args);
                }
            }
            return getBean();
        }
        return doInvoke(method, args);
    }

    private Object doInvoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(getBean(), args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}