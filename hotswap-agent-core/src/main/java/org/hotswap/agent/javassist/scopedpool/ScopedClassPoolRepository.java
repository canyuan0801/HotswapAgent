

package org.hotswap.agent.javassist.scopedpool;

import java.util.Map;

import org.hotswap.agent.javassist.ClassPool;


public interface ScopedClassPoolRepository {

    void setClassPoolFactory(ScopedClassPoolFactory factory);


    ScopedClassPoolFactory getClassPoolFactory();


    boolean isPrune();


    void setPrune(boolean prune);


    ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src);


    ClassPool findClassPool(ClassLoader cl);


    ClassPool registerClassLoader(ClassLoader ucl);


    Map<ClassLoader,ScopedClassPool> getRegisteredCLs();


    void clearUnregisteredClassLoaders();


    void unregisterClassLoader(ClassLoader cl);
}
