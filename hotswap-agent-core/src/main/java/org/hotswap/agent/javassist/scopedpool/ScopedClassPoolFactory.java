

package org.hotswap.agent.javassist.scopedpool;

import org.hotswap.agent.javassist.ClassPool;


public interface ScopedClassPoolFactory {

    ScopedClassPool create(ClassLoader cl, ClassPool src,
                           ScopedClassPoolRepository repository);


    ScopedClassPool create(ClassPool src,
                           ScopedClassPoolRepository repository);
}
