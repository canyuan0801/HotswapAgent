
package org.hotswap.agent.plugin.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;


public class HotSwapTool {


    public static final String IDENTIFIER = "___";
    private static final String CLASS_FILE_SUFFIX = ".class";
    private static Map<Class<?>, Integer> currentVersion = new Hashtable<Class<?>, Integer>();
    private static Redefiner redefiner;
    private static int redefinitionCount;
    private static long totalTime;

    static {
        try {

            redefiner = new InstrumentationRedefiner();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public static int getCurrentVersion(Class<?> baseClass) {
        if (!currentVersion.containsKey(baseClass)) {
            currentVersion.put(baseClass, 0);
        }
        return currentVersion.get(baseClass);
    }


    public static void shutdown() throws IOException {
        redefiner.close();
        redefiner = null;
    }

    private static Map<Class<?>, byte[]> buildRedefinitionMap(
            Map<String, File> classes)
            throws IOException, ClassNotFoundException {



        final Map<String, String> typeMappings = new HashMap<String, String>();
        for (String name : classes.keySet()) {
            Class<?> clazz = Class.forName(name);
            ClassRedefinitionPolicy policy = clazz
                    .getAnnotation(ClassRedefinitionPolicy.class);
            Class<?> replacement = (policy != null
                    && policy.alias() != ClassRedefinitionPolicy.NoClass.class)
                            ? policy.alias()
                            : clazz;
            typeMappings.put(Type.getInternalName(clazz),
                    stripVersion(Type.getInternalName(replacement)));

        }

        Map<Class<?>, byte[]> classesMap = new HashMap<Class<?>, byte[]>();
        for (File file : classes.values()) {
            loadAdaptedClass(file, typeMappings, classesMap);
        }
        return classesMap;
    }

    private static void loadAdaptedClass(File file,
            Map<String, String> typeMappnigs, Map<Class<?>, byte[]> result)
            throws IOException, ClassNotFoundException {

        ClassWriter writer = new ClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        TestClassAdapter adapter = new TestClassAdapter(writer, typeMappnigs);

        InputStream in = new FileInputStream(file);
        try {
            new ClassReader(in).accept(adapter, ClassReader.EXPAND_FRAMES);
        } finally {
            try {
                in.close();
            } catch (IOException e) {

            }
        }
        byte[] bytes = writer.toByteArray();
        String className = adapter.getClassName().replace('/', '.');
        result.put(Class.forName(className), bytes);
    }


    public static void toVersion(Class<?> outerClass, int versionNumber,
            Class<?>... extraClasses) {
        assert versionNumber >= 0;






        Map<String, File> files = findClassesWithVersion(outerClass,
                versionNumber);

        for (Class<?> extra : extraClasses) {
            if (parseClassVersion(extra.getSimpleName()) == versionNumber) {
                String packageName = extra.getPackage().getName().replace('.',
                        '/');
                URL url = extra.getClassLoader().getResource(packageName);
                if (url == null) {
                    throw new IllegalArgumentException(
                            "Cannot find URL corresponding to the package '"
                                    + packageName + "'");
                }
                File file = new File(url.getFile(),
                        extra.getSimpleName() + ".class");
                files.put(extra.getName(), file);
            }
        }

        try {
            Map<Class<?>, byte[]> map = buildRedefinitionMap(files);

            long startTime = System.currentTimeMillis();
            redefiner.redefineClasses(map);
            long curTime = System.currentTimeMillis() - startTime;
            totalTime += curTime;
            redefinitionCount++;

        } catch (UnmodifiableClassException e) {
            throw new UnsupportedOperationException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot redefine classes", e);
        } catch (IOException e) {
            throw new RuntimeException("Cannot redefine classes", e);
        }

        setCurrentVersion(outerClass, versionNumber);
    }

    private static Map<String, File> findClassesWithVersion(Class<?> baseClass,
            int version) {
        Map<String, File> classes = new HashMap<String, File>();

        String packageName = baseClass.getPackage().getName().replace('.', '/');
        URL url = baseClass.getClassLoader().getResource(packageName);
        if (url == null) {
            throw new IllegalArgumentException(
                    "Cannot find URL corresponding to the package '"
                            + packageName + "'");
        }
        File folder = new File(url.getFile());
        for (File f : folder.listFiles(IsClassFile.INSTANCE)) {
            String simpleName = f.getName().substring(0,
                    f.getName().length() - CLASS_FILE_SUFFIX.length());
            String name = baseClass.getPackage().getName() + '.' + simpleName;

            if (isInnerClass(name, baseClass)
                    && parseClassVersion(simpleName) == version) {
                classes.put(name, f);
            }
        }
        return classes;
    }

    private enum IsClassFile implements FilenameFilter {
        INSTANCE;

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(CLASS_FILE_SUFFIX);
        }
    }

    private static boolean isInnerClass(String name, Class<?> baseClass) {
        return name.startsWith(baseClass.getName() + "$");
    }

    private static void setCurrentVersion(Class<?> baseClass, int value) {
        currentVersion.put(baseClass, value);
    }


    private static int parseClassVersion(String simpleName) {
        int index = simpleName.indexOf(IDENTIFIER);
        if (index == -1) {
            return -1;
        }
        return Integer.valueOf(simpleName.substring(index + IDENTIFIER.length(),
                simpleName.length()));
    }

    private static String stripVersion(String className) {
        int index = className.indexOf(IDENTIFIER);
        if (index == -1) {
            return className;
        }
        return className.substring(0, index);
    }

    public static void resetTimings() {
        redefinitionCount = 0;
        totalTime = 0;
    }

    public static int getRedefinitionCount() {
        return redefinitionCount;
    }

    public static long getTotalTime() {
        return totalTime;
    }
}
