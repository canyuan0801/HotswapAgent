
package org.hotswap.agent.plugin.spring.files;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.SpringPlugin;
import org.hotswap.agent.plugin.spring.core.BeanFactoryProcessor;
import org.hotswap.agent.plugin.spring.scanner.ClassPathBeanDefinitionScannerAgent;
import org.hotswap.agent.plugin.spring.utils.ResourceUtils;
import org.hotswap.agent.util.spring.util.ObjectUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.hotswap.agent.plugin.spring.utils.ResourceUtils.convertToClasspathURL;


public class XmlBeanDefinitionScannerAgent {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(XmlBeanDefinitionScannerAgent.class);

    public static final String PROPERTY_PLACEHOLDER_CONFIGURER = "org.springframework.beans.factory.config.PropertyPlaceholderConfigurer";
    public static final String PROPERTY_SOURCES_PLACEHOLDER_CONFIGURER = "org.springframework.context.support.PropertySourcesPlaceholderConfigurer";

    private static Map<DefaultListableBeanFactory, Map<String, XmlBeanDefinitionScannerAgent>> beanFactoryToAgentMap = new ConcurrentHashMap<>();
    private static Map<String, XmlBeanDefinitionScannerAgent> pathToAgent = new HashMap<>();
    private static boolean basePackageInited = false;


    private BeanDefinitionReader reader;


    private URL url;


    private Map<String, String> beansRegistered = new HashMap<>();


    private Set<String> propertyLocations = new HashSet<>();


    public static boolean reloadFlag = false;

    public static void registerBean(String beanName, BeanDefinition beanDefinition) {
        XmlBeanDefinitionScannerAgent agent = findAgent(beanDefinition);
        if (agent == null) {
            LOGGER.trace("cannot find registered XmlBeanDefinitionScannerAgent for bean {}", beanName);
            return;
        }
        LOGGER.info("registering bean {} to XmlBeanDefinitionScannerAgent {}", beanName, agent.url);
        registerBeanName(agent, beanName, beanDefinition.getBeanClassName());
        registerPropertyLocations(agent, beanDefinition);
    }


    public static void registerXmlBeanDefinitionScannerAgent(XmlBeanDefinitionReader reader, Resource resource) {
        LOGGER.trace("registerXmlBeanDefinitionScannerAgent, reader: {}, resource: {}, beanFactory:{}", reader,
                resource, ObjectUtils.identityToString(reader.getBeanFactory()));
        BeanDefinitionRegistry beanDefinitionRegistry = reader.getBeanFactory();
        if (beanDefinitionRegistry instanceof DefaultListableBeanFactory) {
            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanDefinitionRegistry;
            Map<String, XmlBeanDefinitionScannerAgent> agentMap = beanFactoryToAgentMap.computeIfAbsent(defaultListableBeanFactory, k -> new HashMap<>());
            fillAgentInstance(reader, resource, agentMap);
            return;
        }
        fillAgentInstance(reader, resource, pathToAgent);
    }

    public static Set<String> reloadXmlsAndGetBean(DefaultListableBeanFactory beanFactory, boolean propertiesChanged,
                                                   Map<String, String> placeHolderXmlRelation,Set<String> recreateBean, Set<URL> xmls) {
        LOGGER.debug("reloadXmlsAndGetBean, propertiesChanged: {}, placeHolderXmlRelation: {}, recreateBean: {}, xmls: {}",
                propertiesChanged, placeHolderXmlRelation, recreateBean, xmls);
        synchronized (xmls) {
            Set<String> xmlResourcePaths = new HashSet<>();
            if (propertiesChanged) {
                xmlResourcePaths.addAll(placeHolderXmlRelation.values());
            }
            Set<String> result = XmlBeanDefinitionScannerAgent.reloadXmls(beanFactory, xmls, xmlResourcePaths);

            xmls.clear();
            return result;
        }
    }

    private static void fillAgentInstance(XmlBeanDefinitionReader reader, Resource resource, Map<String, XmlBeanDefinitionScannerAgent> instances) {
        String path = ResourceUtils.getPath(resource);
        if (path == null) {
            return;
        }

        URL resourceUrl = null;
        try {
            resourceUrl = resource.getURL();
        } catch (IOException e) {

        }

        if (!instances.containsKey(path)) {
            instances.put(path, new XmlBeanDefinitionScannerAgent(reader, resourceUrl));
        }
    }

    public static Set<String> reloadXmls(DefaultListableBeanFactory beanFactory, Set<URL> urls, Set<String> resourcePaths) {
        Set<XmlBeanDefinitionScannerAgent> agents = new HashSet<>(resourcePaths.size() + urls.size());
        for (String resourcePath : resourcePaths) {

            XmlBeanDefinitionScannerAgent xmlBeanDefinitionScannerAgent = getAgent(beanFactory, resourcePath);
            if (xmlBeanDefinitionScannerAgent == null) {
                LOGGER.warning("url " + resourcePath + " is not associated with any XmlBeanDefinitionScannerAgent, not reloading");
                continue;
            }
            try {
                LOGGER.trace("Reloading XML {} since property file changed: {}", xmlBeanDefinitionScannerAgent.url, resourcePath);
                if (agents.add(xmlBeanDefinitionScannerAgent)) {
                    xmlBeanDefinitionScannerAgent.clearCache();
                }
            } catch (org.springframework.beans.factory.parsing.BeanDefinitionParsingException e) {
                LOGGER.error("Reloading XML failed: {}", e.getMessage());
            }
        }
        for (URL url : urls) {
            XmlBeanDefinitionScannerAgent xmlBeanDefinitionScannerAgent = getAgent(beanFactory, convertToClasspathURL(url.getPath()));
            if (xmlBeanDefinitionScannerAgent == null) {
                LOGGER.warning("url " + url + " is not associated with any XmlBeanDefinitionScannerAgent, not reloading");
                continue;
            }
            try {
                LOGGER.trace("Reloading XML {} since xml file changed", url);
                if (agents.add(xmlBeanDefinitionScannerAgent)) {
                    xmlBeanDefinitionScannerAgent.clearCache();
                }
            } catch (org.springframework.beans.factory.parsing.BeanDefinitionParsingException e) {
                LOGGER.error("Reloading XML failed: {}", e.getMessage());
            }
        }
        Set<String> beanNames = new HashSet<>();
        for (XmlBeanDefinitionScannerAgent agent : agents) {
            try {
                agent.reloadDefinition();
                beanNames.addAll(agent.beansRegistered.keySet());
            } catch (org.springframework.beans.factory.parsing.BeanDefinitionParsingException e) {
                LOGGER.error("Reloading XML failed: {}", e.getMessage());
            }
        }
        return beanNames;
    }

    private static XmlBeanDefinitionScannerAgent getAgent(DefaultListableBeanFactory beanFactory, String path) {
        Map<String, XmlBeanDefinitionScannerAgent> instances = beanFactoryToAgentMap.get(beanFactory);
        if (instances != null) {
            XmlBeanDefinitionScannerAgent xmlBeanDefinitionScannerAgent = instances.get(path);
            if (xmlBeanDefinitionScannerAgent != null) {
                return xmlBeanDefinitionScannerAgent;
            }
        }
        return pathToAgent.get(path);
    }

    private static XmlBeanDefinitionScannerAgent findAgent(BeanDefinition beanDefinition) {
        if (!(beanDefinition instanceof AbstractBeanDefinition)) {
            LOGGER.debug("BeanDefinition [{}] is not an instance of AbstractBeanDefinition, ignore", beanDefinition);
            return null;
        }

        if (beanDefinition instanceof AnnotatedBeanDefinition) {
            LOGGER.debug("BeanDefinition [{}] is an instance of AnnotatedBeanDefinition, ignore", beanDefinition);
            return null;
        }

        Resource resource = ((AbstractBeanDefinition) beanDefinition).getResource();
        if (resource == null) {
            LOGGER.debug("BeanDefinition [{}] has no resource, ignore", beanDefinition);
            return null;
        }

        try {
            if (resource instanceof ByteArrayResource) {
                LOGGER.debug("BeanDefinition [{}] has ByteArrayResource as resource, ignore. {}", beanDefinition,
                        new String(((ByteArrayResource) resource).getByteArray()));
                return null;
            }
            String path = convertToClasspathURL(resource.getURL().getPath());
            if (path == null) {
                return null;
            }
            return pathToAgent.get(path);
        } catch (IOException e) {
            LOGGER.warning("Fail to fetch url from resource: {}", resource);
            return null;
        }
    }

    private static void registerBeanName(XmlBeanDefinitionScannerAgent agent, String beanName, String beanClassName) {
        agent.beansRegistered.put(beanName, beanClassName == null ? "" : beanClassName);
    }

    private static void registerPropertyLocations(XmlBeanDefinitionScannerAgent agent, BeanDefinition beanDefinition) {
        String clazz = beanDefinition.getBeanClassName();
        if (!PROPERTY_PLACEHOLDER_CONFIGURER.equals(clazz) && !PROPERTY_SOURCES_PLACEHOLDER_CONFIGURER.equals(clazz)) {
            return;
        }

        PropertyValue pv = beanDefinition.getPropertyValues().getPropertyValue("location");
        if (pv != null && pv.getValue() instanceof TypedStringValue) {
            String location = ((TypedStringValue) pv.getValue()).getValue();
            if (location != null) {
                agent.propertyLocations.add(convertPropertyLocation(location));
            }
        }

        pv = beanDefinition.getPropertyValues().getPropertyValue("locations");
        if (pv != null && pv.getValue() instanceof ManagedList) {
            for (Object o : (ManagedList<?>) pv.getValue()) {
                TypedStringValue value = (TypedStringValue) o;
                String location = value.getValue();
                if (location == null) {
                    continue;
                }

                agent.propertyLocations.add(convertPropertyLocation(location));
            }
        }
    }

    private static String convertPropertyLocation(String location) {
        if (location.startsWith("classpath:")) {
            location = location.substring("classpath:".length());
        } else {
            location = convertToClasspathURL(location);
        }
        return location;
    }

    private XmlBeanDefinitionScannerAgent(BeanDefinitionReader reader, URL url) {
        this.reader = reader;
        this.url = url;

        if (SpringPlugin.basePackagePrefixes != null && !basePackageInited) {
            ClassPathBeanDefinitionScannerAgent xmlBeanDefinitionScannerAgent = ClassPathBeanDefinitionScannerAgent.getInstance(new ClassPathBeanDefinitionScanner(reader.getRegistry()));
            for (String basePackage : SpringPlugin.basePackagePrefixes) {
                xmlBeanDefinitionScannerAgent.registerBasePackage(basePackage);
            }
            basePackageInited = true;
        }
    }

    void clearCache() {
        DefaultListableBeanFactory factory = maybeRegistryToBeanFactory();
        if (factory == null) {
            LOGGER.warning("Fail to find bean factory for url {}, cannot reload", this.url);
            return;
        }
        removeRegisteredBeanDefinitions(factory);
    }

    void reloadDefinition() {

        LOGGER.info("Reloading XML file '{}' of {} ", url, ObjectUtils.identityToString(this.reader.getRegistry()));


        this.reader.loadBeanDefinitions(new FileSystemResource(url.getPath()));

        reloadFlag = false;
    }

    private void removeRegisteredBeanDefinitions(DefaultListableBeanFactory factory) {
        LOGGER.debug("Remove all beans defined in the XML file {} before reloading it", url.getPath());
        for (String beanName : beansRegistered.keySet()) {
            try {
                BeanFactoryProcessor.removeBeanDefinition(factory, beanName);
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.debug("Bean {} not found in factory, ignore", beanName);
            }
        }

        beansRegistered.clear();
    }

    private DefaultListableBeanFactory maybeRegistryToBeanFactory() {
        BeanDefinitionRegistry registry = this.reader.getRegistry();
        if (registry instanceof DefaultListableBeanFactory) {
            return (DefaultListableBeanFactory) registry;
        } else if (registry instanceof GenericApplicationContext) {
            return ((GenericApplicationContext) registry).getDefaultListableBeanFactory();
        }
        return null;
    }
}
