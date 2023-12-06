/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ApplicationEventCapturingListener {

    private List<Object> captured = new ArrayList<>();

    private boolean capturing = true;

    private Class<?> eventType = ApplicationEvent.class;

    public void setCapureEventsOf(Class<?> type) {
        this.eventType = type;
    }

    @EventListener
    public void capture(Object anyEvent) {
        if (capturing && this.eventType.isInstance(anyEvent)) {
            captured.add(anyEvent);
        }
    }

    public ApplicationEventCapturingListener stop() {
        capturing = false;
        return this;
    }

    public ApplicationEventCapturingListener start() {
        capturing = true;
        return this;
    }

    public ApplicationEventCapturingListener restart() {
        stop();
        clear();
        start();
        return this;
    }

    public <T> Optional<T> firstAndRemove(Class<T> type) {
        Optional<T> first = captured.stream().filter(type::isInstance).map(type::cast).findFirst();
        if (first.isPresent()) {
            assertTrue(captured.remove(first.get()));
        }
        return first;
    }

    public <T> T expectOne(Class<T> type) {
        List<T> list = allOf(type);
        assertEquals(1, list.size(), "exactly only one " + type.getSimpleName());
        return list.get(0);
    }

    public <T> List<T> allOf(Class<T> type) {
        return captured.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public <T> Optional<T> first(Class<T> type) {
        return captured.stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    public void clear() {
        captured.clear();
    }
}
