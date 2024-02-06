
package org.hotswap.agent.plugin.spring.scanner;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.MergeableCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.reload.SpringChangedReloadCommand;
import org.hotswap.agent.plugin.spring.reload.SpringReloadConfig;
import org.hotswap.agent.util.IOUtils;
import org.hotswap.agent.watch.WatchFileEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ClassPathBeanRefreshCommand extends MergeableCommand {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassPathBeanRefreshCommand.class);

    ClassLoader appClassLoader;

    String basePackage;

    String className;


    WatchFileEvent event;
    byte[] classDefinition;

    Scheduler scheduler;

    public ClassPathBeanRefreshCommand(ClassLoader appClassLoader, String basePackage, String className,
                                       byte[] classDefinition, Scheduler scheduler) {
        this.appClassLoader = appClassLoader;
        this.basePackage = basePackage;
        this.className = className;
        this.classDefinition = classDefinition;
        this.scheduler = scheduler;
    }

    public ClassPathBeanRefreshCommand(ClassLoader appClassLoader, String basePackage, String className,
                                       WatchFileEvent event, Scheduler scheduler) {
        this.appClassLoader = appClassLoader;
        this.basePackage = basePackage;
        this.event = event;
        this.className = className;
        this.scheduler = scheduler;
    }

    @Override
    public void executeCommand() {
        if (isDeleteEvent()) {
            LOGGER.trace("Skip Spring reload for delete event on class '{}'", className);
            return;
        }

        try {
            if (classDefinition == null) {
                try {
                    this.classDefinition = IOUtils.toByteArray(event.getURI());
                } catch (IllegalArgumentException e) {
                    LOGGER.debug("File {} not found on filesystem (deleted?). Unable to refresh associated Spring bean.", event.getURI());
                    return;
                }
            }

            LOGGER.debug("Executing ClassPathBeanDefinitionScannerAgent.refreshClass('{}')", className);

            Class<?> clazz = Class.forName("org.hotswap.agent.plugin.spring.scanner.ClassPathBeanDefinitionScannerAgent", true, appClassLoader);
            Method method  = clazz.getDeclaredMethod(
                    "refreshClassAndCheckReload", new Class[] {ClassLoader.class, String.class, String.class, byte[].class});
            method.invoke(null, appClassLoader , basePackage, basePackage, classDefinition);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Plugin error, method not found", e);
        } catch (InvocationTargetException e) {
            LOGGER.error("Error refreshing class {} in classLoader {}", e, className, appClassLoader);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Plugin error, illegal access", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Plugin error, Spring class not found in application classloader", e);
        }

    }


    private boolean isDeleteEvent() {

        List<ClassPathBeanRefreshCommand> mergedCommands = new ArrayList<>();
        for (Command command : getMergedCommands()) {
            mergedCommands.add((ClassPathBeanRefreshCommand) command);
        }
        mergedCommands.add(this);

        boolean createFound = false;
        boolean deleteFound = false;
        for (ClassPathBeanRefreshCommand command : mergedCommands) {
            if (command.event != null) {
                if (command.event.getEventType().equals(FileEvent.DELETE))
                    deleteFound = true;
                if (command.event.getEventType().equals(FileEvent.CREATE))
                    createFound = true;
            }
        }

        LOGGER.trace("isDeleteEvent result {}: createFound={}, deleteFound={}", createFound, deleteFound);
        return !createFound && deleteFound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassPathBeanRefreshCommand that = (ClassPathBeanRefreshCommand) o;

        if (!appClassLoader.equals(that.appClassLoader)) return false;
        if (!className.equals(that.className)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appClassLoader.hashCode();
        result = 31 * result + className.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClassPathBeanRefreshCommand{" +
                "appClassLoader=" + appClassLoader +
                ", basePackage='" + basePackage + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}
