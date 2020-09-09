/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import org.springframework.context.event.EventListener;

/**
 * Marker interface in case listening for all catalog remote events is required (e.g. {@link
 * EventListener @EventListener(RemoteCatalogEvent.class) void
 * onAllRemoteCatalogEvents(RemoteCatalogEvent event)}
 */
public interface RemoteCatalogEvent {}
