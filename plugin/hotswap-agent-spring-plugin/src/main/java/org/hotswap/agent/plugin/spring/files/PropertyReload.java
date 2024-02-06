
package org.hotswap.agent.plugin.spring.files;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.core.BeanFactoryProcessor;
import org.hotswap.agent.plugin.spring.listener.SpringEventSource;
import org.hotswap.agent.plugin.spring.transformers.api.ReloadablePropertySource;
import org.hotswap.agent.plugin.spring.transformers.api.ReloadableResourcePropertySource;
import org.hotswap.agent.plugin.spring.utils.AnnotatedBeanDefinitionUtils;
import org.hotswap.agent.util.ReflectionHelper;
import org.hotswap.agent.util.spring.util.ObjectUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.function.Consumer;

public class PropertyReload {
    private static AgentLogger LOGGER = AgentLogger.getLogger(PropertyReload.class);


    public static void reloadPropertySource(DefaultListableBeanFactory beanFactory) {
        ConfigurableEnvironment environment = beanFactory.getBean(ConfigurableEnvironment.class);
        if (environment != null) {
            Map<String, String> oldValueMap = getPropertyOfPropertySource(environment);

            doReloadPropertySource(environment.getPropertySources());

            processChangedValue(beanFactory, environment, oldValueMap);
        }

        refreshPlaceholderConfigurerSupport(beanFactory);
    }

    private static Map<String, String> getPropertyOfPropertySource(ConfigurableEnvironment environment) {
        Set<String> canModifiedKey = new HashSet<>();
        Map<String, String> result = new HashMap<>();

        processKeysOfPropertySource(environment.getPropertySources(), canModifiedKey::addAll);

        for (String key : canModifiedKey) {
            result.put(key, environment.getProperty(key));
        }
        return result;
    }

    private static void processChangedValue(DefaultListableBeanFactory beanFactory, ConfigurableEnvironment environment,
                                            Map<String, String> oldValueMap) {
        Set<String> canModifiedKey = new HashSet<>();
        processKeysOfPropertySource(environment.getPropertySources(), canModifiedKey::addAll);

        List<PropertiesChangeEvent.PropertyChangeItem> propertyChangeItems = new ArrayList<>();
        for (String key : canModifiedKey) {
            String oldValue = oldValueMap.get(key);
            String newValue = environment.getProperty(key);
            if ((oldValue != null && !oldValue.equals(newValue)) || (oldValue == null && newValue != null)) {
                propertyChangeItems.add(new PropertiesChangeEvent.PropertyChangeItem(key, oldValue, newValue));
                LOGGER.debug("property of '{}' reload, key:{}, oldValue:{}, newValue:{}",
                        ObjectUtils.identityToString(beanFactory), key, oldValue, newValue);
            }
        }
        if (!propertyChangeItems.isEmpty()) {
            SpringEventSource.INSTANCE.fireEvent(new PropertiesChangeEvent(propertyChangeItems, beanFactory));
        }
    }

    private static void processKeysOfPropertySource(MutablePropertySources propertySources, Consumer<Set<String>> consumer) {
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof MapPropertySource) {
                consumer.accept(((MapPropertySource) propertySource).getSource().keySet());
            }
        }
    }


    private static void doReloadPropertySource(MutablePropertySources propertySources) {
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof ReloadableResourcePropertySource) {
                try {
                    ((ReloadableResourcePropertySource) propertySource).reload();
                } catch (IOException e) {
                    LOGGER.error("reload property source error", e, propertySource.getName());
                }
            }
            if (propertySource instanceof ReloadablePropertySource) {
                ((ReloadablePropertySource) propertySource).reload();
            }
        }
    }

    private static void refreshPlaceholderConfigurerSupport(DefaultListableBeanFactory beanFactory) {
        String[] beanFactoryBeanNamesForTypes = beanFactory.getBeanNamesForType(PlaceholderConfigurerSupport.class);
        if (beanFactoryBeanNamesForTypes != null) {
            for (String beanFactoryBeanName : beanFactoryBeanNamesForTypes) {
                PlaceholderConfigurerSupport placeholderConfigurerSupport = beanFactory.getBean(beanFactoryBeanName, PlaceholderConfigurerSupport.class);
                refreshSinglePlaceholderConfigurerSupport(beanFactory, placeholderConfigurerSupport);
            }
        }
    }


    private static void refreshSinglePlaceholderConfigurerSupport(DefaultListableBeanFactory beanFactory, PlaceholderConfigurerSupport placeholderConfigurerSupport) {
        if (placeholderConfigurerSupport instanceof PropertySourcesPlaceholderConfigurer) {
            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = (PropertySourcesPlaceholderConfigurer) placeholderConfigurerSupport;


            MutablePropertySources origPropertySources = getPropertySources(propertySourcesPlaceholderConfigurer);

            origPropertySources.forEach(propertySource -> origPropertySources.remove(propertySource.getName()));
            ReflectionHelper.set(propertySourcesPlaceholderConfigurer, "propertySources", null);

            propertySourcesPlaceholderConfigurer.postProcessBeanFactory(beanFactory);

            MutablePropertySources curPropertySources = getPropertySources(propertySourcesPlaceholderConfigurer);

            curPropertySources.forEach(propertySource -> origPropertySources.addLast(propertySource));


            ReflectionHelper.set(propertySourcesPlaceholderConfigurer, "propertySources", origPropertySources);
        } else if (placeholderConfigurerSupport instanceof PropertyPlaceholderConfigurer) {
            PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = (PropertyPlaceholderConfigurer) placeholderConfigurerSupport;
            propertyPlaceholderConfigurer.postProcessBeanFactory(beanFactory);
        }
    }

    private static MutablePropertySources getPropertySources(PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer) {
        return (MutablePropertySources) ReflectionHelper.getNoException(propertySourcesPlaceholderConfigurer, propertySourcesPlaceholderConfigurer.getClass(), "propertySources");
    }


    public static Set<String> getContainValueAnnotationBeans(DefaultListableBeanFactory beanFactory) {
        Set<String> needRecreateBeans = new HashSet<>();

        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                if (beanDefinition instanceof RootBeanDefinition) {
                    RootBeanDefinition currentBeanDefinition = (RootBeanDefinition) beanDefinition;
                    if (containValueAnnotationInMethod(beanFactory, currentBeanDefinition)) {
                        needRecreateBeans.add(beanName);
                    }
                } else if (beanDefinition instanceof GenericBeanDefinition) {
                    GenericBeanDefinition currentBeanDefinition = (GenericBeanDefinition) beanDefinition;
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) currentBeanDefinition;
                    if (AnnotatedBeanDefinitionUtils.getFactoryMethodMetadata(annotatedBeanDefinition) != null) {
                        continue;
                    }
                    if (BeanFactoryProcessor.needReloadOnConstructor(beanFactory, currentBeanDefinition, beanName, constructors -> checkConstructorContainsValueAnnotation(constructors))) {
                        needRecreateBeans.add(beanName);
                    }
                }
            }
        }
        return needRecreateBeans;
    }

    private static boolean containValueAnnotationInMethod(DefaultListableBeanFactory beanFactory, RootBeanDefinition currentBeanDefinition) {
        if (currentBeanDefinition.getFactoryMethodName() != null && currentBeanDefinition.getFactoryBeanName() != null) {
            Method method = currentBeanDefinition.getResolvedFactoryMethod();
            if (method == null) {
                Object factoryBean = beanFactory.getBean(currentBeanDefinition.getFactoryBeanName());
                Class factoryClass = ClassUtils.getUserClass(factoryBean.getClass());
                Method[] methods = getCandidateMethods(factoryClass, currentBeanDefinition);
                for (Method m : methods) {
                    if (!Modifier.isStatic(m.getModifiers()) && currentBeanDefinition.isFactoryMethod(m) &&
                            m.getParameterCount() != 0 && AnnotatedBeanDefinitionUtils.containValueAnnotation(m.getParameterAnnotations())) {
                        return true;
                    }
                }
            } else if (method.getParameterCount() != 0) {

                if (AnnotatedBeanDefinitionUtils.containValueAnnotation(method.getParameterAnnotations())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Method[] getCandidateMethods(Class<?> factoryClass, RootBeanDefinition mbd) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Method[]>) () ->
                    (mbd.isNonPublicAccessAllowed() ?
                            ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods()));
        } else {
            return (mbd.isNonPublicAccessAllowed() ?
                    ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods());
        }
    }

    private static boolean checkConstructorContainsValueAnnotation(Constructor<?>[] constructors) {
        for (Constructor constructor : constructors) {
            if (constructor.getParameterCount() != 0 && AnnotatedBeanDefinitionUtils.containValueAnnotation(constructor.getParameterAnnotations())) {
                return true;
            }
        }
        return false;
    }
}
