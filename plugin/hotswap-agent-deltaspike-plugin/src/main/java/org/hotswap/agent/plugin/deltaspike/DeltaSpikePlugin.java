
package org.hotswap.agent.plugin.deltaspike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.deltaspike.command.PartialBeanClassRefreshCommand;
import org.hotswap.agent.plugin.deltaspike.command.RepositoryRefreshCommand;
import org.hotswap.agent.plugin.deltaspike.jsf.ViewConfigReloadCommand;
import org.hotswap.agent.plugin.deltaspike.transformer.DeltaSpikeProxyContextualLifecycleTransformer;
import org.hotswap.agent.plugin.deltaspike.transformer.DeltaSpikeProxyTransformer;
import org.hotswap.agent.plugin.deltaspike.transformer.DeltaspikeContextsTransformer;
import org.hotswap.agent.plugin.deltaspike.transformer.PartialBeanTransformer;
import org.hotswap.agent.plugin.deltaspike.transformer.RepositoryTransformer;
import org.hotswap.agent.plugin.deltaspike.transformer.ViewConfigTransformer;
import org.hotswap.agent.util.AnnotationHelper;


@Plugin(name = "Deltaspike",
        description = "Apache DeltaSpike (http:
        testedVersions = {"1.5.2, 1.7.x-1.9.x"},
        expectedVersions = {"1.5.x-1.9.x"},
        supportClass = {
            DeltaSpikeProxyTransformer.class, PartialBeanTransformer.class, RepositoryTransformer.class, ViewConfigTransformer.class,
            DeltaspikeContextsTransformer.class, DeltaSpikeProxyContextualLifecycleTransformer.class
        }
)
public class DeltaSpikePlugin {

    private static AgentLogger LOGGER = AgentLogger.getLogger(DeltaSpikePlugin.class);

    private static final String REPOSITORY_ANNOTATION = "org.apache.deltaspike.data.api.Repository";
    public static final int WAIT_ON_REDEFINE = 500;

    @Init
    ClassLoader appClassLoader;

    @Init
    Scheduler scheduler;

    Map<Object, String> registeredPartialBeans = new WeakHashMap<>();
    Map<Object, List<String>> registeredViewConfExtRootClasses = new WeakHashMap<>();
    Set<Object> registeredWindowContexts = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());

    Map<Object, String> registeredRepoComponents = new WeakHashMap<>();

    Map<Object, String> registeredRepoProxies = new WeakHashMap<>();
    List<Class<?>> repositoryClasses;

    @Init
    public void init(PluginConfiguration pluginConfiguration) {
        LOGGER.info("Deltaspike plugin initialized.");
    }


    public void registerRepoComponent(Object repoComponent, Class<?> repositoryClass) {
        if (!registeredRepoComponents.containsKey(repoComponent)) {
            LOGGER.debug("DeltaspikePlugin - Repository Component registered : {}", repositoryClass.getName());
        }
        registeredRepoComponents.put(repoComponent, repositoryClass.getName());
    }

    public void registerRepositoryClasses(List<Class<?>> repositoryClassesList) {
        this.repositoryClasses = new ArrayList<>(repositoryClassesList);
    }


    public void registerRepoProxy(Object repoProxy, Class<?> repositoryClass) {
        if (repositoryClasses == null) {
            return;
        }
        if (!registeredRepoProxies.containsKey(repoProxy)) {
            LOGGER.debug("DeltaspikePlugin - repository proxy registered : {}", repositoryClass.getName());
        }
        Class<?> checkedClass = repositoryClass;
        while(checkedClass != null && !repositoryClasses.contains(checkedClass)) {
            checkedClass = checkedClass.getSuperclass();
        }
        if (checkedClass != null) {
            registeredRepoProxies.put(repoProxy, repositoryClass.getName());
        }
    }

    public void registerPartialBean(Object bean, Class<?> partialBeanClass) {
        synchronized(registeredPartialBeans) {
            registeredPartialBeans.put(bean, partialBeanClass.getName());
        }
        LOGGER.debug("Partial bean '{}' registered", partialBeanClass.getName());
    }

    public void registerWindowContext(Object windowContext) {
        if (windowContext != null && !registeredWindowContexts.contains(windowContext)) {
            registeredWindowContexts.add(windowContext);
            LOGGER.debug("Window context '{}' registered.", windowContext.getClass().getName());
        }
    }

    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void classReload(CtClass clazz, Class original, ClassPool classPool) throws NotFoundException {
        checkRefreshViewConfigExtension(clazz, original);
        PartialBeanClassRefreshCommand cmd = checkRefreshPartialBean(clazz, original, classPool);
        if (cmd != null) {
            checkRefreshRepository(clazz, classPool, cmd);
        }
    }

    private PartialBeanClassRefreshCommand checkRefreshPartialBean(CtClass clazz, Class original, ClassPool classPool) throws NotFoundException {
        PartialBeanClassRefreshCommand cmd = null;
        Object partialBean = getObjectByName(registeredPartialBeans, clazz.getName());
        if (partialBean != null) {
            String oldSignForProxyCheck = DeltaspikeClassSignatureHelper.getSignaturePartialBeanClass(original);
            cmd = new PartialBeanClassRefreshCommand(appClassLoader, partialBean, clazz.getName(), oldSignForProxyCheck, scheduler);
            scheduler.scheduleCommand(cmd, WAIT_ON_REDEFINE);
        }
        return cmd;
    }

    private void checkRefreshRepository(CtClass clazz, ClassPool classPool, PartialBeanClassRefreshCommand masterCmd) throws NotFoundException {
        if (isRepository(clazz, classPool)) {
            Object repositoryComponent = getObjectByName(registeredRepoComponents, clazz.getName());
            RepositoryRefreshCommand cmd = null;
            if (repositoryComponent != null) {

                cmd = new RepositoryRefreshCommand(appClassLoader, clazz.getName(), repositoryComponent);
            } else if (repositoryClasses!= null) {
                cmd = new RepositoryRefreshCommand(appClassLoader, clazz.getName(), getRepositoryProxies(clazz.getName()));
            }
            if (cmd != null) {
                masterCmd.addChainedCommand(cmd);
            }
        }
    }

    private List<Object> getRepositoryProxies(String repositoryClassName) {
        List<Object> result = new ArrayList<>();
        for (Entry<Object, String> entry: registeredRepoProxies.entrySet()) {
            if (repositoryClassName.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private boolean isRepository(CtClass clazz, ClassPool classPool) throws NotFoundException {
        if (isSyntheticCdiClass(clazz.getName())) {
            return false;
        }
        CtClass ctInvocationHandler = classPool.get("java.lang.reflect.InvocationHandler");
        if (clazz.subtypeOf(ctInvocationHandler)) {
            return false;
        }
        if (AnnotationHelper.hasAnnotation(clazz, REPOSITORY_ANNOTATION)) {
            return true;
        }
        CtClass superClass = clazz.getSuperclass();
        if (superClass != null) {
            return isRepository(superClass, classPool);
        }
        return false;
    }

    private Object getObjectByName(Map<Object, String> registeredComponents, String className) {
        for (Entry<Object, String> entry : registeredComponents.entrySet()) {
           if (className.equals(entry.getValue())) {
               return entry.getKey();
           }
        }
        return null;
    }

    private void checkRefreshViewConfigExtension(CtClass clazz, Class original) {
        String className = original.getName();
        int index = className.indexOf("$");
        String rootClassName = (index!=-1) ? className.substring(0, index) : className;
        for (Entry<Object, List<String>> entry: registeredViewConfExtRootClasses.entrySet()) {
            List<String> rootClassNameList = entry.getValue();
            for (String viewConfigClassName: rootClassNameList) {
                if (viewConfigClassName.equals(rootClassName)) {
                    scheduler.scheduleCommand(new ViewConfigReloadCommand(appClassLoader, entry.getKey(), entry.getValue()), WAIT_ON_REDEFINE);
                    return;
                }
            }
        }
    }

    public void registerViewConfigRootClasses(Object viewConfigExtension, List rootClassList) {
        if (rootClassList != null ) {
            List<String> rootClassNameList = new ArrayList<>();
            for (Object viewConfigClassObj : rootClassList) {
                Class<?> viewConfigClass = (Class<?>) viewConfigClassObj;
                LOGGER.debug("ViewConfigRoot class '{}' registered.", viewConfigClass.getName());
                rootClassNameList.add(viewConfigClass.getName());
            }
            registeredViewConfExtRootClasses.put(viewConfigExtension, rootClassNameList);
        }
    }

    private boolean isSyntheticCdiClass(String className) {
        return className.contains("$$");
    }

}
