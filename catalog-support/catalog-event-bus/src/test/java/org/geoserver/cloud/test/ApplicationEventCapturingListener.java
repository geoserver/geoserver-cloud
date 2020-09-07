/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEventCapturingListener {

    private List<ApplicationEvent> captured = new ArrayList<>();

    private boolean capturing = true;

    private Class<? extends ApplicationEvent> eventType = ApplicationEvent.class;

    public void setCapureEventsOf(Class<? extends ApplicationEvent> type) {
        this.eventType = type;
    }

    @EventListener(ApplicationEvent.class)
    public void capture(ApplicationEvent anyEvent) {
        if (capturing && this.eventType.isInstance(anyEvent)) captured.add(anyEvent);
    }

    public ApplicationEventCapturingListener stop() {
        capturing = false;
        return this;
    }

    public ApplicationEventCapturingListener start() {
        capturing = true;
        return this;
    }

    public <T extends ApplicationEvent> Optional<T> firstAndRemove(Class<T> type) {
        Optional<T> first = captured.stream().filter(type::isInstance).map(type::cast).findFirst();
        if (first.isPresent()) {
            assertTrue(captured.remove(first.get()));
        }
        return first;
    }

    public <T extends ApplicationEvent> T expectOne(Class<T> type) {
        List<T> list = allOf(type);
        assertEquals("exactly only one " + type.getSimpleName(), 1, list.size());
        return list.get(0);
    }

    public <T extends ApplicationEvent> List<T> allOf(Class<T> type) {
        return captured.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public <T extends ApplicationEvent> Optional<T> first(Class<T> type) {
        return captured.stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    public void clear() {
        captured.clear();
    }
}
