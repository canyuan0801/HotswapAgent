
package org.hotswap.agent.plugin.spring;

import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.plugin.spring.scanner.ClassPathBeanRefreshCommand;
import org.hotswap.agent.util.HaClassFileTransformer;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Objects;

public class SpringBeanClassFileTransformer implements HaClassFileTransformer {
    private ClassLoader appClassLoader;
    private Scheduler scheduler;
    private String basePackage;

    public SpringBeanClassFileTransformer(ClassLoader appClassLoader, Scheduler scheduler, String basePackage) {
        this.appClassLoader = appClassLoader;
        this.scheduler = scheduler;
        this.basePackage = basePackage;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        final SpringChangesAnalyzer analyzer = new SpringChangesAnalyzer(appClassLoader);
        if (classBeingRedefined != null) {
            if (analyzer.isReloadNeeded(classBeingRedefined, classfileBuffer)) {
                scheduler.scheduleCommand(new ClassPathBeanRefreshCommand(classBeingRedefined.getClassLoader(),
                        basePackage, className, classfileBuffer, scheduler));
            }
        }
        return classfileBuffer;
    }

    @Override
    public boolean isForRedefinitionOnly() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpringBeanClassFileTransformer that = (SpringBeanClassFileTransformer) o;
        return Objects.equals(appClassLoader, that.appClassLoader) && Objects.equals(basePackage, that.basePackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appClassLoader, basePackage);
    }
}
