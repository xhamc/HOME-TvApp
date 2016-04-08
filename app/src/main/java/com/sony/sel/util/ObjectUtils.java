package com.sony.sel.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Utility class for dealing with Objects in general.
 */
public class ObjectUtils {
    /**
     * checks for equality of two objects, taking into account that one or both
     * of them might be null.
     */
    public static boolean equal(Object o1, Object o2) {
        return ((o1 == o2) || (o1 == null) || (o2 == null)) ? (o1 == o2) : o1.equals(o2);
    }

    /**
     * returns the zero, false, or null value that is the default value
     * for the specified type.
     * 
     * @param c the value type.
     * @return the default value.
     * @throws NullPointerException if c is null.
     */
    public static Object getDefaultValue(Class<?> c) {
        if (!c.isPrimitive() || (c == Void.class) || (c == void.class)) {
            return null;
        }
        if ((c == Boolean.class) || (c == boolean.class)) {
            return Boolean.FALSE;
        }
        else if ((c == Integer.class) || (c == int.class)) {
            return Integer.valueOf((int)0);
        }
        else if ((c == Long.class) || (c == long.class)) {
            return Long.valueOf((long)0);
        }
        else if ((c == Character.class) || (c == char.class)) {
            return Character.valueOf((char)0);
        }
        else if ((c == Byte.class) || (c == byte.class)) {
            return Byte.valueOf((byte)0);
        }
        else if ((c == Short.class) || (c == short.class)) {
            return Short.valueOf((short)0);
        }
        else if ((c == Float.class) || (c == float.class)) {
            return Float.valueOf((float)0);
        }
        else if ((c == Double.class) || (c == double.class)) {
            return Double.valueOf((double)0);
        }
        // shouldn't get here
        return null;
    }
    
    /**
     * Creates a new instance of the specified class, passing the specified arguments to the constructor.
     * If the arguments match multiple constructors, then one will be chosen arbitrarily.
     * 
     * @param clazz the type of Object to create
     * @param args the arguments to pass to the constructor
     * 
     * @return the new Object
     * 
     * @throws InvocationTargetException if the constructor throws an exception
     * @throws IllegalArgumentException if the arguments don't match any of the accessible constructors.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz, Object... args) throws InvocationTargetException, IllegalArgumentException {
        int numArgs = (args == null) ? 0 : args.length;
        for (Constructor<?> c : clazz.getConstructors()) {
            Class<?>[] paramTypes = c.getParameterTypes();
            if (paramTypes.length == args.length) {
                int i = numArgs;
                while (--i >= 0) {
                    if ((args[i] == null) ? paramTypes[i].isPrimitive() : !paramTypes[i].isInstance(args[i])) {
                        break;
                    }
                }
                if (i < 0) {
                    if (Modifier.isPublic(c.getModifiers())) {
                        try {
                            return (T)c.newInstance(args);
                        }
                        catch (InstantiationException e) {
                            // TODO: log
                        } catch (IllegalAccessException e) {
                            // TODO: log
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("matching constructor not found");
    }
}
