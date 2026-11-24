/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.geoserver.config.GeoServer;
import org.geoserver.kml.regionate.ReflectiveRegionatingStrategyFactory;
import org.geoserver.ows.OWS11ServiceExceptionHandler;
import org.geoserver.platform.Service;
import org.geoserver.spring.config.test.ambiguous.TiedConstructorsBean;
import org.geoserver.spring.config.transpiler.generator.TargetConstructorResolver.ConstructorResolution;
import org.geoserver.wcs.response.LegacyServiceExceptionHandler;
import org.geoserver.wfs.response.Wfs2ExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

class TargetConstructorResolverTest {

    private static final Map<String, Class<?>> DECLARED_TYPES = Map.of(
            "wcsService-1.0.0", Service.class,
            "wcsService-1.1.0", Service.class,
            "wfsService-2.0", Service.class,
            "name", String.class);

    private final TargetConstructorResolver resolver =
            new TargetConstructorResolver(beanName -> Optional.ofNullable(DECLARED_TYPES.get(beanName)));

    @Test
    void wcs100PicksServiceConstructor() throws NoSuchMethodException {
        ConstructorArgumentValues args = arguments(reference("wcsService-1.0.0"), reference("geoServer"));
        Constructor<?> expected = LegacyServiceExceptionHandler.class.getConstructor(Service.class, GeoServer.class);

        List<ConstructorResolution> resolutions =
                resolveInBothOrders("wcs100ExceptionHandler", LegacyServiceExceptionHandler.class, args);

        assertThat(resolutions).allSatisfy(resolution -> {
            assertThat(resolution.constructor()).isEqualTo(expected);
            assertThat(resolution.wrappedBeanTypes()).isEmpty();
        });
    }

    @Test
    void kmlPicksStringConstructor() throws NoSuchMethodException {
        ConstructorArgumentValues args = arguments(
                new TypedStringValue("random"),
                new TypedStringValue("org.geoserver.kml.regionate.RandomRegionatingStrategy"),
                reference("geoServer"));
        Constructor<?> expected =
                ReflectiveRegionatingStrategyFactory.class.getConstructor(String.class, String.class, GeoServer.class);

        List<ConstructorResolution> resolutions =
                resolveInBothOrders("randomRegionator", ReflectiveRegionatingStrategyFactory.class, args);

        assertThat(resolutions).extracting(ConstructorResolution::constructor).containsOnly(expected);
    }

    @Test
    void listArgumentPicksList() throws NoSuchMethodException {
        ManagedList<Object> services = new ManagedList<>();
        services.add(reference("wcsService-1.0.0"));
        services.add(reference("wcsService-1.1.0"));
        ConstructorArgumentValues args = arguments(services);
        Constructor<?> expected = OWS11ServiceExceptionHandler.class.getConstructor(List.class);

        List<ConstructorResolution> resolutions =
                resolveInBothOrders("wcs111ExceptionHandler", OWS11ServiceExceptionHandler.class, args);

        assertThat(resolutions).extracting(ConstructorResolution::constructor).containsOnly(expected);
    }

    @Test
    void singleBeanIsWrapped() {
        ConstructorArgumentValues args = arguments(reference("wfsService-2.0"), reference("geoServer"));

        ConstructorResolution resolution = resolver.resolve("wfs20ExceptionHandler", Wfs2ExceptionHandler.class, args);

        assertThat(resolution.wrappedBeanType(0)).contains(Service.class);
        assertThat(resolution.wrappedBeanType(1)).isEmpty();
    }

    @Test
    void unknownTypeForListFails() {
        ConstructorArgumentValues args = arguments(reference("unknownService"), reference("geoServer"));

        assertThatThrownBy(() -> resolver.resolve("wfs20ExceptionHandler", Wfs2ExceptionHandler.class, args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'wfs20ExceptionHandler'")
                .hasMessageContaining("'unknownService'")
                .hasMessageContaining("unknown");
    }

    @Test
    void tieFails() {
        ConstructorArgumentValues args = arguments(reference("name"));

        assertThatThrownBy(() -> resolver.resolve("tied", TiedConstructorsBean.class, args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'tied'")
                .hasMessageContaining("TiedConstructorsBean(CharSequence)")
                .hasMessageContaining("TiedConstructorsBean(Comparable)");
    }

    @Test
    void unknownTypeDisagreementFails() {
        ConstructorArgumentValues args = arguments(reference("unknownService"), reference("geoServer"));

        assertThatThrownBy(() -> resolver.resolve("wcs100ExceptionHandler", LegacyServiceExceptionHandler.class, args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'wcs100ExceptionHandler'")
                .hasMessageContaining("'unknownService'")
                .hasMessageContaining("unknown");
    }

    // Spring's build-time resolver decides for most beans of the GeoServer XML: there, the choice must be the same
    @Test
    void agreesWithSpring() throws IOException {
        DefaultListableBeanFactory beanFactory = beanFactoryWithGeoServerXml();
        TargetConstructorResolver withSpringTypes =
                new TargetConstructorResolver(beanName -> typeKnownBySpring(beanFactory, beanName));
        List<String> compared = new ArrayList<>();
        Map<String, String> disagreements = new TreeMap<>();

        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Optional<Constructor<?>> springChoice = springChoice(beanFactory, beanName);
            if (springChoice.isPresent()) {
                compared.add(beanName);
                String spring = springChoice.get().toGenericString();
                String transpiler = transpilerChoice(withSpringTypes, beanFactory, beanName);
                if (!spring.equals(transpiler)) {
                    disagreements.put(beanName, "Spring: %s, transpiler: %s".formatted(spring, transpiler));
                }
            }
        }

        assertThat(disagreements).isEmpty();
        assertThat(compared)
                .hasSizeGreaterThan(300)
                .contains(
                        "wcs100ExceptionHandler",
                        "randomRegionator",
                        "resourceLoader",
                        "geoserverConsoleDisabled",
                        "webDispatcherMappingSystem");
    }

    /** The bean definitions of the GeoServer XML files on the test classpath, as Spring reads them. */
    private static DefaultListableBeanFactory beanFactoryWithGeoServerXml() throws IOException {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setAllowBeanDefinitionOverriding(true);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource resource : resolver.getResources("classpath*:applicationContext.xml")) {
            reader.loadBeanDefinitions(resource);
        }
        return beanFactory;
    }

    /** The type of a bean as Spring's build-time resolver sees it, without creating factory beans. */
    private static Optional<Class<?>> typeKnownBySpring(DefaultListableBeanFactory beanFactory, String beanName) {
        try {
            return Optional.ofNullable(beanFactory.getType(beanName, false));
        } catch (BeansException e) {
            return Optional.empty();
        }
    }

    /** The constructor chosen by Spring's build-time resolver for a bean with constructor arguments. */
    private static Optional<Constructor<?>> springChoice(DefaultListableBeanFactory beanFactory, String beanName) {
        BeanDefinition definition = beanFactory.getMergedBeanDefinition(beanName);
        boolean hasConstructorArguments = !definition.isAbstract()
                && definition.hasConstructorArgumentValues()
                && definition.getFactoryMethodName() == null
                && definition.getBeanClassName() != null;
        if (!hasConstructorArguments) {
            return Optional.empty();
        }
        try {
            RegisteredBean bean = RegisteredBean.of(beanFactory, beanName);
            Executable executable = bean.resolveInstantiationDescriptor().executable();
            return Optional.of((Constructor<?>) executable);
        } catch (RuntimeException | LinkageError e) {
            // Spring fails on lists, maps and some child beans of the XML: those beans are not compared
            return Optional.empty();
        }
    }

    private static String transpilerChoice(
            TargetConstructorResolver resolver, DefaultListableBeanFactory beanFactory, String beanName) {
        BeanDefinition definition = beanFactory.getMergedBeanDefinition(beanName);
        try {
            Class<?> beanClass = ClassUtils.forName(definition.getBeanClassName(), beanFactory.getBeanClassLoader());
            ConstructorArgumentValues args = definition.getConstructorArgumentValues();
            Constructor<?> constructor =
                    resolver.resolve(beanName, beanClass, args).constructor();
            return constructor == null ? "none" : constructor.toGenericString();
        } catch (IllegalStateException | ClassNotFoundException e) {
            return "failure: " + e.getMessage();
        }
    }

    /** Resolves with the candidates in reflection order, then reversed: both must give the same constructor. */
    private List<ConstructorResolution> resolveInBothOrders(
            String beanName, Class<?> beanClass, ConstructorArgumentValues args) {
        List<Constructor<?>> candidates = publicConstructors(beanClass, args.getArgumentCount());
        List<Constructor<?>> reversed = new ArrayList<>(candidates);
        Collections.reverse(reversed);
        assertThat(candidates).hasSizeGreaterThan(1);

        ConstructorResolution inReflectionOrder = resolver.resolve(beanName, candidates, args);
        ConstructorResolution inReverseOrder = resolver.resolve(beanName, reversed, args);
        return List.of(inReflectionOrder, inReverseOrder);
    }

    private static List<Constructor<?>> publicConstructors(Class<?> beanClass, int parameterCount) {
        return Arrays.stream(beanClass.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == parameterCount)
                .toList();
    }

    private static ConstructorArgumentValues arguments(Object... values) {
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        for (Object value : values) {
            args.addGenericArgumentValue(value);
        }
        return args;
    }

    private static RuntimeBeanReference reference(String beanName) {
        return new RuntimeBeanReference(beanName);
    }
}
