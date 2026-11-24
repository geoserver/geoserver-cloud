/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.geoserver.spring.config.test.ambiguous.TiedConstructorsBean;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.generator.TypeNameResolver.TypeResolutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.GenericBeanDefinition;

class TypeNameResolverTest {

    private final TranspilationContext context =
            TranspilationContext.forTesting("org.geoserver.test.generated", "TestConfig", false, false);

    @Test
    void testTypeNameWithSpaces() {
        TypeResolutionResult result =
                TypeNameResolver.resolveTypeName(" org.geoserver.security.xml.XMLSecurityProvider ", context);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.getResolvedTypeName()).isEqualTo("org.geoserver.security.xml.XMLSecurityProvider");
    }

    @Test
    void testBlankTypeName() {
        TypeResolutionResult result = TypeNameResolver.resolveTypeName("   ", context);

        assertThat(result.isResolved()).isFalse();
        assertThat(result.getResolvedTypeName()).isEqualTo("java.lang.Object");
    }

    // a bean with tied constructors may be excluded from the transpilation: the inference skips it
    @Test
    void inferenceSkipsTiedBeans() {
        GenericBeanDefinition tied = new GenericBeanDefinition();
        tied.setBeanClassName(TiedConstructorsBean.class.getName());
        tied.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("external"));
        Map<String, BeanDefinition> definitions = Map.of("tied", tied);

        TypeResolutionResult result = TypeNameResolver.resolveDependencyType("external", definitions, context);

        assertThat(result.isResolved()).isFalse();
    }
}
