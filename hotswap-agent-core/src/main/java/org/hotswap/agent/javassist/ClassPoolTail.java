

package org.hotswap.agent.javassist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class ClassPathList {
    ClassPathList next;
    ClassPath path;

    ClassPathList(ClassPath p, ClassPathList n) {
        next = n;
        path = p;
    }
}

final class DirClassPath implements ClassPath {
    String directory;

    DirClassPath(String dirName) {
        directory = dirName;
    }

    @Override
    public InputStream openClassfile(String classname) {
        try {
            char sep = File.separatorChar;
            String filename = directory + sep
                + classname.replace('.', sep) + ".class";
            return new FileInputStream(filename.toString());
        }
        catch (FileNotFoundException e) {}
        catch (SecurityException e) {}
        return null;
    }

    @Override
    public URL find(String classname) {
        char sep = File.separatorChar;
        String filename = directory + sep
            + classname.replace('.', sep) + ".class";
        File f = new File(filename);
        if (f.exists())
            try {
                return f.getCanonicalFile().toURI().toURL();
            }
            catch (MalformedURLException e) {}
            catch (IOException e) {}

        return null;
    }

    @Override
    public String toString() {
        return directory;
    }
}

final class JarDirClassPath implements ClassPath {
    JarClassPath[] jars;

    JarDirClassPath(String dirName) throws NotFoundException {
        File[] files = new File(dirName).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jar") || name.endsWith(".zip");
            }
        });

        if (files != null) {
            jars = new JarClassPath[files.length];
            for (int i = 0; i < files.length; i++)
                jars[i] = new JarClassPath(files[i].getPath());
        }
    }

    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        if (jars != null)
            for (int i = 0; i < jars.length; i++) {
                InputStream is = jars[i].openClassfile(classname);
                if (is != null)
                    return is;
            }

        return null;
    }

    @Override
    public URL find(String classname) {
        if (jars != null)
            for (int i = 0; i < jars.length; i++) {
                URL url = jars[i].find(classname);
                if (url != null)
                    return url;
            }

        return null;
    }
}

final class JarClassPath implements ClassPath {
    List<String> jarfileEntries;
    String jarfileURL;

    JarClassPath(String pathname) throws NotFoundException {
        JarFile jarfile = null;
        try {
            jarfile = new JarFile(pathname);
            jarfileEntries = new ArrayList<String>();
            for (JarEntry je:Collections.list(jarfile.entries()))
                if (je.getName().endsWith(".class"))
                    jarfileEntries.add(je.getName());
            jarfileURL = new File(pathname).getCanonicalFile()
                    .toURI().toURL().toString();
            return;
        } catch (IOException e) {}
        finally {
            if (null != jarfile)
                try {
                    jarfile.close();
                } catch (IOException e) {}
        }
        throw new NotFoundException(pathname);
    }

    @Override
    public InputStream openClassfile(String classname)
            throws NotFoundException
    {
        URL jarURL = find(classname);
        if (null != jarURL)
            try {
                java.net.URLConnection con = jarURL.openConnection();
                con.setUseCaches(false);
                return con.getInputStream();
            }
            catch (IOException e) {
                throw new NotFoundException("broken jar file?: "
                        + classname);
            }
        return null;
    }

    @Override
    public URL find(String classname) {
        String jarname = classname.replace('.', '/') + ".class";
        if (jarfileEntries.contains(jarname))
            try {
                return new URL(String.format("jar:%s!/%s", jarfileURL, jarname));
            }
            catch (MalformedURLException e) {}
        return null;
    }

    @Override
    public String toString() {
        return jarfileURL == null ? "<null>" : jarfileURL.toString();
    }
}

final class ClassPoolTail {
    protected ClassPathList pathList;

    public ClassPoolTail() {
        pathList = null;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[class path: ");
        ClassPathList list = pathList;
        while (list != null) {
            buf.append(list.path.toString());
            buf.append(File.pathSeparatorChar);
            list = list.next;
        }

        buf.append(']');
        return buf.toString();
    }

    public synchronized ClassPath insertClassPath(ClassPath cp) {
        pathList = new ClassPathList(cp, pathList);
        return cp;
    }

    public synchronized ClassPath appendClassPath(ClassPath cp) {
        ClassPathList tail = new ClassPathList(cp, null);
        ClassPathList list = pathList;
        if (list == null)
            pathList = tail;
        else {
            while (list.next != null)
                list = list.next;

            list.next = tail;
        }

        return cp;
    }

    public synchronized void removeClassPath(ClassPath cp) {
        ClassPathList list = pathList;
        if (list != null)
            if (list.path == cp)
                pathList = list.next;
            else {
                while (list.next != null)
                    if (list.next.path == cp)
                        list.next = list.next.next;
                    else
                        list = list.next;
            }
    }

    public ClassPath appendSystemPath() {
        if (org.hotswap.agent.javassist.bytecode.ClassFile.MAJOR_VERSION < org.hotswap.agent.javassist.bytecode.ClassFile.JAVA_9)
            return appendClassPath(new ClassClassPath());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return appendClassPath(new LoaderClassPath(cl));
    }

    public ClassPath insertClassPath(String pathname)
        throws NotFoundException
    {
        return insertClassPath(makePathObject(pathname));
    }

    public ClassPath appendClassPath(String pathname)
        throws NotFoundException
    {
        return appendClassPath(makePathObject(pathname));
    }

    private static ClassPath makePathObject(String pathname)
        throws NotFoundException
    {
        String lower = pathname.toLowerCase();
        if (lower.endsWith(".jar") || lower.endsWith(".zip"))
            return new JarClassPath(pathname);

        int len = pathname.length();
        if (len > 2 && pathname.charAt(len - 1) == '*'
            && (pathname.charAt(len - 2) == '/'
                || pathname.charAt(len - 2) == File.separatorChar)) {
            String dir = pathname.substring(0, len - 2);
            return new JarDirClassPath(dir);
        }

        return new DirClassPath(pathname);
    }


    void writeClassfile(String classname, OutputStream out)
        throws NotFoundException, IOException, CannotCompileException
    {
        InputStream fin = openClassfile(classname);
        if (fin == null)
            throw new NotFoundException(classname);

        try {
            copyStream(fin, out);
        }
        finally {
            fin.close();
        }
    }





    InputStream openClassfile(String classname)
        throws NotFoundException
    {
        ClassPathList list = pathList;
        InputStream ins = null;
        NotFoundException error = null;
        while (list != null) {
            try {
                ins = list.path.openClassfile(classname);
            }
            catch (NotFoundException e) {
                if (error == null)
                    error = e;
            }

            if (ins == null)
                list = list.next;
            else
                return ins;
        }

        if (error != null)
            throw error;
        return null;
    }


    public URL find(String classname) {
        ClassPathList list = pathList;
        URL url = null;
        while (list != null) {
            url = list.path.find(classname);
            if (url == null)
                list = list.next;
            else
                return url;
        }

        return null;
    }


    public static byte[] readStream(InputStream fin) throws IOException {
        byte[][] bufs = new byte[8][];
        int bufsize = 4096;

        for (int i = 0; i < 8; ++i) {
            bufs[i] = new byte[bufsize];
            int size = 0;
            int len = 0;
            do {
                len = fin.read(bufs[i], size, bufsize - size);
                if (len >= 0)
                    size += len;
                else {
                    byte[] result = new byte[bufsize - 4096 + size];
                    int s = 0;
                    for (int j = 0; j < i; ++j) {
                        System.arraycopy(bufs[j], 0, result, s, s + 4096);
                        s = s + s + 4096;
                    }

                    System.arraycopy(bufs[i], 0, result, s, size);
                    return result;
                }
            } while (size < bufsize);
            bufsize *= 2;
        }

        throw new IOException("too much data");
    }


    public static void copyStream(InputStream fin, OutputStream fout)
        throws IOException
    {
        int bufsize = 4096;
        byte[] buf = null;
        for (int i = 0; i < 64; ++i) {
            if (i < 8) {
                bufsize *= 2;
                buf = new byte[bufsize];
            }
            int size = 0;
            int len = 0;
            do {
                len = fin.read(buf, size, bufsize - size);
                if (len >= 0)
                    size += len;
                else {
                    fout.write(buf, 0, size);
                    return;
                }
            } while (size < bufsize);
            fout.write(buf);
        }

        throw new IOException("too much data");
    }
}
