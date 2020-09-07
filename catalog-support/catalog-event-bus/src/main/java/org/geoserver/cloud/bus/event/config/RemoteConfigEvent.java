/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import org.springframework.context.event.EventListener;

/**
 * Marker interface in case listening for all config remote events is required (e.g. {@link
 * EventListener @EventListener(RemoteConfigEvent.class) void
 * onAllRemoteCatalogEvents(RemoteConfigEvent event)}
 */
public interface RemoteConfigEvent {}
