/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.geoserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.SuffixAwareHandlerMapping;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Assertions on which configuration owns Spring MVC in a running application context.
 *
 * <p>A context may hold more than one bean of type {@link WebMvcConfigurationSupport} without conflict, as long as only
 * one of them configures Spring MVC. Two mechanisms decide that, and neither is bean count:
 *
 * <ul>
 *   <li>{@code ConfigurationClassParser} claims a configuration class's superclasses the first time it encounters them.
 *       Whichever {@code WebMvcConfigurationSupport} subclass is parsed first claims that superclass and the
 *       infrastructure beans declared there; a subclass parsed afterwards contributes only the {@code @Bean} methods it
 *       declares itself, which is enough to silently take over an individual bean such as
 *       {@code mvcContentNegotiationManager} while leaving the rest to the first one.
 *   <li>A {@code WebMvcConfigurationSupport} subclass registered through a {@code @Bean} factory method is not a
 *       configuration class at all - {@code ConfigurationClassUtils.checkConfigurationClassCandidate} rejects any bean
 *       definition with a factory method - and contributes nothing. GeoServer's {@code APIConfigurationSupport} reaches
 *       the context that way, on purpose: the OGC API dispatcher calls its handler-adapter method directly instead of
 *       letting it configure the context.
 * </ul>
 *
 * <p>These assertions therefore check that one named configuration owns every bean of the Spring MVC infrastructure,
 * which is what "single ownership" has to mean for split ownership to be caught.
 */
public final class SpringMvcOwnership {

    private static final String REQUEST_MAPPING_HANDLER_MAPPING = "requestMappingHandlerMapping";

    /**
     * The beans a {@link WebMvcConfigurationSupport} declares that decide how requests are dispatched and rendered: the
     * handler adapter and its message converters, the handler mapping, and the content negotiation, conversion and
     * validation services all three of them are built from.
     */
    private static final String[] MVC_INFRASTRUCTURE_BEANS = {
        "requestMappingHandlerAdapter",
        REQUEST_MAPPING_HANDLER_MAPPING,
        "mvcContentNegotiationManager",
        "mvcConversionService",
        "mvcValidator"
    };

    private static final String BOOT_DEFAULT_MVC_CONFIGURATION =
            "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration$EnableWebMvcConfiguration";

    private SpringMvcOwnership() {
        // static assertions only
    }

    /**
     * Asserts that GeoServer's {@link RestConfiguration} owns Spring MVC's infrastructure beans, which is what the REST
     * and GWC services need: it registers the converters the REST API answers with and the handler mapping that
     * resolves path extensions such as {@code /rest/styles/line.json}.
     */
    public static void assertGeoServerRestConfigurationOwnsSpringMvc(ApplicationContext context) {
        assertMvcInfrastructureOwnedBy(context, RestConfiguration.class.getName());
        assertThat(context.getBean(REQUEST_MAPPING_HANDLER_MAPPING)).isInstanceOf(SuffixAwareHandlerMapping.class);
    }

    /**
     * Asserts that Spring Boot's auto-configured Web MVC setup owns Spring MVC's infrastructure beans. Expected of
     * every service that serves neither the REST API nor GWC. Extension modules such as OGC API Features can still
     * contribute their own {@code WebMvcConfigurationSupport} bean to the context (see the class javadoc); that is fine
     * as long as they leave every infrastructure bean to Boot.
     *
     * <p>Also asserts that GeoServer's {@link RestConfiguration} itself is absent: a service serving neither the REST
     * API nor GWC has no reason to pull it into the context, and its presence here would mean some other configuration
     * is importing it outside {@code GeoServerRestFrameworkAutoConfiguration}. Checked by bean name rather than type:
     * the OGC API Features extension's {@code APIConfigurationSupport} is itself a {@link RestConfiguration} subclass,
     * registered under its own bean name precisely so it does not conflict with GeoServer's, and a type-based check
     * would wrongly flag it too.
     */
    public static void assertSpringBootOwnsSpringMvc(ApplicationContext context) {
        assertMvcInfrastructureOwnedBy(context, BOOT_DEFAULT_MVC_CONFIGURATION);
        assertThat(context.containsBean(RestConfiguration.class.getName())).isFalse();
    }

    private static void assertMvcInfrastructureOwnedBy(ApplicationContext context, String expectedOwner) {
        for (String beanName : MVC_INFRASTRUCTURE_BEANS) {
            assertThat(factoryBeanName(context, beanName))
                    .as("%s owner", beanName)
                    .isEqualTo(expectedOwner);
        }
    }

    private static String factoryBeanName(ApplicationContext context, String beanName) {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
        BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
        return definition.getFactoryBeanName();
    }
}
