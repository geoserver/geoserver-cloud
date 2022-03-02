/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.geoserver.cloud.bus.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.bus.GeoServerBusProperties;
import org.geoserver.cloud.bus.event.RemoteAddEvent;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.bus.event.RemoteRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigEvent;
import org.geoserver.cloud.test.TestConfigurationAutoConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = {TestConfigurationAutoConfiguration.class, BusEventCollector.class},
        properties = {
            "spring.cloud.bus.id=app:1",
            "spring.cloud.stream.bindings.springCloudBusOutput.producer.errorChannelEnabled=true",
            "spring.autoconfigure.exclude=org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration",
            "logging.level.root=WARN",
            "logging.level.org.springframework.cloud.bus.BusConsumer=INFO"
        })
@Testcontainers
public abstract class BusAmqpIntegrationTests {

    @Container
    private static final RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer("rabbitmq:3.9-management");

    private static ConfigurableApplicationContext remoteAppContext;
    private @Autowired ConfigurableApplicationContext localAppContext;

    protected @Autowired GeoServer geoserver;
    protected @Autowired Catalog catalog;
    protected @Autowired GeoServerBusProperties geoserverBusProperties;

    protected CatalogTestData testData;

    protected EventsCaptor eventsCaptor;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    }

    @BeforeAll
    static void setUpRemoteApplicationContext() {
        remoteAppContext =
                new SpringApplicationBuilder(
                                TestConfigurationAutoConfiguration.class, BusEventCollector.class)
                        .properties(
                                "server.port=0",
                                "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
                                "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort(),
                                "spring.cloud.bus.id=app:2",
                                "spring.autoconfigure.exclude=org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration")
                        .run();
    }

    @AfterAll
    static void afterAll() {
        if (remoteAppContext != null) {
            remoteAppContext.close();
        }
    }

    @BeforeEach
    final void before() {
        assertThat(rabbitMQContainer.isRunning());
        BusEventCollector localAppEvents = localAppContext.getBean(BusEventCollector.class);
        BusEventCollector remoteAppEvents = remoteAppContext.getBean(BusEventCollector.class);
        this.eventsCaptor =
                new EventsCaptor(localAppEvents, remoteAppEvents, geoserverBusProperties);

        // restore default settings
        disablePayload();

        eventsCaptor.stop().clear().capureEventsOf(RemoteInfoEvent.class);

        testData =
                CatalogTestData.empty(() -> catalog, () -> geoserver)
                        .createCatalogObjects()
                        .createConfigObjects();

        // Patch patch = new Patch();
        // patch.add("defaultWorkspace", null);
        // context.publishEvent(new RemoteDefaultWorkspaceEvent(catalog, patch, "this", null));

        // eventsCaptor.start();
    }

    @AfterEach
    void after() {
        eventsCaptor.stop();
        eventsCaptor.clear();
        testData.deleteAll();
    }

    protected void disablePayload() {
        geoserverBusProperties.setSendDiff(false);
        geoserverBusProperties.setSendObject(false);
    }

    protected void enablePayload(boolean enabled) {
        geoserverBusProperties.setSendDiff(enabled);
        geoserverBusProperties.setSendObject(enabled);
    }

    protected void enablePayload() {
        geoserverBusProperties.setSendDiff(true);
        geoserverBusProperties.setSendObject(true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T extends Info> void testRemoteRemoveEvent(
            T info, Consumer<T> remover, Class<? extends RemoteRemoveEvent> eventType) {

        this.eventsCaptor.clear();
        remover.accept(info);

        RemoteRemoveEvent<?, T> event = eventsCaptor.local().expectOne(eventType);
        assertRemoteEvent(info, event);

        // local-remote event ok, check the one sent over the wire

        RemoteRemoveEvent<?, T> parsedSentEvent = eventsCaptor.remote().expectOne(eventType);
        assertRemoteEvent(info, parsedSentEvent);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T extends Info> void testRemoteModifyEvent( //
            @NonNull T info, //
            @NonNull Consumer<T> modifier, //
            @NonNull Consumer<T> saver,
            @NonNull Class<? extends RemoteModifyEvent> eventType) {

        Class<T> type = resolveInfoInterface(info);
        T proxy = ModificationProxy.create(ModificationProxy.unwrap(info), type);
        modifier.accept(proxy);

        Patch expected = resolveExpectedDiff(proxy).clean().toPatch();

        this.eventsCaptor.clear();
        this.eventsCaptor.start();
        saver.accept(proxy);

        RemoteModifyEvent<?, T> localRemoteEvent = eventsCaptor.local().expectOne(eventType);
        assertRemoteEvent(info, localRemoteEvent);

        RemoteModifyEvent<?, T> sentRemoteEvent = eventsCaptor.remote().expectOne(eventType);
        assertRemoteEvent(info, sentRemoteEvent);

        if (this.geoserverBusProperties.isSendDiff()) {
            assertEquals(expected, localRemoteEvent.patch().get());
            assertEquals(expected, sentRemoteEvent.patch().get());
        }
    }

    protected <T extends Info> PropertyDiff resolveExpectedDiff(T proxy) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(proxy);
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();
        assertFalse(propertyNames.isEmpty(), "Test should change at least one property");

        PropertyDiff expected = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
        return expected;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Info> Class<T> resolveInfoInterface(T info) {
        Class<T> type;
        ClassMappings classMappings =
                ClassMappings.fromImpl(ModificationProxy.unwrap(info).getClass());
        if (classMappings != null) {
            type = (Class<T>) classMappings.getInterface();
        } else if (info instanceof GeoServerInfo) {
            type = (Class<T>) GeoServerInfo.class;
        } else if (info instanceof SettingsInfo) {
            type = (Class<T>) SettingsInfo.class;
        } else if (info instanceof LoggingInfo) {
            type = (Class<T>) LoggingInfo.class;
        } else {
            throw new IllegalArgumentException("Unknown Info type: " + info);
        }
        return type;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T extends Info> void testRemoteAddEvent(
            T info, Consumer<T> addOp, Class<? extends RemoteAddEvent> eventType) {

        this.eventsCaptor.stop().clear().capureEventsOf(eventType);
        eventsCaptor.start();
        addOp.accept(info);

        RemoteAddEvent<?, T> event = eventsCaptor.local().expectOne(eventType);
        assertRemoteEvent(info, event);

        // ok, that's the event published to the local application context, and which
        // spring-cloud-bus took care of not re-publishing. Let's capture the actual out-bound
        // message that traveled through the bus channel
        RemoteAddEvent<?, T> parsedSentEvent = eventsCaptor.remote().expectOne(eventType);
        assertRemoteEvent(info, parsedSentEvent);
    }

    protected <T extends Info> void assertRemoteEvent(T info, RemoteInfoEvent<?, T> event) {
        assertNotNull(event.getId());
        assertEquals("**", event.getDestinationService());
        assertNotNull(event.getObjectId());
        assertNotNull(event.getInfoType());
        switch (event.getInfoType()) {
            case Catalog:
                assertEquals(RemoteCatalogEvent.CATALOG_ID, event.getObjectId());
                break;
            case GeoServerInfo:
                assertEquals(RemoteConfigEvent.GEOSERVER_ID, event.getObjectId());
                break;
            case LoggingInfo:
                assertEquals(RemoteConfigEvent.LOGGING_ID, event.getObjectId());
                break;
            default:
                assertEquals(info.getId(), event.getObjectId());
                break;
        }
        assertTrue(event.getInfoType().getType().isInstance(info));

        if (event instanceof RemoteAddEvent) {
            RemoteAddEvent<?, T> e = (RemoteAddEvent<?, T>) event;
            if (geoserverBusProperties.isSendObject()) {
                assertTrue(e.object().isPresent());
                T object = e.object().get();
                assertEquals(info.getId(), object.getId());
                //                testData.assertEqualsLenientConnectionParameters(info, object);
            } else {
                assertFalse(e.object().isPresent());
            }
        }

        if (event instanceof RemoteRemoveEvent) {
            RemoteRemoveEvent<?, T> e = (RemoteRemoveEvent<?, T>) event;
            if (geoserverBusProperties.isSendObject()) {
                assertTrue(e.object().isPresent());
                T object = e.object().get();
                testData.assertEqualsLenientConnectionParameters(info, object);
            } else {
                assertFalse(e.object().isPresent());
            }
        }

        if (event instanceof RemoteModifyEvent) {
            RemoteModifyEvent<?, T> modifyEvent = (RemoteModifyEvent<?, T>) event;
            if (geoserverBusProperties.isSendDiff()) {
                assertTrue(modifyEvent.patch().isPresent());
                Patch diff = modifyEvent.patch().get();
                assertThat(diff.getPatches().size(), greaterThan(0));
            } else {
                assertFalse(modifyEvent.patch().isPresent());
            }
        }
    }

    @Accessors(fluent = true)
    @AllArgsConstructor
    protected static class EventsCaptor {
        final @Getter BusEventCollector local;
        final @Getter BusEventCollector remote;
        final GeoServerBusProperties geoserverBusProperties;

        public EventsCaptor capureEventsOf(
                @SuppressWarnings("rawtypes") Class<? extends RemoteInfoEvent> type) {
            local.capture(type);
            remote.capture(type);
            return this;
        }

        public EventsCaptor stop() {
            geoserverBusProperties.setSendEvents(false);
            remote.stop();
            local.stop();
            return this;
        }

        public EventsCaptor start() {
            remote.start();
            local.start();
            geoserverBusProperties.setSendEvents(true);
            return this;
        }

        public EventsCaptor clear() {
            remote.clear();
            local.clear();
            return this;
        }
    }
}
