/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster.bus.event;

import org.gwc.tiling.cluster.event.AbortJobComand;
import org.springframework.cloud.bus.event.Destination;

/**
 * @since 1.0
 */
public class AbortJobRemoteCommand extends CacheJobRemoteEvent<AbortJobComand> {

    private static final long serialVersionUID = 1L;

    protected AbortJobRemoteCommand() {
        super();
    }

    public AbortJobRemoteCommand(Object source, String originService, Destination destination) {
        super(source, originService, destination);
    }
}
