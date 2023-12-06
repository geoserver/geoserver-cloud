/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.ui;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.springframework.boot.info.BuildProperties;

/**
 * Adds gs-cloud version info to the home page from spring-boot's {@link BuildProperties}
 *
 * @since 1.0
 */
@RequiredArgsConstructor
public class GeoServerCloudHomePageContentProvider implements GeoServerHomePageContentProvider {

    private final @NonNull GeoServerSecurityManager secManager;

    private final @NonNull BuildProperties buildProperties;

    @Override
    public Component getPageBodyComponent(String id) {
        if (secManager.checkAuthenticationForAdminRole()) {
            return new GeoServerCloudStatusPanel(id, buildProperties);
        }
        return new WebMarkupContainer(id); // Placeholder
    }
}
