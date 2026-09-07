/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Registers the access denied page with {@link GeoServerExtensionsHelper} before every Spring test context starts.
 *
 * <p>GeoServer's exception translation filters register {@code /accessDenied.html} as their error page only when
 * {@code GeoServerExtensions.file} resolves it, and log "Cannot find: /accessDenied.html" on every filter chain build
 * otherwise. That lookup goes through {@code ServletContext.getRealPath}, which embedded Tomcat answers for the page
 * shipped inside this module's jar, while the mock servlet context of a {@code @SpringBootTest} without a web server
 * cannot turn a jar entry into a path. The helper's file cache is consulted ahead of the servlet context, which makes
 * the lookup succeed either way; the response itself is served from the classpath by
 * {@link AccessDeniedPageAutoConfiguration}.
 *
 * <p>Registered through this module's test {@code META-INF/spring.factories}, hence applied to every Spring test of the
 * modules with this test jar on their classpath.
 */
public class AccessDeniedPageContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return AccessDeniedPageContextCustomizer.INSTANCE;
    }

    /** Stateless, hence a single instance: customizers take part in the key of the test context cache. */
    static final class AccessDeniedPageContextCustomizer implements ContextCustomizer {

        static final AccessDeniedPageContextCustomizer INSTANCE = new AccessDeniedPageContextCustomizer();

        static final String ACCESS_DENIED_PAGE = "/accessDenied.html";

        private static volatile File extractedPage;

        private AccessDeniedPageContextCustomizer() {
            // single instance
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
            GeoServerExtensionsHelper.file(ACCESS_DENIED_PAGE, extractedPage());
        }

        /** The page copied out of the classpath into a temporary file, once per JVM. */
        static File extractedPage() {
            File page = extractedPage;
            if (page == null) {
                page = extract();
                extractedPage = page;
            }
            return page;
        }

        private static File extract() {
            ClassPathResource resource = new ClassPathResource("META-INF/resources" + ACCESS_DENIED_PAGE);
            try (InputStream in = resource.getInputStream()) {
                Path copy = Files.createTempFile("accessDenied", ".html");
                copy.toFile().deleteOnExit();
                Files.copy(in, copy, StandardCopyOption.REPLACE_EXISTING);
                return copy.toFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
