/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster.bus;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.cluster.event.AbortJobComand;
import org.gwc.tiling.cluster.event.CacheJobEvent;
import org.gwc.tiling.cluster.event.DescribeJobsCommand;
import org.gwc.tiling.cluster.event.DescribeJobsResponse;
import org.gwc.tiling.cluster.event.LaunchJobCommand;
import org.gwc.tiling.integration.cluster.bus.event.AbortJobRemoteCommand;
import org.gwc.tiling.integration.cluster.bus.event.CacheJobRemoteEvent;
import org.gwc.tiling.integration.cluster.bus.event.DescribeJobsRemoteCommand;
import org.gwc.tiling.integration.cluster.bus.event.DescribeJobsRemoteResponse;
import org.gwc.tiling.integration.cluster.bus.event.LaunchJobRemoteCommand;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class RemoteCacheJobEventsBridge {

    private final @NonNull ApplicationEventPublisher publisher;
    private final @NonNull ClusteringCacheJobManager jobManager;
    private final @NonNull Function<String, Destination> destinationFactory;

    @EventListener(LaunchJobCommand.class)
    public LaunchJobRemoteCommand launchJobCommandBroadcast(LaunchJobCommand localEvent) {
        return asRemote(localEvent, LaunchJobRemoteCommand.class);
    }

    @EventListener(LaunchJobRemoteCommand.class)
    public LaunchJobCommand launchJobCommandIngest(LaunchJobRemoteCommand remoteEvent) {
        return asLocal(remoteEvent);
    }

    @EventListener(AbortJobComand.class)
    public AbortJobRemoteCommand abortJobCommandBroadcast(AbortJobComand localEvent) {
        return asRemote(localEvent, AbortJobRemoteCommand.class);
    }

    @EventListener(AbortJobRemoteCommand.class)
    public AbortJobComand abortJobCommandIngest(AbortJobRemoteCommand remoteEvent) {
        return asLocal(remoteEvent);
    }

    @EventListener(DescribeJobsCommand.class)
    public DescribeJobsRemoteCommand describeJobsCommandBroadcast(DescribeJobsCommand localEvent) {
        return asRemote(localEvent, DescribeJobsRemoteCommand.class);
    }

    @EventListener(DescribeJobsRemoteCommand.class)
    public DescribeJobsCommand describeJobsCommandIngest(DescribeJobsRemoteCommand remoteEvent) {
        return asLocal(remoteEvent);
    }

    @EventListener(DescribeJobsResponse.class)
    public DescribeJobsRemoteResponse describeJobsResponseBroadcast(
            DescribeJobsResponse localEvent) {
        return asRemote(localEvent, DescribeJobsRemoteResponse.class);
    }

    @EventListener(DescribeJobsRemoteResponse.class)
    public DescribeJobsResponse describeJobsResponseIngest(DescribeJobsRemoteResponse remoteEvent) {
        return asLocal(remoteEvent);
    }

    private <L extends CacheJobEvent, R extends CacheJobRemoteEvent<L>> L asLocal(R remote) {
        return isFromSelf(remote) ? null : remote.getEvent();
    }

    private <E extends CacheJobEvent, R extends CacheJobRemoteEvent<E>> R asRemote(
            E event, Class<R> remoteEventType) {

        if (isFromSelf(event)) {
            String instanceId = serviceId();
            Destination destination = destination(event);
            return factory(remoteEventType, instanceId, destination).get().setEvent(event);
        }
        return null;
    }

    private <R extends CacheJobRemoteEvent<?>> Supplier<R> factory(
            Class<R> remoteEventType, String instanceId, Destination destination) {

        return () -> {
            try {
                return remoteEventType
                        .getDeclaredConstructor(Object.class, String.class, Destination.class)
                        .newInstance(this, instanceId, destination);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private @NonNull String serviceId() {
        return jobManager.instanceId();
    }

    private boolean isFromSelf(CacheJobEvent localEvent) {
        return jobManager.isFromSelf(localEvent);
    }

    private boolean isFromSelf(CacheJobRemoteEvent<?> remoteEvent) {
        return isFromSelf(remoteEvent.getEvent());
    }

    private Destination destination(CacheJobEvent localEvent) {
        String targetService = null;
        if (localEvent instanceof DescribeJobsResponse) {
            var djr = (DescribeJobsResponse) localEvent;
            targetService = djr.getTargetInstanceId();
        }
        return destinationFactory.apply(targetService);
    }
}
