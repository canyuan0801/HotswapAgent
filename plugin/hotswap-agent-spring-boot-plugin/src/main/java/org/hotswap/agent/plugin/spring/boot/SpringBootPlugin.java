
package org.hotswap.agent.plugin.spring.boot;

import java.util.concurrent.atomic.AtomicBoolean;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.boot.transformers.PropertySourceLoaderTransformer;
import org.hotswap.agent.plugin.spring.boot.transformers.PropertySourceTransformer;
import org.hotswap.agent.util.PluginManagerInvoker;


@Plugin(name = "SpringBoot", description = "Reload Spring Boot after properties/yaml changed.",
        testedVersions = {"All between 1.5.x - 2.7.x"}, expectedVersions = {"1.5.x+", "2.x"},
        supportClass = {PropertySourceLoaderTransformer.class,
                PropertySourceTransformer.class})
public class SpringBootPlugin {

    private static final AgentLogger LOGGER = AgentLogger.getLogger(SpringBootPlugin.class);

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    private final AtomicBoolean isInit = new AtomicBoolean(false);

    public void init() {
        if (isInit.compareAndSet(false, true)) {
            LOGGER.info("Spring Boot plugin initialized");
        }
    }

    public void init(String version) throws ClassNotFoundException {
        if (isInit.compareAndSet(false, true)) {
            LOGGER.info("Spring Boot plugin initialized - Spring Boot core version '{}'", version);
        }
    }

    @OnClassLoadEvent(classNameRegexp = "org.springframework.boot.SpringApplication")
    public static void register(ClassLoader appClassLoader, CtClass clazz, ClassPool classPool) throws
        CannotCompileException, NotFoundException {
        StringBuilder src = new StringBuilder("{");

        src.append(PluginManagerInvoker.buildInitializePlugin(SpringBootPlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(SpringBootPlugin.class, "init",
                "org.springframework.boot.SpringBootVersion.getVersion()", String.class.getName()));
        src.append("}");

        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            constructor.insertBeforeBody(src.toString());
        }

        CtMethod method = clazz.getDeclaredMethod("createApplicationContext");
        method.insertAfter(
            "{org.hotswap.agent.plugin.spring.boot.listener.PropertySourceChangeListener.register($_);}");
    }
}
