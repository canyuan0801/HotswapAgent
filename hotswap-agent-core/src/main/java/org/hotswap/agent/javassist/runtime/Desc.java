

package org.hotswap.agent.javassist.runtime;


public class Desc {


    public static boolean useContextClassLoader = false;

    private static final ThreadLocal<Boolean> USE_CONTEXT_CLASS_LOADER_LOCALLY = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static void setUseContextClassLoaderLocally() {
        USE_CONTEXT_CLASS_LOADER_LOCALLY.set(true);
    }

    public static void resetUseContextClassLoaderLocally() {
        USE_CONTEXT_CLASS_LOADER_LOCALLY.remove();
    }

    private static Class<?> getClassObject(String name)
        throws ClassNotFoundException
    {
        if (useContextClassLoader || USE_CONTEXT_CLASS_LOADER_LOCALLY.get())
            return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        return Class.forName(name);
    }


    public static Class<?> getClazz(String name) {
        try {
            return getClassObject(name);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "$class: internal error, could not find class '" + name
                    + "' (Desc.useContextClassLoader: "
                    + Boolean.toString(useContextClassLoader) + ")", e);
        }
    }


    public static Class<?>[] getParams(String desc) {
        if (desc.charAt(0) != '(')
            throw new RuntimeException("$sig: internal error");

        return getType(desc, desc.length(), 1, 0);
    }


    public static Class<?> getType(String desc) {
        Class<?>[] result = getType(desc, desc.length(), 0, 0);
        if (result == null || result.length != 1)
            throw new RuntimeException("$type: internal error");

        return result[0];
    }

    private static Class<?>[] getType(String desc, int descLen,
                                   int start, int num) {
        Class<?> clazz;
        if (start >= descLen)
            return new Class[num];

        char c = desc.charAt(start);
        switch (c) {
        case 'Z' :
            clazz = Boolean.TYPE;
            break;
        case 'C' :
            clazz = Character.TYPE;
            break;
        case 'B' :
            clazz = Byte.TYPE;
            break;
        case 'S' :
            clazz = Short.TYPE;
            break;
        case 'I' :
            clazz = Integer.TYPE;
            break;
        case 'J' :
            clazz = Long.TYPE;
            break;
        case 'F' :
            clazz = Float.TYPE;
            break;
        case 'D' :
            clazz = Double.TYPE;
            break;
        case 'V' :
            clazz = Void.TYPE;
            break;
        case 'L' :
        case '[' :
            return getClassType(desc, descLen, start, num);
        default :
            return new Class[num];
        }

        Class<?>[] result = getType(desc, descLen, start + 1, num + 1);
        result[num] = clazz;
        return result;
    }

    private static Class<?>[] getClassType(String desc, int descLen,
                                        int start, int num) {
        int end = start;
        while (desc.charAt(end) == '[')
            ++end;

        if (desc.charAt(end) == 'L') {
            end = desc.indexOf(';', end);
            if (end < 0)
                throw new IndexOutOfBoundsException("bad descriptor");
        }

        String cname;
        if (desc.charAt(start) == 'L')
            cname = desc.substring(start + 1, end);
        else
            cname = desc.substring(start, end + 1);

        Class<?>[] result = getType(desc, descLen, end + 1, num + 1);
        try {
            result[num] = getClassObject(cname.replace('/', '.'));
        }
        catch (ClassNotFoundException e) {

            throw new RuntimeException(e.getMessage());
        }

        return result;
    }
}
