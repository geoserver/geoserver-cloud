/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster.bus.event;

import org.gwc.tiling.cluster.event.LaunchJobCommand;
import org.springframework.cloud.bus.event.Destination;

/**
 * @since 1.0
 */
public class LaunchJobRemoteCommand extends CacheJobRemoteEvent<LaunchJobCommand> {

    private static final long serialVersionUID = 1L;

    protected LaunchJobRemoteCommand() {
        super();
    }

    public LaunchJobRemoteCommand(Object source, String originService, Destination destination) {
        super(source, originService, destination);
    }
}
