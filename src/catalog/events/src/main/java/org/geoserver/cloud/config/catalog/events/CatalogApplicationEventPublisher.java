/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.events;

import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.catalog.CatalogInfoAddEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoModifyEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoPreModifyEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoveEvent;
import org.geoserver.cloud.event.config.ConfigInfoAddEvent;
import org.geoserver.cloud.event.config.ConfigInfoModifyEvent;
import org.geoserver.cloud.event.config.ConfigInfoPreModifyEvent;
import org.geoserver.cloud.event.config.ConfigInfoRemoveEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.ows.util.OwsUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

/**
 * Adapts the listener pattern used by {@link Catalog#addListener Catalog} and {@link
 * GeoServer#addListener GeoServer} used to notify configuration events, as regular spring {@link
 * ApplicationEvent application events}, and publishes them to the local {@link ApplicationContext},
 * so other components interested in these kind of events don't need to register themselves to the
 * {@link Catalog} and {@link GeoServer} as listeners.
 *
 * <p>
 *
 * @see InfoEvent LocalInfoEvent's class hierarchy
 */
@RequiredArgsConstructor
class CatalogApplicationEventPublisher {

    private final @NonNull Consumer<? super InfoEvent<?, ?>> eventPublisher;
    private final @NonNull Catalog catalog;
    private final @NonNull GeoServer geoServer;

    private LocalCatalogEventPublisher publishingCatalogListener;
    private LocalConfigEventPublisher publishingConfigListener;

    public @PostConstruct void initialize() {
        publishingCatalogListener = new LocalCatalogEventPublisher(this);
        publishingConfigListener = new LocalConfigEventPublisher(this);

        catalog.addListener(publishingCatalogListener);
        geoServer.addListener(publishingConfigListener);
    }

    void publish(@NonNull InfoEvent<?, ?> event) {
        eventPublisher.accept(event);
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    public static class LocalCatalogEventPublisher implements CatalogListener {

        private final CatalogApplicationEventPublisher publisher;

        /**
         * @throws CatalogException meaning the operation that generated the event should be
         *     reverted (as handled by Catalog.event())
         */
        private void publish(InfoEvent<?, ?> event) throws CatalogException {
            try {
                publisher.publish(event);
            } catch (RuntimeException e) {
                throw new CatalogException(e);
            }
        }

        public @Override void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            publish(CatalogInfoAddEvent.createLocal(event));
        }

        public @Override void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            publish(CatalogInfoRemoveEvent.createLocal(event));
        }

        public @Override void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            publish(CatalogInfoPreModifyEvent.createLocal(event));
        }

        public @Override void handlePostModifyEvent(CatalogPostModifyEvent event)
                throws CatalogException {
            publish(CatalogInfoModifyEvent.createLocal(event));
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
    public static class LocalConfigEventPublisher extends ConfigurationListenerAdapter
            implements ConfigurationListener {

        private final CatalogApplicationEventPublisher publisher;

        /**
         * A given object may get multiple pre-modify events, hence the stack by id. For instance,
         * {@code UpdateSequenceListener} changes {@code GeoServerInfo} while {@code
         * GeoServer.save(GeoServerInfo)} is being processed
         */
        private static final ThreadLocal<Map<String, LinkedList<PropertyDiff>>> PRE_CHANGE_DIFF =
                ThreadLocal.withInitial(HashMap::new);

        private void push(String objectId, PropertyDiff diff) {
            Map<String, LinkedList<PropertyDiff>> map = PRE_CHANGE_DIFF.get();
            map.computeIfAbsent(objectId, k -> new LinkedList<>()).addFirst(diff);
        }

        private @Nullable PropertyDiff pop(String objectId) {
            Map<String, LinkedList<PropertyDiff>> map = PRE_CHANGE_DIFF.get();
            LinkedList<PropertyDiff> stack = map.get(objectId);
            final PropertyDiff diff = stack == null || stack.isEmpty() ? null : stack.removeFirst();
            if (stack != null && stack.isEmpty()) {
                map.remove(objectId);
            }
            if (map.isEmpty()) {
                PRE_CHANGE_DIFF.remove();
            }
            return diff;
        }

        private void publishPreModify(
                @NonNull String id,
                Info info,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {

            PropertyDiff diff = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
            push(id, diff);
            publisher.publish(ConfigInfoPreModifyEvent.createLocal(info, diff));
        }

        private void publishPostModify(@NonNull String id, Info info) {
            PropertyDiff diff = pop(id);
            publisher.publish(ConfigInfoModifyEvent.createLocal(info, diff));
        }

        public @Override void handleGlobalChange(
                GeoServerInfo global,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            String id = InfoEvent.resolveId(global);
            publishPreModify(id, global, propertyNames, oldValues, newValues);
        }

        /**
         * Note: GeoServerImpl sends a post-modify event on setGlobal(), but no pre-event nor
         * add-event exists
         */
        public @Override void handlePostGlobalChange(GeoServerInfo global) {
            final String id = InfoEvent.resolveId(global);
            final PropertyDiff diff = pop(id);
            if (diff == null) {
                // means there was no handleServiceChange() call and this is an add instead, shame's
                // on GeoServerImpl
                publisher.publish(ConfigInfoAddEvent.createLocal(global));
            } else {
                // already called pop()
                publisher.publish(ConfigInfoModifyEvent.createLocal(global, diff));
            }
        }

        public @Override void handleSettingsAdded(SettingsInfo settings) {
            publisher.publish(ConfigInfoAddEvent.createLocal(settings));
        }

        public @Override void handleSettingsModified(
                SettingsInfo settings,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            if (settings.getId() == null) {
                // shouldn't happen, but can happen. GeoServerImpl doesn't check for it
                OwsUtils.set(settings, "id", UUID.randomUUID().toString());
            }
            publishPreModify(settings.getId(), settings, propertyNames, oldValues, newValues);
        }

        public @Override void handleSettingsPostModified(SettingsInfo settings) {
            publishPostModify(settings.getId(), settings);
        }

        public @Override void handleSettingsRemoved(SettingsInfo settings) {
            ConfigInfoRemoveEvent<?, SettingsInfo> event =
                    ConfigInfoRemoveEvent.createLocal(settings);
            publisher.publish(event);
        }

        public @Override void handleLoggingChange(
                LoggingInfo logging,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // LoggingInfo has no-id
            publishPreModify(
                    InfoEvent.resolveId(logging), logging, propertyNames, oldValues, newValues);
        }

        public @Override void handlePostLoggingChange(LoggingInfo logging) {
            // LoggingInfo has no-id
            final String id = InfoEvent.resolveId(logging);
            PropertyDiff diff = pop(id);
            if (diff == null) {
                // it was a GeoServer.setLogging instead...
                publisher.publish(ConfigInfoAddEvent.createLocal(logging));
            } else {
                // already called pop()
                publisher.publish(ConfigInfoModifyEvent.createLocal(logging, diff));
            }
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
         * {@link ConfigInfoAddEvent} instead of a {@link ConfigInfoModifyEvent}.
         */
        public @Override void handlePostServiceChange(ServiceInfo service) {
            PropertyDiff diff = pop(service.getId());
            if (diff == null) {
                // means there was no handleServiceChange() call and this is an add instead, shame's
                // on GeoServerImpl
                publisher.publish(ConfigInfoAddEvent.createLocal(service));
            } else {
                // already called pop()
                PropertyDiff clean = diff.clean();
                publisher.publish(ConfigInfoModifyEvent.createLocal(service, clean));
            }
        }

        public @Override void handleServiceRemove(ServiceInfo service) {
            ConfigInfoRemoveEvent<?, ServiceInfo> event =
                    ConfigInfoRemoveEvent.createLocal(service);
            publisher.publish(event);
        }
    }
}
