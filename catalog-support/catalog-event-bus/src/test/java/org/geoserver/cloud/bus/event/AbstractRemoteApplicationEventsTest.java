/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.autoconfigure.bus.RemoteInfoEventInboundResolver;
import org.geoserver.cloud.bus.GeoServerBusProperties;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigEvent;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.bus.SpringCloudBusClient;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;

/**
 * Uses {@code spring-cloud-stream-test-support} (must be a test dependency) to collect the
 * out-bound messages with a {@link MessageCollector}, but from the {@link
 * SpringCloudBusClient#OUTPUT} channel.
 *
 * <p>See <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/_testing.html">spring
 * streams testing</a> docs for reference.
 */
public class AbstractRemoteApplicationEventsTest {

    /**
     * Spring-cloud-stream test message collector used to capture out-bound {@link RemoteInfoEvent}s
     */
    protected @Autowired MessageCollector messageCollector;

    /**
     * Spring-cloud-bus auto-configured message channels for spring-cloud-stream {@link
     * SpringCloudBusClient#springCloudBusOutput()}
     */
    protected @Autowired SpringCloudBusClient springCloudBusChannels;

    /**
     * Message converter registered by spring-cloud-bus, used to parse the json message sent to the
     * bus and captured by {@link #messageCollector}
     */
    protected @Autowired @Qualifier("busJsonConverter") AbstractMessageConverter busJsonConverter;

    protected @Autowired RemoteInfoEventInboundResolver inboundEventResolver;

    protected @Autowired GeoServer geoserver;
    protected @Autowired Catalog catalog;

    protected @Autowired ApplicationEventCapturingListener localRemoteEventsListener;

    protected @Autowired GeoServerBusProperties geoserverBusProperties;

    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog, () -> geoserver);

    protected BusChannelEventCollector outBoundEvents;

    public @Before void before() {
        // restore default settings
        disablePayload();
        localRemoteEventsListener.stop();
        localRemoteEventsListener.setCapureEventsOf(RemoteInfoEvent.class);
        testData.deleteAll();

        localRemoteEventsListener.clear();
        BlockingQueue<Message<?>> outChannel =
                messageCollector.forChannel(springCloudBusChannels.springCloudBusOutput());
        outBoundEvents =
                new BusChannelEventCollector(outChannel, busJsonConverter, inboundEventResolver);

        localRemoteEventsListener.start();
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

    protected <T extends Info> void testRemoteRemoveEvent(
            T info,
            Consumer<T> remover,
            @SuppressWarnings("rawtypes") Class<? extends RemoteRemoveEvent> eventType) {
        this.localRemoteEventsListener.clear();
        this.outBoundEvents.clear();
        remover.accept(info);
        @SuppressWarnings("unchecked")
        RemoteRemoveEvent<?, T> event = localRemoteEventsListener.expectOne(eventType);
        assertRemoteEvent(info, event);

        // local-remote event ok, check the one sent over the wire
        @SuppressWarnings("unchecked")
        RemoteRemoveEvent<?, T> parsedSentEvent = this.outBoundEvents.expectOne(eventType);
        assertRemoteEvent(info, parsedSentEvent);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Info> void testRemoteModifyEvent(
            @NonNull T info,
            @NonNull Consumer<T> modifier,
            @NonNull Consumer<T> saver,
            @SuppressWarnings("rawtypes") @NonNull Class<? extends RemoteModifyEvent> eventType) {

        Class<T> type = resolveInfoInterface(info);
        T proxy = ModificationProxy.create(ModificationProxy.unwrap(info), type);
        modifier.accept(proxy);

        Patch expected = resolveExpectedDiff(proxy).clean().toPatch();

        this.localRemoteEventsListener.clear();
        this.localRemoteEventsListener.start();
        this.outBoundEvents.clear();
        saver.accept(proxy);

        RemoteModifyEvent<?, T> localRemoteEvent = localRemoteEventsListener.expectOne(eventType);
        assertRemoteEvent(info, localRemoteEvent);

        RemoteModifyEvent<?, T> sentRemoteEvent = this.outBoundEvents.expectOne(eventType);
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
        assertFalse("Test should change at least one property", propertyNames.isEmpty());

        PropertyDiff expected = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
        return expected;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Info> Class<T> resolveInfoInterface(T info) {
        Class<T> type;
        ClassMappings classMappings =
                ClassMappings.fromImpl(ModificationProxy.unwrap(info).getClass());
        if (classMappings != null) {
            type = classMappings.getInterface();
        } else if (info instanceof GeoServerInfo) {
            type = (Class<T>) GeoServerInfo.class;
        } else if (info instanceof SettingsInfo) {
            type = (Class<T>) SettingsInfo.class;
        } else if (info instanceof LoggingInfo) {
            type = (Class<T>) LoggingInfo.class;
        } else {
            throw new IllegalArgumentException("uknown Info type: " + info);
        }
        return type;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T extends Info> void testRemoteAddEvent(
            T info, Consumer<T> addOp, Class<? extends RemoteAddEvent> eventType) {
        this.localRemoteEventsListener.clear();
        this.outBoundEvents.clear();
        addOp.accept(info);

        RemoteAddEvent<?, T> event = localRemoteEventsListener.expectOne(eventType);
        assertRemoteEvent(info, event);

        // ok, that's the event published to the local application context, and which
        // spring-cloud-bus took care of not re-publishing. Let's capture the actual out-bound
        // message from the bus channel
        RemoteAddEvent<?, T> parsedSentEvent = this.outBoundEvents.expectOne(eventType);
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
                testData.assertEqualsLenientConnectionParameters(info, object);
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
}
