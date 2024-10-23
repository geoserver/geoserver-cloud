/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import java.util.Arrays;
import java.util.Objects;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @see LiteralSerializer
 * @see LiteralDeserializer
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class Literal extends Expression {

    private Object value;

    public static Literal valueOf(Object value) {
        return value instanceof Literal l ? l : new Literal().setValue(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Literal)) return false;

        final Object v1 = value;
        final Object v2 = ((Literal) o).value;
        return valueEquals(v1, v2);
    }

    public static boolean valueEquals(final Object v1, final Object v2) {
        if (Objects.equals(v1, v2)) {
            return true;
        }

        if (v1 != null && v1.getClass().isArray() && v2 != null && v2.getClass().isArray()) {
            if (!v1.getClass().equals(v2.getClass())) return false;
            final Class<?> componentType = v1.getClass().getComponentType();

            if (componentType.isPrimitive()) {
                return switch (componentType.getCanonicalName()) {
                    case "byte" -> Arrays.equals((byte[]) v1, (byte[]) v2);
                    case "boolean" -> Arrays.equals((boolean[]) v1, (boolean[]) v2);
                    case "char" -> Arrays.equals((char[]) v1, (char[]) v2);
                    case "short" -> Arrays.equals((short[]) v1, (short[]) v2);
                    case "int" -> Arrays.equals((int[]) v1, (int[]) v2);
                    case "long" -> Arrays.equals((long[]) v1, (long[]) v2);
                    case "float" -> Arrays.equals((float[]) v1, (float[]) v2);
                    case "double" -> Arrays.equals((double[]) v1, (double[]) v2);
                    default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(componentType));
                };
            } else {
                Object[] a1 = (Object[]) v1;
                Object[] a2 = (Object[]) v2;
                return Arrays.deepEquals(a1, a2);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Literal.class, value);
    }
}
