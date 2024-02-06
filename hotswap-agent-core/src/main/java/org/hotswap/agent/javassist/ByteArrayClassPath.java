

package org.hotswap.agent.javassist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;


public class ByteArrayClassPath implements ClassPath {
    protected String classname;
    protected byte[] classfile;


    public ByteArrayClassPath(String name, byte[] classfile) {
        this.classname = name;
        this.classfile = classfile;
    }

    @Override
    public String toString() {
        return "byte[]:" + classname;
    }


    @Override
    public InputStream openClassfile(String classname) {
        if(this.classname.equals(classname))
            return new ByteArrayInputStream(classfile);
        return null;
    }


    @Override
    public URL find(String classname) {
        if(this.classname.equals(classname)) {
            String cname = classname.replace('.', '/') + ".class";
            try {
                return new URL(null, "file:/ByteArrayClassPath/" + cname, new BytecodeURLStreamHandler());
            }
            catch (MalformedURLException e) {}
        }

        return null;
    }

    private class BytecodeURLStreamHandler extends URLStreamHandler {
        protected URLConnection openConnection(final URL u) {
            return new BytecodeURLConnection(u);
        }
    }

    private class BytecodeURLConnection extends URLConnection {
        protected BytecodeURLConnection(URL url) {
            super(url);
        }

        public void connect() throws IOException {
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(classfile);
        }

        public int getContentLength() {
            return classfile.length;
        }
    }
}
