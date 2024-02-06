

package org.hotswap.agent.javassist;

import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;


public class LoaderClassPath implements ClassPath {
    private Reference<ClassLoader> clref;


    public LoaderClassPath(ClassLoader cl) {
        clref = new WeakReference<ClassLoader>(cl);
    }

    @Override
    public String toString() {
        return clref.get() == null ? "<null>" : clref.get().toString();
    }


    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        String cname = classname.replace('.', '/') + ".class";
        ClassLoader cl = clref.get();
        if (cl == null)
            return null;
        InputStream is = cl.getResourceAsStream(cname);
        return is;
    }


    @Override
    public URL find(String classname) {
        String cname = classname.replace('.', '/') + ".class";
        ClassLoader cl = clref.get();
        if (cl == null)
            return null;
        URL url = cl.getResource(cname);
        return url;
    }
}
