/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogAddEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogPostModifyEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogPreModifyEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogRemoveEvent;
import org.geoserver.cloud.event.config.LocalConfigAddEvent;
import org.geoserver.cloud.event.config.LocalConfigPostModifyEvent;
import org.geoserver.cloud.event.config.LocalConfigPreModifyEvent;
import org.geoserver.cloud.event.config.LocalConfigRemoveEvent;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.ows.util.OwsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Adapts the listener pattern used by {@link Catalog#addListener Catalog} and {@link
 * GeoServer#addListener GeoServer} used to notify configuration events, as regular spring {@link
 * ApplicationEvent application events}, and publishes them to the local {@link ApplicationContext},
 * so other components interested in these kind of events don't need to register themselves to the
 * {@link Catalog} and {@link GeoServer} as listeners.
 *
 * <p>
 *
 * @see LocalInfoEvent LocalInfoEvent's class hierarchy
 */
public class LocalApplicationEventPublisher {

    @Setter(value = AccessLevel.PACKAGE)
    private @Autowired ApplicationEventPublisher localContextPublisher;

    @Setter(value = AccessLevel.PACKAGE)
    private @Autowired @Qualifier("catalog") Catalog catalog;

    @Setter(value = AccessLevel.PACKAGE)
    private @Autowired @Qualifier("geoServer") GeoServer geoServer;

    private LocalCatalogEventPublisher catalogListener;
    private LocalConfigEventPublisher configListener;

    public @PostConstruct void initialize() {
        catalogListener = new LocalCatalogEventPublisher(this, catalog);
        configListener = new LocalConfigEventPublisher(this, geoServer);

        catalog.addListener(catalogListener);
        geoServer.addListener(configListener);
    }

    public void publish(LocalInfoEvent<?, ?> event) {
        localContextPublisher.publishEvent(event);
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    static class LocalCatalogEventPublisher implements CatalogListener {

        private final LocalApplicationEventPublisher publisher;
        private final Catalog catalog;

        private void publish(LocalInfoEvent<?, ?> event) throws CatalogException {
            try {
                publisher.publish(event);
            } catch (RuntimeException e) {
                throw new CatalogException(e);
            }
        }

        public @Override void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            publish(LocalCatalogAddEvent.of(catalog, event));
        }

        public @Override void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            publish(LocalCatalogRemoveEvent.of(catalog, event));
        }

        public @Override void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            publish(LocalCatalogPreModifyEvent.of(catalog, event));
        }

        public @Override void handlePostModifyEvent(CatalogPostModifyEvent event)
                throws CatalogException {
            publish(LocalCatalogPostModifyEvent.of(catalog, event));
        }

        /**
         * {@inheritDoc}
         *
         * <p>no-op.
         */
        public @Override void reloaded() {}
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    static class LocalConfigEventPublisher extends ConfigurationListenerAdapter
            implements ConfigurationListener {

        private final LocalApplicationEventPublisher publisher;
        private final GeoServer geoServer;

        private static final ThreadLocal<Map<String, PropertyDiff>> PRE_CHANGE_DIFF =
                ThreadLocal.withInitial(HashMap::new);

        private void publishPreModify(
                @NonNull String id,
                Info info,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {

            PropertyDiff diff = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
            PRE_CHANGE_DIFF.get().put(id, diff);
            publisher.publish(LocalConfigPreModifyEvent.of(geoServer, info, diff));
        }

        private void publishPostModify(@NonNull String id, Info info) {
            Map<String, PropertyDiff> perObjectDiffs = PRE_CHANGE_DIFF.get();
            PropertyDiff diff = perObjectDiffs.remove(id);
            if (perObjectDiffs.isEmpty()) {
                PRE_CHANGE_DIFF.remove();
            }
            publisher.publish(LocalConfigPostModifyEvent.of(geoServer, info, diff));
        }

        public @Override void handleGlobalChange(
                GeoServerInfo global,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            String id = nonnNullIdendifier(global, "geoserver.global");
            publishPreModify(id, global, propertyNames, oldValues, newValues);
        }

        public @Override void handlePostGlobalChange(GeoServerInfo global) {
            String id = nonnNullIdendifier(global, "geoserver.global");
            publishPostModify(id, global);
        }

        public @Override void handleSettingsAdded(SettingsInfo settings) {
            publisher.publish(LocalConfigAddEvent.of(geoServer, settings));
        }

        public @Override void handleSettingsModified(
                SettingsInfo settings,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            if (settings.getId() == null) {
                // shouldn't happen, but can happen. GeoSeverImpl doesn't check for it
                OwsUtils.set(settings, "id", UUID.randomUUID().toString());
            }
            publishPreModify(settings.getId(), settings, propertyNames, oldValues, newValues);
        }

        public @Override void handleSettingsPostModified(SettingsInfo settings) {
            publishPostModify(settings.getId(), settings);
        }

        public @Override void handleSettingsRemoved(SettingsInfo settings) {
            publisher.publish(LocalConfigRemoveEvent.of(geoServer, settings));
        }

        public @Override void handleLoggingChange(
                LoggingInfo logging,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // LoggingInfo has no-id
            publishPreModify(
                    nonnNullIdendifier(logging, "logging"),
                    logging,
                    propertyNames,
                    oldValues,
                    newValues);
        }

        public @Override void handlePostLoggingChange(LoggingInfo logging) {
            // LoggingInfo has no-id
            publishPostModify(nonnNullIdendifier(logging, "logging"), logging);
        }

        public @Override void handleServiceChange(
                ServiceInfo service,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {

            publishPreModify(service.getId(), service, propertyNames, oldValues, newValues);
        }

        /**
         * Note {@link GeoServerImpl} sends a post-service change event (i.e. calls this method)
         * when a {@link ServiceInfo} has been added. There's no {@code handleServiceAdded} method
         * in {@link ConfigurationListener}. This method will identify that situation and fire a
         * {@link LocalConfigAddEvent} instead of a {@link LocalConfigPostModifyEvent}.
         */
        public @Override void handlePostServiceChange(ServiceInfo service) {
            Map<String, PropertyDiff> perObjectDiffs = PRE_CHANGE_DIFF.get();
            PropertyDiff diff = perObjectDiffs.remove(service.getId());
            if (diff == null) {
                // means there was no handleServiceChange() call and this is an add instead, shame's
                // on GeoServerImpl
                publisher.publish(LocalConfigAddEvent.of(geoServer, service));
            } else {
                publishPostModify(service.getId(), service);
            }
        }

        public @Override void handleServiceRemove(ServiceInfo service) {
            publisher.publish(LocalConfigRemoveEvent.of(geoServer, service));
        }

        private String nonnNullIdendifier(Info info, String defaultValue) {
            return info.getId() == null ? defaultValue : info.getId();
        }
    }
}
