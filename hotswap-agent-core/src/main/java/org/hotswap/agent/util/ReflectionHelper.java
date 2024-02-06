
package org.hotswap.agent.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.hotswap.agent.logging.AgentLogger;


public class ReflectionHelper {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ReflectionHelper.class);


    public static Object invoke(Object target, Class<?> clazz, String methodName, Class<?>[] parameterTypes,
            Object... args) {
        try {
            Method method = null;
            try {
                method = clazz.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
            }
            method.setAccessible(true);

            return method.invoke(target, args);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Illegal arguments method %s.%s(%s) on %s, params %s", clazz.getName(), methodName,
                    Arrays.toString(parameterTypes), target, Arrays.toString(args)), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(String.format("Error invoking method %s.%s(%s) on %s, params %s", clazz.getName(), methodName,
                    Arrays.toString(parameterTypes), target, Arrays.toString(args)), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("No such method %s.%s(%s) on %s, params %s", clazz.getName(), methodName,
                    Arrays.toString(parameterTypes), target, Arrays.toString(args)), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("No such method %s.%s(%s) on %s, params %s", clazz.getName(), methodName,
                    Arrays.toString(parameterTypes), target, Arrays.toString(args)), e);
        }
    }


    public static Object invokeNoException(Object target, String className, ClassLoader cl, String methodName,
            Class<?>[] parameterTypes, Object... args) {
        Class<?> clazz;
        try {
            clazz = cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Class {} not found", e, className);
            return null;
        }

        try {
            return invoke(target, clazz, methodName, parameterTypes, args);
        } catch (IllegalArgumentException e) {
            LOGGER.trace("Method {}.{} not found", e, className, methodName);
            return null;
        }
    }


    public static Object invoke(Object target, String methodName) {
        return invoke(target, target.getClass(), methodName, new Class[] {});
    }

    public static Object invokeConstructor(String className, ClassLoader cl, Class<?>[] parameterTypes,
                                           Object... args) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = Class.forName(className, true, cl);
        Constructor constructor = clazz.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }


    public static Object get(Object target, String fieldName) {
        if (target == null)
            throw new NullPointerException("Target object cannot be null.");

        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                break;
            } catch (NoSuchFieldException e) {

            }
            clazz = clazz.getSuperclass();
        }

        if (clazz == null) {
            throw new IllegalArgumentException(String.format("No such field %s.%s on %s", target.getClass(), fieldName, target));
        }

        return get(target, clazz, fieldName);
    }


    public static Object get(Object target, Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(String.format("No such field %s.%s on %s", clazz.getName(), fieldName, target), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Illegal access field %s.%s on %s", clazz.getName(), fieldName, target), e);
        }
    }


    public static Object getNoException(Object target, Class<?> clazz, String fieldName) {
        try {
            return get(target, clazz, fieldName);
        } catch (Exception e) {
            LOGGER.trace("Error getting field {}.{} on object {}", e, clazz, fieldName, target);
            return null;
        }
    }


    public static Object getNoException(Object target, String className, ClassLoader cl, String fieldName) {
        Class<?> clazz;
        try {
            clazz = cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Class {} not found", e, className);
            return null;
        }

        return getNoException(target, clazz, fieldName);
    }


    public static void set(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(String.format("No such field %s.%s on %s", clazz.getName(), fieldName, target), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Illegal access field %s.%s on %s", clazz.getName(), fieldName, target), e);
        }
    }

    public static void set(Object target, String fieldName, Object value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                break;
            } catch (NoSuchFieldException e) {

            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(String.format("Illegal access field %s.%s on %s", clazz.getName(),
                        fieldName, target), e);
            }
            clazz = clazz.getSuperclass();
        }

        if (clazz == null) {
            throw new IllegalArgumentException(String.format("No such field %s.%s on %s", target.getClass(), fieldName, target));
        }
    }
}
