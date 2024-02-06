
package org.hotswap.agent.plugin.spring.scanner;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.reload.SpringChangedReloadCommand;
import org.hotswap.agent.plugin.spring.reload.SpringReloadConfig;
import org.hotswap.agent.util.IOUtils;
import org.hotswap.agent.util.classloader.ClassLoaderHelper;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.WatchFileEvent;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SpringBeanWatchEventListener implements WatchEventListener {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(SpringBeanWatchEventListener.class);


    private static final int WAIT_ON_CREATE = 600;

    private Scheduler scheduler;
    private ClassLoader appClassLoader;
    private String basePackage;

    public SpringBeanWatchEventListener(Scheduler scheduler, ClassLoader appClassLoader, String basePackage) {
        this.scheduler = scheduler;
        this.appClassLoader = appClassLoader;
        this.basePackage = basePackage;
    }

    @Override
    public void onEvent(WatchFileEvent event) {

        if (event.getEventType() == FileEvent.CREATE && event.isFile() && event.getURI().toString().endsWith(".class")) {

            String className;
            try {
                className = IOUtils.urlToClassName(event.getURI());
            } catch (IOException e) {
                LOGGER.trace("Watch event on resource '{}' skipped, probably Ok because of delete/create event " +
                        "sequence (compilation not finished yet).", e, event.getURI());
                return;
            }
            if (!ClassLoaderHelper.isClassLoaded(appClassLoader, className)) {

                scheduler.scheduleCommand(new ClassPathBeanRefreshCommand(appClassLoader,
                        basePackage, className, event, scheduler), WAIT_ON_CREATE);
                LOGGER.trace("Scheduling Spring reload for class '{}' in classLoader {}", className, appClassLoader);
                scheduler.scheduleCommand(new SpringChangedReloadCommand(appClassLoader), SpringReloadConfig.reloadDelayMillis);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpringBeanWatchEventListener that = (SpringBeanWatchEventListener) o;
        return Objects.equals(appClassLoader, that.appClassLoader) && Objects.equals(basePackage, that.basePackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appClassLoader, basePackage);
    }
}
