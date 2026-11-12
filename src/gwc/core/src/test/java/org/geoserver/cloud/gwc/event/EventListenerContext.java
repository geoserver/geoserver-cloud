/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.event;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.event.DefaultEventListenerFactory;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Builds an application context calling the {@code @EventListener} methods of objects, as the context of a service
 * does. The objects are registered as they are: the context runs no injection and no initialization on them. Listeners
 * of the same order run in the order of their objects.
 */
public final class EventListenerContext {

    private EventListenerContext() {
        // static methods only
    }

    /** Returns a refreshed context calling the event listeners of the objects, given in registration order. */
    public static GenericApplicationContext of(Object... listeners) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(
                AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME, EventListenerMethodProcessor.class);
        context.registerBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME, DefaultEventListenerFactory.class);
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        for (int i = 0; i < listeners.length; i++) {
            beanFactory.registerSingleton("listener" + i, listeners[i]);
        }
        context.refresh();
        return context;
    }
}
