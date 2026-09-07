/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import org.geoserver.cloud.autoconfigure.main.AccessDeniedPageContextCustomizerFactory.AccessDeniedPageContextCustomizer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

/** Tests for {@link AccessDeniedPageContextCustomizerFactory}. */
class AccessDeniedPageContextCustomizerTest {

    @AfterEach
    void clearExtensions() {
        GeoServerExtensionsHelper.clear();
    }

    /**
     * What GeoServer's exception translation filters check before registering the page as their error page: the lookup
     * must succeed with no servlet context at all, as it has to under the mock servlet context of a test.
     */
    @Test
    void pageResolvesThroughGeoServerExtensionsOnceTheContextIsCustomized() throws Exception {
        assertThat(GeoServerExtensions.file(AccessDeniedPageContextCustomizer.ACCESS_DENIED_PAGE))
                .isNull();

        new AccessDeniedPageContextCustomizerFactory()
                .createContextCustomizer(getClass(), null)
                .customizeContext(new GenericApplicationContext(), null);

        File page = GeoServerExtensions.file(AccessDeniedPageContextCustomizer.ACCESS_DENIED_PAGE);
        assertThat(page).isNotNull().isFile();
        assertThat(Files.readString(page.toPath())).contains("Access Denied");
    }

    /** Customizers take part in the key of the test context cache, a single instance keeps the key stable. */
    @Test
    void customizerIsASingleInstance() {
        AccessDeniedPageContextCustomizerFactory factory = new AccessDeniedPageContextCustomizerFactory();
        assertThat(factory.createContextCustomizer(getClass(), null))
                .isSameAs(factory.createContextCustomizer(Object.class, null));
    }
}
