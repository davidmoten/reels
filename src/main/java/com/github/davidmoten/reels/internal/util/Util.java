package com.github.davidmoten.reels.internal.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.github.davidmoten.reels.CreateException;

public final class Util {

    private Util() {
        // prevent instantiation
    }

    public static void rethrow(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else if (e instanceof Error) {
            throw (Error) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find the next larger positive power of two value up from the given value. If
     * value is a power of two then this value will be returned.
     *
     * @param value from which next positive power of two will be found.
     * @return the next positive power of 2 or this value if it is a power of 2.
     */
    public static int roundToPowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    @SuppressWarnings("unchecked")
    public static <C> Constructor<C> getMatchingConstructor(Class<C> c, Object[] args) {
        for (Constructor<?> con : c.getDeclaredConstructors()) {
            Class<?>[] types = con.getParameterTypes();
            if (types.length != args.length)
                continue;
            boolean match = true;
            for (int i = 0; i < types.length; i++) {
                Class<?> need = types[i], got = args[i].getClass();
                if (!typesMatch(need, got)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return (Constructor<C>) con;
            }
        }
        throw new CreateException(
                "Cannot find an appropriate constructor for class " + c + " and arguments " + Arrays.toString(args));
    }

    private static boolean typesMatch(Class<?> need, Class<?> got) {
        if (need.isAssignableFrom(got)) {
            return true;
        } else if (need.isPrimitive()) {
            return primitiveTypesMatch(need, got);
        } else {
            return false;
        }
    }

    //VisibleForTesting
    static boolean primitiveTypesMatch(Class<?> need, Class<?> got) {
        return int.class.equals(need) && Integer.class.equals(got) //
                || long.class.equals(need) && Long.class.equals(got) //
                || char.class.equals(need) && Character.class.equals(got) //
                || short.class.equals(need) && Short.class.equals(got) //
                || boolean.class.equals(need) && Boolean.class.equals(got) //
                || byte.class.equals(need) && Byte.class.equals(got)
                || double.class.equals(need) && Double.class.equals(got) //
                || float.class.equals(need) && Float.class.equals(got);
    }

}
