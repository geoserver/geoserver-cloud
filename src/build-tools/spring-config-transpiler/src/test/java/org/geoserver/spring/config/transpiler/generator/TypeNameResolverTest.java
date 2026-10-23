/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.generator.TypeNameResolver.TypeResolutionResult;
import org.junit.jupiter.api.Test;

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
}
