/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto.mapper;

import java.util.function.Supplier;
import org.geoserver.catalog.Info;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ImageProcessingInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.ImageProcessingInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.jackson.databind.config.dto.GeoServer;
import org.geoserver.jackson.databind.config.dto.Logging;
import org.geoserver.jackson.databind.config.dto.Settings;
import org.geoserver.ows.util.OwsUtils;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

/**
 * Auto-wired object factory for config info interfaces, so the mapstruct code-generated mappers
 * know how to instantiate them
 */
@Component
public class ObjectFacotries {

    private <T extends Info> T create(String id, Supplier<T> factoryMethod) {
        T info = factoryMethod.get();
        OwsUtils.set(info, "id", id);
        return info;
    }

    public @ObjectFactory GeoServerInfo geoServerInfo(GeoServer source) {
        return create(source.getId(), GeoServerInfoImpl::new);
    }

    public @ObjectFactory SettingsInfo settingsInfo(Settings source) {
        return create(source.getId(), SettingsInfoImpl::new);
    }

    public @ObjectFactory LoggingInfo loggingInfo(Logging source) {
        return create(source.getId(), LoggingInfoImpl::new);
    }

    public @ObjectFactory CoverageAccessInfo coverageAccessInfo() {
        return new CoverageAccessInfoImpl();
    }

    public @ObjectFactory ImageProcessingInfo imageProcessingInfo() {
        return new ImageProcessingInfoImpl();
    }

    public @ObjectFactory ContactInfo contactInfo() {
        return new ContactInfoImpl();
    }
}
