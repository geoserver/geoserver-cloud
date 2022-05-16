/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.gwc.tiling.cluster.event.AbortJobComand;
import org.gwc.tiling.cluster.event.CacheJobEvent;
import org.gwc.tiling.cluster.event.DescribeJobsCommand;
import org.gwc.tiling.cluster.event.DescribeJobsResponse;
import org.gwc.tiling.cluster.event.LaunchJobCommand;
import org.gwc.tiling.cluster.event.PruneJobsCommand;
import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.model.CacheJobStatus.Status;
import org.gwc.tiling.service.CacheJobManager;
import org.gwc.tiling.service.CacheJobManagerImpl;
import org.gwc.tiling.service.CacheJobRegistry;
import org.gwc.tiling.service.CacheJobRequestBuilder;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.gwc.tiling.cluster")
public class ClusteringCacheJobManager implements CacheJobManager {

    private final @NonNull Supplier<String> instanceId;
    private final @NonNull Consumer<? super CacheJobEvent> eventPublisher;
    private final @NonNull CacheJobManagerImpl localManager;
    private final @NonNull RemoteJobRegistry remotes;

    private final AtomicBoolean joinedCluster = new AtomicBoolean();

    public String instanceId() {
        return instanceId.get();
    }

    /**
     * @return {@code true} if {@link #joinCluster()} succeeded, which can be asynchronous
     */
    public boolean isRunning() {
        return joinedCluster.get();
    }

    /** {@inheritDoc} */
    public @Override CacheJobRequestBuilder newRequestBuilder() {
        return localManager.newRequestBuilder();
    }

    /** {@inheritDoc} */
    public @Override List<CacheJobInfo> getJobs() {
        return localManager.getJobs();
    }

    /** {@inheritDoc} */
    public @Override Optional<CacheJobStatus> getJobStatus(String jobId) {
        return localManager.getJobStatus(jobId);
    }

    /**
     * Marks this instance as ready to participate in the cluster, and hence to {@link #launchJob
     * launch}, {@link #cancelJob cancel}, and {@link #pruneJobs prune} jobs.
     *
     * <p>Broadcasts a {@link DescribeJobsCommand} for all other instances to send a {@link
     * DescribeJobsResponse} to this one.
     *
     * @see #handleRemoteInstanceStatusUpate(DescribeJobsResponse)
     */
    public void joinCluster() {
        if (joinedCluster.compareAndSet(false, true)) {
            publish(event(DescribeJobsCommand::new));
        }
    }

    /**
     * Makes this instance unable to process cache-mutating requests/events ({@link #isRunning()
     * isRunning() == false}), aborts all running jobs, and broadcasts a {@link
     * DescribeJobsResponse} to all other instances in the cluster, with this instance's jobs, which
     * should all be terminated, either due to the call to abort all running jobs, or because they
     * were already terminated on this instance.
     */
    public void leaveCluster() {
        if (joinedCluster.compareAndExchange(true, false)) {
            var pruned = doLeaveCluster();
            describeJobs(pruned);
        }
    }

    protected List<CacheJobStatus> doLeaveCluster() {
        final Duration timeout = Duration.ofSeconds(5);
        abortJobsWithTimeout(timeout);
        var pruned = localManager.pruneJobs();
        if (!pruned.isEmpty()) {
            debug("broadcasting {} terminated jobs", pruned.size());
        }
        return pruned;
    }

    protected void abortJobsWithTimeout(final Duration timeout) {
        var aborting = localManager.abortAllJobs();
        if (aborting.isEmpty()) return;

        debug("aborting all running jobs before leaving cluster...");

        aborting.stream()
                .map(status -> awaitTermination(status, timeout))
                .map(CompletableFuture::join)
                .forEach(
                        st -> {
                            if (st.getStatus() == Status.ABORTED)
                                info("aborted job {}, status is {}", st.jobId(), st.getStatus());
                            else
                                warn(
                                        "job {} couldn't be aborted within {}ms, status is {}",
                                        st.jobId(),
                                        timeout.toMillis(),
                                        st.getStatus());
                        });
    }

    private CompletableFuture<CacheJobStatus> awaitTermination(
            CacheJobStatus currentStatus, Duration timeout) {

        if (currentStatus.isFinished()) {
            return CompletableFuture.completedFuture(currentStatus);
        }

        final int maxAttempts = 10;
        final Duration retryDuration = timeout.dividedBy(maxAttempts);

        Stream<CacheJobStatus> map =
                IntStream.range(0, maxAttempts)
                        .mapToObj(attempt -> asyncStatus(currentStatus, retryDuration))
                        .map(CompletableFuture::join)
                        .filter(CacheJobStatus::isFinished);

        return CompletableFuture.supplyAsync(() -> map.findFirst().orElse(currentStatus));
    }

    private CompletableFuture<CacheJobStatus> asyncStatus(CacheJobStatus curr, Duration delay) {

        Executor delayedExecutor = CompletableFuture.delayedExecutor(delay.toNanos(), NANOSECONDS);
        Supplier<CacheJobStatus> supplier =
                () -> {
                    Optional<CacheJobStatus> status = localManager.getJobStatus(curr.jobId());
                    info("job {} status: {}", status.get().jobId(), status.get().getStatus());
                    return status.orElse(curr);
                };
        return CompletableFuture.supplyAsync(supplier, delayedExecutor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Launches a cache job for the provided {@code request} and broadcasts a {@link
     * LaunchJobCommand} event for other instances to follow suite.
     *
     * @see #handleRemoteLaunchJobRequest(LaunchJobCommand)
     */
    public @Override CacheJobInfo launchJob(CacheJobRequest request) {
        failIfNotRunning();
        CacheJobInfo job = localManager.launchJob(request);
        publish(event(LaunchJobCommand::new).withJob(job));
        return job;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Aborts the job with {@link CacheJobInfo#getId() id} {@code jobId} and broadcasts an {@link
     * AbortJobComand} event for other instances to follow suite, irrespectively of whether the job
     * was found and aborted in the local instance.
     *
     * @see #handleRemoteAbortJobRequest(AbortJobComand)
     */
    public @Override Optional<CacheJobStatus> abortJob(String jobId) {
        failIfNotRunning();
        Optional<CacheJobStatus> canceled = localManager.abortJob(jobId);
        publish(event(AbortJobComand::new).withJobId(jobId));
        return canceled;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Prunes local jobs and broadcasts a {@link PruneJobsCommand} event for other instances to
     * follow suite.
     *
     * @see #handleRemotePruneJobsRequest(PruneJobsCommand)
     */
    public @Override List<CacheJobStatus> pruneJobs() {
        failIfNotRunning();
        var pruned = localManager.pruneJobs();
        publish(event(PruneJobsCommand::new));
        return pruned;
    }

    @EventListener(LaunchJobCommand.class)
    public void handleRemoteLaunchJobRequest(LaunchJobCommand event) {
        if (shallConsume(event)) {
            CacheJobInfo job = localManager.launchJob(event.getJob());
            CacheJobStatus status = localManager.getJobStatus(job.getId()).orElseThrow();
            describeJobs(List.of(status));
        }
    }

    /**
     * If a remote instance asked us to tell it which jobs we have, reply with a {@link
     * DescribeJobsResponse}
     */
    @EventListener(DescribeJobsCommand.class)
    public void handleRemoteDescribeJobsRequest(DescribeJobsCommand event) {
        if (shallConsume(event)) {
            final String targetInstanceId = event.getInstanceId();
            final boolean includeTerminated = event.isIncludeTerminated();
            final CacheJobRegistry registry = localManager.getRegistry();
            var jobs = includeTerminated ? registry.getAll() : registry.getAllAlive();
            describeJobs(targetInstanceId, jobs);
        }
    }

    /**
     * If a remote instance sent us its list of jobs, usually in response to a {@link
     * DescribeJobsCommand} broadcasted by us; register them on the {@link RemoteJobRegistry}
     */
    @EventListener(DescribeJobsResponse.class)
    public void handleRemoteInstanceStatusUpate(DescribeJobsResponse event) {
        if (shallConsume(event)) {
            final List<CacheJobStatus> jobs = event.getJobs();
            final String remoteInstance = event.getInstanceId();
            debug("received {} jobs from {}", jobs.size(), remoteInstance);
            jobs.forEach(
                    jobStatus -> {
                        remotes.update(remoteInstance, jobStatus);
                        mergeLocal(jobStatus);
                    });
        }
    }

    @EventListener(PruneJobsCommand.class)
    public void handleRemotePruneJobsRequest(PruneJobsCommand event) {
        if (shallConsume(event)) {
            var pruned = localManager.pruneJobs();
            if (!pruned.isEmpty()) {
                describeJobs(pruned);
            }
        }
    }

    @EventListener(AbortJobComand.class)
    public void handleRemoteAbortJobRequest(AbortJobComand event) {
        if (shallConsume(event)) {
            Optional<CacheJobStatus> aborted = localManager.abortJob(event.getJobId());
            aborted.map(List::of).ifPresent(this::describeJobs);
        }
    }

    public boolean isFromSelf(@NonNull CacheJobEvent event) {
        String thisInstanceId = instanceId();
        String eventInstanceId = event.getInstanceId();
        return thisInstanceId.equals(eventInstanceId);
    }

    private boolean shallConsume(CacheJobEvent event) {
        return isRunning() && !isFromSelf(event);
    }

    /**
     * @param remoteJobStatus
     */
    private void mergeLocal(CacheJobStatus remoteJobStatus) {
        CacheJobInfo jobInfo = remoteJobStatus.getJobInfo();
        if (remoteJobStatus.isFinished()) {
            debug(
                    "remote job {} is {}, not launching local job",
                    jobInfo.getId(),
                    remoteJobStatus.getStatus().toString().toLowerCase());
            return;
        }
        Optional<CacheJobStatus> status = localManager.getJobStatus(jobInfo.getId());
        status.ifPresentOrElse(this::ignoreExistingJob, () -> launchRemoteJob(jobInfo));
    }

    protected void launchRemoteJob(CacheJobInfo jobInfo) {
        if (isRunning()) {
            info("launching local job {}, notified from another instance", jobInfo.getId());
            localManager.launchJob(jobInfo);
        } else {
            debug(
                    "job {} notified from another instance won't be launched, this instance is not running",
                    jobInfo.getId());
        }
    }

    private void ignoreExistingJob(CacheJobStatus existing) {
        debug(
                "job {} is already {} on this instance",
                existing.jobId(),
                existing.getStatus().toString().toLowerCase());
    }

    /**
     * Configures MDC with instance-id key = this.instanceId(), see logback-test.xml for an example
     * on how to configure every message to include it (i.e. adding {@literal %X{instance-id}} to
     * the pattern at the desired place)
     */
    private void trace(String msg, Object... args) {
        MDC.put("instance-id", instanceId());
        log.trace(msg, args);
        MDC.clear();
    }

    private void debug(String msg, Object... args) {
        MDC.put("instance-id", instanceId());
        log.debug(msg, args);
        MDC.clear();
    }

    private void info(String msg, Object... args) {
        MDC.put("instance-id", instanceId());
        log.info(msg, args);
        MDC.clear();
    }

    private void warn(String msg, Object... args) {
        MDC.put("instance-id", instanceId());
        log.warn(msg, args);
        MDC.clear();
    }

    private void failIfNotRunning() {
        if (!isRunning())
            throw new IllegalStateException(
                    "Cache job manager is not running. Call joinCluster() first");
    }

    public @Override String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), instanceId());
    }

    private void publish(CacheJobEvent event) {
        eventPublisher.accept(event);
    }

    private <L extends CacheJobEvent> L event(Function<String, L> factory) {
        return factory.apply(instanceId());
    }

    protected void describeJobs(List<CacheJobStatus> jobs) {
        String targetInstanceId = null; // broadcast to all
        describeJobs(targetInstanceId, jobs);
    }

    protected void describeJobs(final String targetInstanceId, List<CacheJobStatus> jobs) {
        final String instanceId = instanceId();
        publish(new DescribeJobsResponse(instanceId, targetInstanceId, jobs));
    }
}
