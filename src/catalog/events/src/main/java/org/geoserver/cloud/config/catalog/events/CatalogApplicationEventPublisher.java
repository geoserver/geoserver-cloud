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
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.config.ConfigInfoAdded;
import org.geoserver.cloud.event.config.ConfigInfoModified;
import org.geoserver.cloud.event.config.ConfigInfoRemoved;
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
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private final @NonNull Consumer<? super InfoEvent> eventPublisher;
    private final @NonNull Catalog catalog;
    private final @NonNull GeoServer geoServer;
    private final @NonNull Supplier<Long> updateSequenceIncrementor;

    private LocalCatalogEventPublisher publishingCatalogListener;
    private LocalConfigEventPublisher publishingConfigListener;

    public @PostConstruct void initialize() {
        publishingCatalogListener = new LocalCatalogEventPublisher(this);
        publishingConfigListener = new LocalConfigEventPublisher(this);

        catalog.addListener(publishingCatalogListener);
        geoServer.addListener(publishingConfigListener);
    }

    void publish(@NonNull InfoEvent event) {
        eventPublisher.accept(event);
    }

    @NonNull
    Long incrementSequence() {
        return this.updateSequenceIncrementor.get();
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    public static class LocalCatalogEventPublisher implements CatalogListener {

        private final @NonNull CatalogApplicationEventPublisher publisher;

        private @NonNull Long incrementSequence() {
            return publisher.incrementSequence();
        }

        /**
         * @throws CatalogException meaning the operation that generated the event should be
         *     reverted (as handled by Catalog.event())
         */
        private void publish(InfoEvent event) throws CatalogException {
            try {
                publisher.publish(event);
            } catch (RuntimeException e) {
                throw new CatalogException(e);
            }
        }

        @Override
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            publish(CatalogInfoAdded.createLocal(incrementSequence(), event));
        }

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            publish(CatalogInfoRemoved.createLocal(incrementSequence(), event.getSource()));
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            // no-op
        }

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
            publish(CatalogInfoModified.createLocal(incrementSequence(), event));
        }

        @Override
        public void reloaded() {
            // no-op
        }
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    public static class LocalConfigEventPublisher extends ConfigurationListenerAdapter
            implements ConfigurationListener {

        private final @NonNull CatalogApplicationEventPublisher publisher;

        /**
         * A given object may get multiple pre-modify events, hence the stack by id. For instance,
         * {@code UpdateSequenceListener} changes {@code GeoServerInfo} while {@code
         * GeoServer.save(GeoServerInfo)} is being processed. Note {@code UpdateSequenceListener} is
         * now unused in gs-cloud, replaced by {@link UpdateSequence}, and hence shall not be loaded
         * as a bean at all.
         */
        private static final ThreadLocal<Map<String, LinkedList<Patch>>> PRE_CHANGE_DIFF =
                ThreadLocal.withInitial(HashMap::new);

        private @NonNull Long incrementSequence() {
            return publisher.incrementSequence();
        }

        private void push(String objectId, Patch patch) {
            Map<String, LinkedList<Patch>> map = PRE_CHANGE_DIFF.get();
            map.computeIfAbsent(objectId, k -> new LinkedList<>()).addFirst(patch);
        }

        private @Nullable Patch pop(String objectId) {
            Map<String, LinkedList<Patch>> map = PRE_CHANGE_DIFF.get();
            LinkedList<Patch> stack = map.get(objectId);
            final Patch patch = stack == null || stack.isEmpty() ? null : stack.removeFirst();
            if (stack != null && stack.isEmpty()) {
                map.remove(objectId);
            }
            if (map.isEmpty()) {
                PRE_CHANGE_DIFF.remove();
            }
            return patch;
        }

        private void publish(InfoEvent event) {
            publisher.publish(event);
        }

        private void preparePreModify(
                @NonNull String id,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {

            PropertyDiff diff = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
            Patch patch = diff.clean().toPatch();
            push(id, patch);
        }

        private void publishPostModify(@NonNull String id, @NonNull Info info) {
            Patch patch = Objects.requireNonNull(pop(id));
            publish(ConfigInfoModified.createLocal(incrementSequence(), info, patch));
        }

        @Override
        public void handleGlobalChange(
                GeoServerInfo global,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            String id = InfoEvent.resolveId(global);
            preparePreModify(id, propertyNames, oldValues, newValues);
        }

        /**
         * Note: GeoServerImpl sends a post-modify event on setGlobal(), but no pre-event nor
         * add-event exists
         */
        @Override
        public void handlePostGlobalChange(GeoServerInfo global) {
            final String id = InfoEvent.resolveId(global);
            final Patch patch = pop(id);
            if (patch == null) {
                // means there was no handleServiceChange() call and this is an add instead, shame's
                // on GeoServerImpl
                publish(ConfigInfoAdded.createLocal(incrementSequence(), global));
            } else {
                // already called pop()
                ConfigInfoModified event =
                        ConfigInfoModified.createLocal(incrementSequence(), global, patch);
                publish(event);
            }
        }

        @Override
        public void handleSettingsAdded(SettingsInfo settings) {
            publish(ConfigInfoAdded.createLocal(incrementSequence(), settings));
        }

        @Override
        public void handleSettingsModified(
                SettingsInfo settings,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            if (settings.getId() == null) {
                // shouldn't happen, but can happen. GeoServerImpl doesn't check for it
                OwsUtils.set(settings, "id", UUID.randomUUID().toString());
            }

            preparePreModify(settings.getId(), propertyNames, oldValues, newValues);
        }

        @Override
        public void handleSettingsPostModified(SettingsInfo settings) {
            publishPostModify(settings.getId(), settings);
        }

        @Override
        public void handleSettingsRemoved(SettingsInfo settings) {
            publish(ConfigInfoRemoved.createLocal(incrementSequence(), settings));
        }

        @Override
        public void handleLoggingChange(
                LoggingInfo logging,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // LoggingInfo has no-id
            preparePreModify(InfoEvent.resolveId(logging), propertyNames, oldValues, newValues);
        }

        @Override
        public void handlePostLoggingChange(LoggingInfo logging) {
            // LoggingInfo has no-id
            final String id = InfoEvent.resolveId(logging);
            Patch patch = pop(id);
            if (patch == null) {
                // it was a GeoServer.setLogging instead...
                publish(ConfigInfoAdded.createLocal(incrementSequence(), logging));
            } else {
                // already called pop()
                publish(ConfigInfoModified.createLocal(incrementSequence(), logging, patch));
            }
        }

        @Override
        public void handleServiceChange(
                ServiceInfo service,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {

            preparePreModify(service.getId(), propertyNames, oldValues, newValues);
        }

        /**
         * Note {@link GeoServerImpl} sends a post-service change event (i.e. calls this method)
         * when a {@link ServiceInfo} has been added. There's no {@code handleServiceAdded} method
         * in {@link ConfigurationListener}. This method will identify that situation and fire a
         * {@link ConfigInfoAdded} instead of a {@link ConfigInfoModified}.
         */
        @Override
        public void handlePostServiceChange(ServiceInfo service) {
            Patch patch = pop(service.getId());
            if (patch == null) {
                // means there was no handleServiceChange() call and this is an add instead, shame's
                // on GeoServerImpl
                publish(ConfigInfoAdded.createLocal(incrementSequence(), service));
            } else {
                // already called pop()
                publish(ConfigInfoModified.createLocal(incrementSequence(), service, patch));
            }
        }

        @Override
        public void handleServiceRemove(ServiceInfo service) {
            publish(ConfigInfoRemoved.createLocal(incrementSequence(), service));
        }
    }
}
