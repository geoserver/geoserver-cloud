package org.geoserver.cloud.config.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class FilteringXmlBeanDefinitionReaderTest {

    private ClassPathXmlApplicationContext context;

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
    public @Before void before() {
        registry = new DefaultListableBeanFactory();
        reader = new FilteringXmlBeanDefinitionReader(registry);
    }

    public @Test void testLoadAll() {
        verify(
                baseResource,
                "filterFactory",
                "geoServer",
                "memoryLockProvider",
                "nullLockProvider");
    }

    public @Test void testFilterAll() {
        String location =
                baseResource
                        + "#name=^(nullLockProvider|memoryLockProvider|filterFactory|geoServer)$";
        verify(location);
    }

    public @Test void testFilterByIdAttribute() {
        String location = baseResource + "#name=^(nullLockProvider|memoryLockProvider)$";
        verify(location, "filterFactory", "geoServer");
    }

    public @Test void testFilterByNameAttribute() {
        String location = baseResource + "#name=^(filterFactory|geoServer)$";
        verify(location, "memoryLockProvider", "nullLockProvider");
    }

    public @Test void testFilterByAliasPostBeanDefinition() {
        String location = baseResource + "#name=^(gsAlias)$";
        verify(location, "filterFactory", "memoryLockProvider", "nullLockProvider");
    }

    public @Test void testFilterByAliasPreBeanDefinition() {
        String location = baseResource + "#name=^(ff)$";
        verify(location, "geoServer", "memoryLockProvider", "nullLockProvider");
    }

    public @Test void testFilterMultipleAliases() {
        String location = baseResource + "#name=^(ff|gsAlias)$";
        verify(location, "memoryLockProvider", "nullLockProvider");
    }

    private void verify(String location, String... expectedBeanNames) {
        Set<String> expected =
                expectedBeanNames == null
                        ? Collections.emptySet()
                        : Arrays.stream(expectedBeanNames)
                                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> loadedNames = loadBeanDefinitionsAndReturnNames(location);
        assertEquals(expected, loadedNames);
    }

    private SortedSet<String> loadBeanDefinitionsAndReturnNames(String location) {
        reader.loadBeanDefinitions(location);
        return Arrays.stream(registry.getBeanDefinitionNames())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
