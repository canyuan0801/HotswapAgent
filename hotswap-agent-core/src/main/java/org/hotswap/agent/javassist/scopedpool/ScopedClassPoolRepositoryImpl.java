

package org.hotswap.agent.javassist.scopedpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.LoaderClassPath;


public class ScopedClassPoolRepositoryImpl implements ScopedClassPoolRepository {

    private static final ScopedClassPoolRepositoryImpl instance = new ScopedClassPoolRepositoryImpl();


    private boolean prune = true;


    boolean pruneWhenCached;


    protected Map<ClassLoader,ScopedClassPool> registeredCLs = Collections
            .synchronizedMap(new WeakHashMap<ClassLoader,ScopedClassPool>());


    protected ClassPool classpool;


    protected ScopedClassPoolFactory factory = new ScopedClassPoolFactoryImpl();


    public static ScopedClassPoolRepository getInstance() {
        return instance;
    }


    private ScopedClassPoolRepositoryImpl() {
        classpool = ClassPool.getDefault();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        classpool.insertClassPath(new LoaderClassPath(cl));
    }


    @Override
    public boolean isPrune() {
        return prune;
    }


    @Override
    public void setPrune(boolean prune) {
        this.prune = prune;
    }


    @Override
    public ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src) {
        return factory.create(cl, src, this);
    }

    @Override
    public ClassPool findClassPool(ClassLoader cl) {
        if (cl == null)
            return registerClassLoader(ClassLoader.getSystemClassLoader());

        return registerClassLoader(cl);
    }


    @Override
    public ClassPool registerClassLoader(ClassLoader ucl) {
        synchronized (registeredCLs) {





            if (registeredCLs.containsKey(ucl)) {
                return registeredCLs.get(ucl);
            }
            ScopedClassPool pool = createScopedClassPool(ucl, classpool);
            registeredCLs.put(ucl, pool);
            return pool;
        }
    }


    @Override
    public Map<ClassLoader,ScopedClassPool> getRegisteredCLs() {
        clearUnregisteredClassLoaders();
        return registeredCLs;
    }


    @Override
    public void clearUnregisteredClassLoaders() {
        List<ClassLoader> toUnregister = null;
        synchronized (registeredCLs) {
            for (Map.Entry<ClassLoader,ScopedClassPool> reg:registeredCLs.entrySet()) {
                if (reg.getValue().isUnloadedClassLoader()) {
                    ClassLoader cl = reg.getValue().getClassLoader();
                    if (cl != null) {
                        if (toUnregister == null)
                            toUnregister = new ArrayList<ClassLoader>();
                        toUnregister.add(cl);
                    }
                    registeredCLs.remove(reg.getKey());
                }
            }
            if (toUnregister != null)
                for (ClassLoader cl:toUnregister)
                    unregisterClassLoader(cl);
        }
    }

    @Override
    public void unregisterClassLoader(ClassLoader cl) {
        synchronized (registeredCLs) {
            ScopedClassPool pool = registeredCLs.remove(cl);
            if (pool != null)
                pool.close();
        }
    }

    public void insertDelegate(ScopedClassPoolRepository delegate) {

    }

    @Override
    public void setClassPoolFactory(ScopedClassPoolFactory factory) {
        this.factory = factory;
    }

    @Override
    public ScopedClassPoolFactory getClassPoolFactory() {
        return factory;
    }
}
