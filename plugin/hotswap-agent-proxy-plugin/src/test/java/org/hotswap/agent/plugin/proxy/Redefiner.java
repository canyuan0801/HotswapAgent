
package org.hotswap.agent.plugin.proxy;

import java.io.Closeable;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;


public interface Redefiner extends Closeable {
    void redefineClasses(Map<Class<?>, byte[]> classes)
            throws ClassNotFoundException, UnmodifiableClassException;
}
