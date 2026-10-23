/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.spring.config.test.components.SimpleComponent;
import org.junit.jupiter.api.Test;

class ReflectionsTest {

    @Test
    void testClassNameWithSpaces() {
        String className = " " + SimpleComponent.class.getName() + " ";

        assertThat(Reflections.convertToRuntimeClass(className)).isEqualTo(SimpleComponent.class);
    }

    @Test
    void testLoadForInspectionWithSpaces() throws ClassNotFoundException {
        String className = " " + SimpleComponent.class.getName() + " ";

        assertThat(Reflections.loadForInspection(className)).isEqualTo(SimpleComponent.class);
    }
}
