/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.RemoteEventsResumed;
import org.geoserver.cloud.security.secrets.StoreSecretsCipher;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Catalog and config events integration with spring cloud bus */
@Configuration(proxyBeanMethods = false)
@RemoteApplicationEventScan(basePackageClasses = {RemoteGeoServerEvent.class})
@Slf4j(topic = "org.geoserver.cloud.event.bus")
public class RemoteGeoServerEventsConfiguration {

    /**
     * Add a {@link GeoToolsFilterModule} to the default jackson spring codecs if not already present, so
     * {@link Expression} and {@link Filter} objects can be used as {@link RemoteGeoServerEvent} payload, especially
     * {@link Literal} expressions.
     */
    @Bean
    @ConditionalOnMissingBean(GeoToolsFilterModule.class)
    GeoToolsFilterModule geoToolsFilterModule() {
        return new GeoToolsFilterModule();
    }

    /**
     * Add a {@link GeoServerCatalogModule} to the default jackson spring codecs if not already present, so
     * {@link CatalogInfo} objects can be used as {@link RemoteGeoServerEvent} payload
     */
    @Bean
    @ConditionalOnMissingBean(GeoServerCatalogModule.class)
    GeoServerCatalogModule geoServerCatalogJacksonModule() {
        return new GeoServerCatalogModule();
    }

    /**
     * Add a {@link GeoServerConfigModule} to the default jackson spring codecs if not already present, so configuration
     * {@link Info} objects can be used as {@link RemoteGeoServerEvent} payload
     */
    @Bean
    @ConditionalOnMissingBean(GeoServerConfigModule.class)
    GeoServerConfigModule geoServerConfigJacksonModule() {
        return new GeoServerConfigModule();
    }

    /**
     * Highest priority listener for incoming {@link RemoteGeoServerEvent} events to resolve the payload
     * {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy} proxies, or {@code null} in
     * case of collection properties.
     *
     * <p>This listener ensures the payload object properties are resolved before being catch up by other listeners.
     */
    @Bean
    InfoEventResolver remoteInfoEventInboundResolver(@Qualifier("rawCatalog") Catalog rawCatalog, GeoServer geoserver) {
        return new InfoEventResolver(rawCatalog, geoserver);
    }

    /**
     * Encrypts the store secrets of the events sent, and decrypts them in the events received. The events are copied
     * with the JSON mapper of the application, used by the event bus to send them.
     */
    @Bean
    RemoteEventSecrets remoteEventSecrets(
            ObjectProvider<StoreSecretsCipher> cipher,
            ObjectProvider<GeoServerSecurityManager> securityManager,
            @Qualifier("rawCatalog") Catalog rawCatalog,
            ObjectProvider<JsonMapper> jsonMapper) {

        StoreSecretsCipher storeSecretsCipher = cipher.getIfAvailable(() -> StoreSecretsCipher.of(securityManager));
        ObjectMapper mapper = jsonMapper.getIfAvailable();
        if (null == mapper) {
            mapper = ObjectMapperUtil.newObjectMapper();
        }
        return new RemoteEventSecrets(storeSecretsCipher, rawCatalog, mapper);
    }

    @Bean
    RemoteGeoServerEventMapper remoteGeoServerEventMapper(
            InfoEventResolver remoteEventPropertiesResolver,
            ServiceMatcher serviceMatcher,
            Destination.Factory destinationFactory,
            RemoteEventSecrets remoteEventSecrets) {

        return new RemoteGeoServerEventMapper(
                remoteEventPropertiesResolver, serviceMatcher, destinationFactory, remoteEventSecrets);
    }

    @Bean
    RemoteGeoServerEventBridge remoteEventBroadcaster(
            ApplicationEventPublisher eventPublisher, RemoteGeoServerEventMapper eventMapper) {

        log.info("Configuring GeoServer Catalog distributed events.");

        Consumer<GeoServerEvent> localEventPublisher = eventPublisher::publishEvent;
        Consumer<RemoteGeoServerEvent> remoteEventPublisher = eventPublisher::publishEvent;
        return new RemoteGeoServerEventBridge(localEventPublisher, remoteEventPublisher, eventMapper);
    }

    /** Publishes {@link RemoteEventsResumed} when the bus runs over RabbitMQ. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.listener.ConsumeOkEvent")
    static class ResumedEventsConfiguration {

        @Bean
        RemoteEventsResumedPublisher remoteEventsResumedPublisher(ApplicationEventPublisher eventPublisher) {
            return new RemoteEventsResumedPublisher(eventPublisher);
        }
    }
}
