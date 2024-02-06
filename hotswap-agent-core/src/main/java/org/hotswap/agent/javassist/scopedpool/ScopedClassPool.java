

package org.hotswap.agent.javassist.scopedpool;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Map;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.LoaderClassPath;
import org.hotswap.agent.javassist.NotFoundException;


public class ScopedClassPool extends ClassPool {
    protected ScopedClassPoolRepository repository;

    protected Reference<ClassLoader> classLoader;

    protected LoaderClassPath classPath;

    protected Map<String,CtClass> softcache = new SoftValueHashMap<String,CtClass>();
    
    boolean isBootstrapCl = true;

    static {
        ClassPool.doPruning = false;
        ClassPool.releaseUnmodifiedClassFile = false;
    }


    protected ScopedClassPool(ClassLoader cl, ClassPool src,
            ScopedClassPoolRepository repository)
    {
       this(cl, src, repository, false);
    }
    

    protected ScopedClassPool(ClassLoader cl, ClassPool src,
            ScopedClassPoolRepository repository, boolean isTemp)
    {
       super(src);
       this.repository = repository;
       this.classLoader = new WeakReference<ClassLoader>(cl);
       if (cl != null) {
           classPath = new LoaderClassPath(cl);
           this.insertClassPath(classPath);
       }
       childFirstLookup = true;
       if (!isTemp && cl == null)
       {
          isBootstrapCl = true;
       }
    }


    public ClassLoader getClassLoader() {
       ClassLoader cl = getClassLoader0();
       if (cl == null && !isBootstrapCl)
       {
          throw new IllegalStateException(
                  "ClassLoader has been garbage collected");
       }
       return cl;
    }

    protected ClassLoader getClassLoader0() {
       return classLoader.get();
    }


    public void close() {
        this.removeClassPath(classPath);
        classes.clear();
        softcache.clear();
    }


    public synchronized void flushClass(String classname) {
        classes.remove(classname);
        softcache.remove(classname);
    }


    public synchronized void soften(CtClass clazz) {
        if (repository.isPrune())
            clazz.prune();
        classes.remove(clazz.getName());
        softcache.put(clazz.getName(), clazz);
    }


    public boolean isUnloadedClassLoader() {
        return false;
    }


    @Override
    protected CtClass getCached(String classname) {
        CtClass clazz = getCachedLocally(classname);
        if (clazz == null) {
            boolean isLocal = false;

            ClassLoader dcl = getClassLoader0();
            if (dcl != null) {
                final int lastIndex = classname.lastIndexOf('$');
                String classResourceName = null;
                if (lastIndex < 0) {
                    classResourceName = classname.replaceAll("[\\.]", "/")
                            + ".class";
                }
                else {
                    classResourceName = classname.substring(0, lastIndex)
                            .replaceAll("[\\.]", "/")
                            + classname.substring(lastIndex) + ".class";
                }

                isLocal = dcl.getResource(classResourceName) != null;
            }

            if (!isLocal) {
                Map<ClassLoader,ScopedClassPool> registeredCLs = repository.getRegisteredCLs();
                synchronized (registeredCLs) {
                    for (ScopedClassPool pool:registeredCLs.values()) {
                        if (pool.isUnloadedClassLoader()) {
                            repository.unregisterClassLoader(pool
                                    .getClassLoader());
                            continue;
                        }

                        clazz = pool.getCachedLocally(classname);
                        if (clazz != null) {
                            return clazz;
                        }
                    }
                }
            }
        }

        return clazz;
    }


    protected void cacheCtClass(String classname, CtClass c, boolean dynamic) {
        if (dynamic) {
            super.cacheCtClass(classname, c, dynamic);
        }
        else {
            if (repository.isPrune())
                c.prune();
            softcache.put(classname, c);
        }
    }


    public void lockInCache(CtClass c) {
        super.cacheCtClass(c.getName(), c, false);
    }


    protected CtClass getCachedLocally(String classname) {
        CtClass cached = (CtClass)classes.get(classname);
        if (cached != null)
            return cached;
        synchronized (softcache) {
            return (CtClass)softcache.get(classname);
        }
    }


    public synchronized CtClass getLocally(String classname)
            throws NotFoundException {
        softcache.remove(classname);
        CtClass clazz = (CtClass)classes.get(classname);
        if (clazz == null) {
            clazz = createCtClass(classname, true);
            if (clazz == null)
                throw new NotFoundException(classname);
            super.cacheCtClass(classname, clazz, false);
        }

        return clazz;
    }


    public Class<?> toClass(CtClass ct, ClassLoader loader, ProtectionDomain domain)
            throws CannotCompileException {












        lockInCache(ct);
        return super.toClass(ct, getClassLoader0(), domain);
    }
}
