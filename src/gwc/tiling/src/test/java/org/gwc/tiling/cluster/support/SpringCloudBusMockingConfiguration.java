/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.cluster.support.DistributedContextSupport.SharedConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import(MockSpringCloudBusConfiguration.class)
@Slf4j(topic = "org.gwc.tiling.cluster.support")
public class SpringCloudBusMockingConfiguration {

    @Bean
    MockSpringCloudBusApplicationEventsBroadcaster
            mockSpringCloudBusApplicationEventsBroadcaster() {
        return new MockSpringCloudBusApplicationEventsBroadcaster();
    }

    @Bean
    ObjectMapper messageCodec() {
        var messageCodec = new ObjectMapper();
        messageCodec.registerModules(new JavaTimeModule(), new Jdk8Module());
        return messageCodec;
    }

    static class MockSpringCloudBusApplicationEventsBroadcaster {

        private @Autowired ClusteringCacheJobManager instance;
        private @Autowired SharedConfig sharedConfig;
        private @Autowired ServiceMatcher serviceMatcher;
        private @Autowired ObjectMapper messageCodec;

        @EventListener(RemoteApplicationEvent.class)
        void mockBusEventMulticast(RemoteApplicationEvent event) {

            final boolean isTransientSource = Object.class.equals(event.getSource().getClass());
            if (isTransientSource || !serviceMatcher.isFromSelf(event)) {
                return;
            }

            List<TestInstance> otherInstances = getOtherInstances();
            Collections.shuffle(otherInstances);
            otherInstances.forEach(instance -> publishOn(instance, event));
        }

        private void publishOn(TestInstance remote, RemoteApplicationEvent event) {
            final ApplicationContext remoteContext = remote.getContext();
            final ServiceMatcher instanceMatcher = remoteContext.getBean(ServiceMatcher.class);

            if (instanceMatcher.isForSelf(event)) {
                CompletableFuture.runAsync(
                        () -> {
                            final RemoteApplicationEvent incomingEvent = simulateWire(event);
                            log.trace(
                                    "Publishing event on {}: {}",
                                    remote.instanceId(),
                                    incomingEvent);
                            remote.getContext().publishEvent(incomingEvent);
                        });
            }
        }

        private RemoteApplicationEvent simulateWire(RemoteApplicationEvent evt) {
            try {
                String serialized = messageCodec.writeValueAsString(evt);
                RemoteApplicationEvent deser =
                        messageCodec.readerFor(evt.getClass()).readValue(serialized);
                return deser;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }

        protected List<TestInstance> getTestInstances() {
            return sharedConfig.getInstances();
        }

        protected ArrayList<TestInstance> getOtherInstances() {
            final String thisInstanceId = instance.instanceId();
            return getTestInstances().stream()
                    .filter(testInstance -> !thisInstanceId.equals(testInstance.instanceId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
