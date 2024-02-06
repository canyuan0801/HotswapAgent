

package org.hotswap.agent.javassist.scopedpool;

import org.hotswap.agent.javassist.ClassPool;


public class ScopedClassPoolFactoryImpl implements ScopedClassPoolFactory {

    public ScopedClassPool create(ClassLoader cl, ClassPool src,
                                  ScopedClassPoolRepository repository) {
        return new ScopedClassPool(cl, src, repository, false);
    }


    public ScopedClassPool create(ClassPool src,
                                  ScopedClassPoolRepository repository) {
        return new ScopedClassPool(null, src, repository, true);
    }
}
