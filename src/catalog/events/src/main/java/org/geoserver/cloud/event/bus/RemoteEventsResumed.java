/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.event.bus;

/**
 * Tells the listeners of this service instance that the events of the other instances reach it again: the consumer of
 * the event bus subscribed to its queue, at startup or after a lost connection.
 *
 * <p>The events sent while the consumer was away are lost. A listener holding state derived from the events, such as a
 * cache, empties or rebuilds it. This event is local: it is never sent over the bus.
 *
 * @param queue the queue of the consumer
 */
public record RemoteEventsResumed(String queue) {}
