

package org.hotswap.agent.javassist;

import java.io.InputStream;
import java.net.URL;


public class ClassClassPath implements ClassPath {
    private Class<?> thisClass;


    public ClassClassPath(Class<?> c) {
        thisClass = c;
    }

    ClassClassPath() {

        this(java.lang.Object.class);
    }


    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        String filename = '/' + classname.replace('.', '/') + ".class";
        return thisClass.getResourceAsStream(filename);
    }


    @Override
    public URL find(String classname) {
        String filename = '/' + classname.replace('.', '/') + ".class";
        return thisClass.getResource(filename);
    }

    @Override
    public String toString() {
        return thisClass.getName() + ".class";
    }
}
