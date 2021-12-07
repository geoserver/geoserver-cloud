/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.TaskScheduler;

/** */
public class TestBindingService extends BindingService {

    private Map<String, SubscribableChannel> channels = new HashMap<>();

    public TestBindingService(
            BindingServiceProperties bindingServiceProperties,
            BinderFactory binderFactory,
            TaskScheduler taskScheduler,
            ObjectMapper objectMapper) {
        super(bindingServiceProperties, binderFactory, taskScheduler, objectMapper);
    }

    @Override
    public <T> Binding<T> bindProducer(T output, String outputName, boolean cache) {
        Binding<T> ret = super.bindProducer(output, outputName, cache);
        if (output instanceof SubscribableChannel) {
            channels.put(outputName, (SubscribableChannel) output);
        }
        return ret;
    }

    public Optional<SubscribableChannel> getChannel(String name) {
        return Optional.ofNullable(channels.get(name));
    }
}
