/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.factory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class FilteringXmlBeanDefinitionReaderTest {

    private final String baseResource =
            "classpath:/org/geoserver/cloud/config/factory/FilteringXmlBeanDefinitionReaderTestData.xml";

    FilteringXmlBeanDefinitionReader reader;
    DefaultListableBeanFactory registry;

    /**
     * These are all the beans defined in the xml resource file:
     *
     * <pre>
     * <code>
     *  <alias name="filterFactory" alias="ff"/>
     *
     *  <bean id="nullLockProvider" class="org.geoserver.platform.resource.NullLockProvider"/>
     *  <bean id="memoryLockProvider" class="org.geoserver.platform.resource.MemoryLockProvider"/>
     *  <bean name="filterFactory" class="org.geotools.filter.FilterFactoryImpl"/>
     *  <bean name="geoServer" class="org.geoserver.config.impl.GeoServerImpl"/>
     *
     *  <alias name="geoServer" alias="gsAlias"/>
     * </code>
     * </pre>
     */
    public @BeforeEach void before() {
        registry = new DefaultListableBeanFactory();
        reader = new FilteringXmlBeanDefinitionReader(registry);
    }

    @Test
    void noFilterIncludesdAll() {
        verify(baseResource, "filterFactory", "geoServer", "memoryLockProvider", "nullLockProvider");
    }

    @Test
    void includedAll_AnyWord() {
        String location = "%s#name=\\w+".formatted(baseResource);
        verify(location, "filterFactory", "geoServer", "memoryLockProvider", "nullLockProvider");
    }

    @Test
    void excludeAll_AnyWord() {
        String location = "%s#name=^(?!\\w+).*$".formatted(baseResource);
        verify(location);
    }

    @Test
    void excludeByName() {
        String location = "%s#name=^(?!nullLockProvider|memoryLockProvider).*$".formatted(baseResource);
        verify(location, "filterFactory", "geoServer");
    }

    @Test
    void excludeAllExplicitlyByName() {
        String location =
                "%s#name=^(?!nullLockProvider|memoryLockProvider|filterFactory|geoServer)$".formatted(baseResource);
        verify(location);
    }

    @Test
    void includeAllExplicitly() {
        String location =
                "%s#name=^(nullLockProvider|memoryLockProvider|filterFactory|geoServer)$".formatted(baseResource);
        verify(location, "filterFactory", "geoServer", "memoryLockProvider", "nullLockProvider");
    }

    @Test
    void excludeAllEndingWithProviderOrStartingWithFilter() {
        String location = "%s#name=^(?!.*Provider|.*Factory).*$".formatted(baseResource);
        verify(location, "geoServer");
    }

    @Test
    void excludeByNameAttribute() {
        String location = "%s#name=^(?!filterFactory|geoServer).*$".formatted(baseResource);
        verify(location, "memoryLockProvider", "nullLockProvider");
    }

    @Test
    void includeByNameAttribute() {
        String location = "%s#name=^(filterFactory|geoServer).*$".formatted(baseResource);
        verify(location, "filterFactory", "geoServer");
    }

    private void verify(String location, String... expectedBeanNames) {
        Set<String> expected = expectedBeanNames == null
                ? Collections.emptySet()
                : Arrays.stream(expectedBeanNames).collect(Collectors.toCollection(TreeSet::new));
        Set<String> loadedNames = loadBeanDefinitionsAndReturnNames(location);
        Assertions.assertEquals(expected, loadedNames, "loaded beans don't match expected");
    }

    private SortedSet<String> loadBeanDefinitionsAndReturnNames(String location) {
        reader.loadBeanDefinitions(location);
        return Arrays.stream(registry.getBeanDefinitionNames()).collect(Collectors.toCollection(TreeSet::new));
    }
}
