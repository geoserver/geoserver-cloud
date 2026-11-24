/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import com.palantir.javapoet.ClassName;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

@UtilityClass
public class Reflections {

    public boolean isPublic(Constructor<?> constructor) {
        return java.lang.reflect.Modifier.isPublic(constructor.getModifiers());
    }

    public boolean isPublic(Class<?> type) {
        return java.lang.reflect.Modifier.isPublic(type.getModifiers());
    }

    /**
     * Walk the class hierarchy of {@code type} (super-classes first, then interfaces) and return the nearest public
     * ancestor, or {@link Object} if none is found. Used to pick a method return/parameter type that is accessible from
     * other packages when the declared bean class is itself package-private.
     */
    public Class<?> findFirstPublicAncestor(Class<?> type) {
        if (type == null) {
            return Object.class;
        }
        // Prefer super-classes (walk up to Object)
        Class<?> walker = type.getSuperclass();
        while (walker != null) {
            if (isPublic(walker)) {
                return walker;
            }
            walker = walker.getSuperclass();
        }
        // Fall back to any public interface implemented directly or indirectly
        return findFirstPublicInterface(type).orElse(Object.class);
    }

    private Optional<Class<?>> findFirstPublicInterface(Class<?> type) {
        for (Class<?> iface : type.getInterfaces()) {
            if (isPublic(iface)) {
                return Optional.of(iface);
            }
            Optional<Class<?>> nested = findFirstPublicInterface(iface);
            if (nested.isPresent()) {
                return nested;
            }
        }
        Class<?> parent = type.getSuperclass();
        if (parent != null) {
            return findFirstPublicInterface(parent);
        }
        return Optional.empty();
    }

    /** Check if a class represents a numeric type that should be rendered as a literal. */
    public static boolean isPrimitiveOrBoxedType(Class<?> type) {
        return type.isPrimitive()
                || type == Integer.class
                || type == Long.class
                || type == Short.class
                || type == Byte.class
                || type == Float.class
                || type == Double.class
                || type == Boolean.class;
    }

    public String buildSetterName(final String propertyName) {
        return "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    /**
     * Check if a type has a constructor that accepts a single String parameter. This method uses reflection to
     * determine if a non-String type can be constructed from a String.
     */
    public boolean hasStringConstructor(Class<?> type) {
        if (type == null || type == String.class) {
            return false;
        }
        try {
            // Use reflection to check for String constructor
            type.getConstructor(String.class);
            return true;
        } catch (NoSuchMethodException _) {
            return false;
        }
    }

    /**
     * The element type of a collection parameter of a constructor: {@code Service} for {@code List<Service>}. Null
     * without a constructor, and for a parameter without a class as its type argument.
     */
    public @Nullable ClassName getGenericCollectionElementType(@Nullable Constructor<?> constructor, int paramIndex) {
        if (constructor == null || paramIndex >= constructor.getParameterCount()) {
            return null;
        }
        Type genericType = constructor.getGenericParameterTypes()[paramIndex];
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> elementClass) {
                return ClassName.get(elementClass);
            }
        }
        return null;
    }

    public Class<?> convertToRuntimeClass(String beanClassName) {
        try {
            return loadForInspection(convertToRuntimeClassName(beanClassName));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Load a class to inspect its type metadata, without running its static initializer.
     *
     * <p>The transpiler only reads signatures, assignability and identity from the classes it loads. Initializing them
     * is both unnecessary and harmful: a static initializer runs application code at annotation-processing time, and
     * that code reaches for runtime services the annotation processor classpath does not provide.
     *
     * <p>Surrounding whitespace in {@code className} is ignored.
     */
    public Class<?> loadForInspection(String className) throws ClassNotFoundException {
        boolean initialize = false;
        return Class.forName(className.trim(), initialize, Reflections.class.getClassLoader());
    }

    /**
     * Convert a class name from Spring XML format to the format expected by Class.forName(). For inner classes, Spring
     * XML uses dot notation (e.g., "Outer.Inner") but Class.forName() expects dollar notation (e.g., "Outer$Inner").
     */
    public String convertToRuntimeClassName(String beanClassName) {
        // Convert any inner class dot notation to dollar notation
        // This handles cases like "org.geoserver.wps.ppio.GeoJSONPPIO.Geometries"
        // to "org.geoserver.wps.ppio.GeoJSONPPIO$Geometries"
        String className = beanClassName.trim();
        String[] parts = className.split("\\.");
        if (parts.length < 2) {
            return className; // No package or simple class name
        }

        // Try to detect if this might be an inner class by checking if any part
        // after the first uppercase letter could be a class name
        StringBuilder runtimeClassName = new StringBuilder();
        boolean foundFirstClass = false;

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                String part = parts[i];
                // If we've found a class-like name and this part starts with uppercase,
                // it's likely an inner class
                if (foundFirstClass && Character.isUpperCase(part.charAt(0))) {
                    runtimeClassName.append("$");
                } else {
                    runtimeClassName.append(".");
                }
            }
            runtimeClassName.append(parts[i]);

            // Mark that we've found a class-like name (starts with uppercase)
            if (!foundFirstClass && Character.isUpperCase(parts[i].charAt(0))) {
                foundFirstClass = true;
            }
        }

        return runtimeClassName.toString();
    }

    /**
     * The constructor of a bean without constructor arguments, as Spring finds it: the no-arg constructor, else the
     * single constructor to autowire. Null when there is neither.
     */
    @SuppressWarnings("java:S1452") // wildcard in return type
    public static @Nullable Constructor<?> resolveImplicitConstructor(Class<?> beanClass) {
        try {
            return beanClass.getDeclaredConstructor();
        } catch (NoSuchMethodException _) {
            return resolveAutowiredConstructor(beanClass);
        }
    }

    /**
     * Resolve constructor for implicit autowiring following Spring semantics: returns the single public constructor if
     * it has parameters, or the single declared constructor if it has parameters. Returns null if ambiguous.
     */
    @SuppressWarnings("java:S1452") // wildcard in return type
    public static @Nullable Constructor<?> resolveAutowiredConstructor(Class<?> beanClass) {
        Constructor<?>[] publicCtors = beanClass.getConstructors();
        if (publicCtors.length == 1 && publicCtors[0].getParameterCount() > 0) {
            return publicCtors[0];
        }
        Constructor<?>[] allCtors = beanClass.getDeclaredConstructors();
        if (allCtors.length == 1 && allCtors[0].getParameterCount() > 0) {
            return allCtors[0];
        }
        return null;
    }

    /** Convert a Class to ClassName for JavaPoet, boxing primitives for method parameter compatibility. */
    public static ClassName classToClassName(Class<?> clazz) {
        return ClassName.get(getBoxedType(clazz));
    }

    /** Get the boxed type ClassName for a primitive type. */
    public static Class<?> getBoxedType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return switch (clazz) {
                case Class<?> c when c == int.class -> Integer.class;
                case Class<?> c when c == long.class -> Long.class;
                case Class<?> c when c == double.class -> Double.class;
                case Class<?> c when c == float.class -> Float.class;
                case Class<?> c when c == boolean.class -> Boolean.class;
                case Class<?> c when c == byte.class -> Byte.class;
                case Class<?> c when c == short.class -> Short.class;
                case Class<?> c when c == char.class -> Character.class;
                default -> throw new IllegalArgumentException("Unknown primitive type: %s".formatted(clazz));
            };
        }
        return clazz;
    }
}
