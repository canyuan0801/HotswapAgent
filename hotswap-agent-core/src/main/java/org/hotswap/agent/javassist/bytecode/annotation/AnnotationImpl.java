

package org.hotswap.agent.javassist.bytecode.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.AnnotationDefaultAttribute;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.MethodInfo;


public class AnnotationImpl implements InvocationHandler {
    private static final String JDK_ANNOTATION_CLASS_NAME = "java.lang.annotation.Annotation";
    private static Method JDK_ANNOTATION_TYPE_METHOD = null;

    private Annotation annotation;
    private ClassPool pool;
    private ClassLoader classLoader;
    private transient Class<?> annotationType;
    private transient int cachedHashCode = Integer.MIN_VALUE;

    static {

        try {
            Class<?> clazz = Class.forName(JDK_ANNOTATION_CLASS_NAME);
            JDK_ANNOTATION_TYPE_METHOD = clazz.getMethod("annotationType", (Class[])null);
        }
        catch (Exception ignored) {

        }
    }


    public static Object make(ClassLoader cl, Class<?> clazz, ClassPool cp,
                              Annotation anon)
        throws IllegalArgumentException
    {
        AnnotationImpl handler = new AnnotationImpl(anon, cp, cl);
        return Proxy.newProxyInstance(cl, new Class[] { clazz }, handler);
    }

    private AnnotationImpl(Annotation a, ClassPool cp, ClassLoader loader) {
        annotation = a;
        pool = cp;
        classLoader = loader;
    }


    public String getTypeName() {
        return annotation.getTypeName();
    }


    private Class<?> getAnnotationType() {
        if (annotationType == null) {
            String typeName = annotation.getTypeName();
            try {
                annotationType = classLoader.loadClass(typeName);
            }
            catch (ClassNotFoundException e) {
                NoClassDefFoundError error = new NoClassDefFoundError("Error loading annotation class: " + typeName);
                error.setStackTrace(e.getStackTrace());
                throw error;
            }
        }
        return annotationType;
    }


    public Annotation getAnnotation() {
        return annotation;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        String name = method.getName();
        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(name)) {
                Object obj = args[0];
                return Boolean.valueOf(checkEquals(obj));
            }
            else if ("toString".equals(name))
                return annotation.toString();
            else if ("hashCode".equals(name))
                return Integer.valueOf(hashCode());
        }
        else if ("annotationType".equals(name)
                 && method.getParameterTypes().length == 0)
           return getAnnotationType();

        MemberValue mv = annotation.getMemberValue(name);
        if (mv == null)
            return getDefault(name, method);
        return mv.getValue(classLoader, pool, method);
    }

    private Object getDefault(String name, Method method)
        throws ClassNotFoundException, RuntimeException
    {
        String classname = annotation.getTypeName();
        if (pool != null) {
            try {
                CtClass cc = pool.get(classname);
                ClassFile cf = cc.getClassFile2();
                MethodInfo minfo = cf.getMethod(name);
                if (minfo != null) {
                    AnnotationDefaultAttribute ainfo
                        = (AnnotationDefaultAttribute)
                          minfo.getAttribute(AnnotationDefaultAttribute.tag);
                    if (ainfo != null) {
                        MemberValue mv = ainfo.getDefaultValue();
                        return mv.getValue(classLoader, pool, method);
                    }
                }
            }
            catch (NotFoundException e) {
                throw new RuntimeException("cannot find a class file: "
                                           + classname);
            }
        }

        throw new RuntimeException("no default value: " + classname + "."
                                   + name + "()");
    }


    @Override
    public int hashCode() {
        if (cachedHashCode == Integer.MIN_VALUE) {
            int hashCode = 0;


            getAnnotationType();

            Method[] methods = annotationType.getDeclaredMethods();
            for (int i = 0; i < methods.length; ++ i) {
                String name = methods[i].getName();
                int valueHashCode = 0;


                MemberValue mv = annotation.getMemberValue(name);
                Object value = null;
                try {
                   if (mv != null)
                       value = mv.getValue(classLoader, pool, methods[i]);
                   if (value == null)
                       value = getDefault(name, methods[i]);
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new RuntimeException("Error retrieving value " + name + " for annotation " + annotation.getTypeName(), e);
                }


                if (value != null) {
                    if (value.getClass().isArray())
                        valueHashCode = arrayHashCode(value);
                    else
                        valueHashCode = value.hashCode();
                }
                hashCode += 127 * name.hashCode() ^ valueHashCode;
            }

            cachedHashCode = hashCode;
        }
        return cachedHashCode;
    }


    private boolean checkEquals(Object obj) throws Exception {
        if (obj == null)
            return false;


        if (obj instanceof Proxy) {
            InvocationHandler ih = Proxy.getInvocationHandler(obj);
            if (ih instanceof AnnotationImpl) {
                AnnotationImpl other = (AnnotationImpl) ih;
                return annotation.equals(other.annotation);
            }
        }

        Class<?> otherAnnotationType = (Class<?>) JDK_ANNOTATION_TYPE_METHOD.invoke(obj);
        if (getAnnotationType().equals(otherAnnotationType) == false)
           return false;

        Method[] methods = annotationType.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++ i) {
            String name = methods[i].getName();


            MemberValue mv = annotation.getMemberValue(name);
            Object value = null;
            Object otherValue = null;
            try {
               if (mv != null)
                   value = mv.getValue(classLoader, pool, methods[i]);
               if (value == null)
                   value = getDefault(name, methods[i]);
               otherValue = methods[i].invoke(obj);
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException("Error retrieving value " + name + " for annotation " + annotation.getTypeName(), e);
            }

            if (value == null && otherValue != null)
                return false;
            if (value != null && value.equals(otherValue) == false)
                return false;
        }

        return true;
    }


    private static int arrayHashCode(Object object)
    {
       if (object == null)
          return 0;

       int result = 1;

       Object[] array = (Object[]) object;
       for (int i = 0; i < array.length; ++i) {
           int elementHashCode = 0;
           if (array[i] != null)
              elementHashCode = array[i].hashCode();
           result = 31 * result + elementHashCode;
       }
       return result;
    }
}
