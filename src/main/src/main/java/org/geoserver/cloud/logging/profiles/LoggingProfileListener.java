/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.logging.profiles;

import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.LoggingInfo;
import org.springframework.context.event.EventListener;

/**
 * Applies the logging profile selected in the GeoServer configuration ({@link LoggingInfo#getLevel()}) when the service
 * loads this configuration, and each time the selection changes.
 *
 * <p>GeoServer runs its {@link GeoServerInitializer initializers} right after it loads its configuration, early in the
 * startup of the service. The rest of the startup logs at the levels of the selected profile, as in vanilla GeoServer.
 *
 * <p>The selection may change on this service instance or on another one. The event bus delivers the changes made on
 * other instances, and all instances then apply the same profile.
 */
@RequiredArgsConstructor
public class LoggingProfileListener implements GeoServerInitializer {

    private static final String PROFILE_PROPERTY = "level";

    private final @NonNull LoggingProfileApplier applier;

    @Override
    public void initialize(GeoServer geoServer) {
        LoggingInfo logging = geoServer.getLogging();
        if (logging != null) {
            applier.apply(logging.getLevel());
        }
    }

    @EventListener(LoggingInfoSet.class)
    public void onLoggingInfoSet(LoggingInfoSet event) {
        LoggingInfo logging = event.getObject();
        applier.apply(logging.getLevel());
    }

    @EventListener(LoggingInfoModified.class)
    public void onLoggingInfoModified(LoggingInfoModified event) {
        Patch patch = event.getPatch();
        Optional<Patch.Property> profile = patch.get(PROFILE_PROPERTY);
        if (profile.isPresent()) {
            String profileName = (String) profile.get().getValue();
            applier.apply(profileName);
        }
    }
}
