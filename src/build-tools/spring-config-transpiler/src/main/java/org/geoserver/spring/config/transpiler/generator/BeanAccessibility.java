/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import java.lang.reflect.Constructor;
import lombok.experimental.UtilityClass;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.jspecify.annotations.Nullable;

/**
 * Rejects a bean when generated code cannot instantiate it without reflection.
 *
 * <p>A generated configuration class calls {@code new} on the bean class. That needs a public class and a public
 * constructor. Spring's XML reader makes a non-public constructor accessible through reflection; the transpiler does
 * not. The error names the bean and the class, and the fix is made upstream, in GeoServer.
 */
@UtilityClass
class BeanAccessibility {

    /** @throws InaccessibleBeanException when {@code beanClass} or {@code constructor} is not public */
    public static void requirePublic(
            TranspilationContext context, String beanName, Class<?> beanClass, @Nullable Constructor<?> constructor) {

        if (!Reflections.isPublic(beanClass)) {
            throw inaccessible(context, beanName, "the non-public class " + beanClass.getName());
        }
        if (constructor != null && !Reflections.isPublic(constructor)) {
            throw inaccessible(context, beanName, "the non-public constructor " + constructor);
        }
    }

    private static InaccessibleBeanException inaccessible(TranspilationContext context, String beanName, String what) {
        String[] locations = context.getXmlLocations();
        String where = locations.length == 0 ? "" : " in " + String.join(", ", locations);
        String message =
                """
                Bean '%s'%s has %s. GeoServer Cloud instantiates XML beans from generated Java configuration \
                classes, and generated code cannot call a non-public class or constructor without reflection. \
                Make the class and its constructor public, or exclude the bean from the transpiled configuration."""
                        .formatted(beanName, where, what);
        return new InaccessibleBeanException(message);
    }
}
