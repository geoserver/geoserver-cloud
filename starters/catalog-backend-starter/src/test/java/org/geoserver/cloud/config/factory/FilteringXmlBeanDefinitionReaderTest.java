/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.factory;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

@Slf4j(topic = "FilteringXmlBeanDefinitionReaderTest")
public class FilteringXmlBeanDefinitionReaderTest {

    private final String baseResource =
            "classpath:/org/geoserver/cloud/config/factory/FilteringXmlBeanDefinitionReaderTestData.xml";

    FilteringXmlBeanDefinitionReader reader;
    DefaultListableBeanFactory registry;

    public @Rule TestName testName = new TestName();

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
    public @Before void before() {
        registry = new DefaultListableBeanFactory();
        reader = new FilteringXmlBeanDefinitionReader(registry);
    }

    public @Test void noFilterIncludesdAll() {
        verify(
                baseResource,
                "filterFactory",
                "geoServer",
                "memoryLockProvider",
                "nullLockProvider");
    }

    public @Test void includedAll_AnyWord() {
        String location = baseResource + "#name=\\w+";
        verify(location, "filterFactory", "geoServer", "memoryLockProvider", "nullLockProvider");
    }

    public @Test void excludeAll_AnyWord() {
        String location = baseResource + "#name=^(?!\\w+).*$";
        verify(location);
    }

    public @Test void excludeByName() {
        String location = baseResource + "#name=^(?!nullLockProvider|memoryLockProvider).*$";
        verify(location, "filterFactory", "geoServer");
    }

    public @Test void excludeAllExplicitlyByName() {
        String location =
                baseResource
                        + "#name=^(?!nullLockProvider|memoryLockProvider|filterFactory|geoServer)$";
        verify(location);
    }

    public @Test void includeAllExplicitly() {
        String location =
                baseResource
                        + "#name=^(nullLockProvider|memoryLockProvider|filterFactory|geoServer)$";
        verify(location, "filterFactory", "geoServer", "memoryLockProvider", "nullLockProvider");
    }

    public @Test void excludeAllEndingWithProviderOrStartingWithFilter() {
        String location = baseResource + "#name=^(?!.*Provider|.*Factory).*$";
        verify(location, "geoServer");
    }

    public @Test void excludeByIdAttribute() {
        String location = baseResource + "#name=^(?!nullLockProvider|memoryLockProvider).*$";
        verify(location, "filterFactory", "geoServer");
    }

    public @Test void excludeByNameAttribute() {
        String location = baseResource + "#name=^(?!filterFactory|geoServer).*$";
        verify(location, "memoryLockProvider", "nullLockProvider");
    }

    public @Test void includeByNameAttribute() {
        String location = baseResource + "#name=^(filterFactory|geoServer).*$";
        verify(location, "filterFactory", "geoServer");
    }

    private void verify(String location, String... expectedBeanNames) {
        log.info(testName.getMethodName() + ":");
        Set<String> expected =
                expectedBeanNames == null
                        ? Collections.emptySet()
                        : Arrays.stream(expectedBeanNames)
                                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> loadedNames = loadBeanDefinitionsAndReturnNames(location);
        assertEquals("loaded beans don't match expected", expected, loadedNames);
    }

    private SortedSet<String> loadBeanDefinitionsAndReturnNames(String location) {
        reader.loadBeanDefinitions(location);
        return Arrays.stream(registry.getBeanDefinitionNames())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
