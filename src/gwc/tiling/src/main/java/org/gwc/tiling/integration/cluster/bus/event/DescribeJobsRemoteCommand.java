/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster.bus.event;

import org.gwc.tiling.cluster.event.DescribeJobsCommand;
import org.springframework.cloud.bus.event.Destination;

/**
 * @since 1.0
 */
public class DescribeJobsRemoteCommand extends CacheJobRemoteEvent<DescribeJobsCommand> {

    private static final long serialVersionUID = 1L;

    protected DescribeJobsRemoteCommand() {
        super();
    }

    public DescribeJobsRemoteCommand(Object source, String originService, Destination destination) {
        super(source, originService, destination);
    }
}
