/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.gwc.tiling.event.JobAborted;
import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.model.CacheJobStatus.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class CacheJobManagerImpl implements CacheJobManager {

    private final @NonNull @Getter CacheJobRegistry registry;
    private final @NonNull Supplier<CacheJobRequestBuilder> requestBuilderFactory;

    private @Autowired ApplicationEventPublisher eventPublisher;

    private final ConcurrentMap<String, CacheJobWorker> workers = new ConcurrentHashMap<>();

    private CacheJobWorker workerFor(@NonNull CacheJobInfo job) {
        return workers.computeIfAbsent(job.getId(), id -> new CacheJobWorker(job, eventPublisher));
    }

    private Optional<CacheJobWorker> findWorker(@NonNull String jobId) {
        return Optional.ofNullable(workers.get(jobId));
    }

    protected String newJobId() {
        UUID id = UUID.randomUUID();
        return id.toString().replaceAll("-", "");
    }

    public @Override CacheJobRequestBuilder newRequestBuilder() {
        return requestBuilderFactory.get();
    }

    public @Override List<CacheJobInfo> getJobs() {
        return registry.getAll().stream().map(CacheJobStatus::getJobInfo).toList();
    }

    public @Override Optional<CacheJobStatus> getJobStatus(String jobId) {
        return registry.getStatus(jobId);
    }

    public @Override @NonNull CacheJobInfo launchJob(@NonNull CacheJobRequest request) {
        String id = newJobId();
        CacheJobInfo jobInfo = CacheJobInfo.builder().id(id).request(request).build();
        return launchJob(jobInfo);
    }

    public @NonNull CacheJobInfo launchJob(@NonNull CacheJobInfo job) {
        CacheJobStatus status = registry.add(job);
        workerFor(job).launch();
        return status.getJobInfo();
    }

    public @Override Optional<CacheJobStatus> abortJob(@NonNull String jobId) {
        Optional<CacheJobStatus> st =
                registry.setStatus(jobId, curr -> curr.isTerminal() ? curr : Status.ABORTING);

        st.filter(s -> !s.isFinished())
                .map(CacheJobStatus::jobId)
                .flatMap(this::findWorker)
                .ifPresent(CacheJobWorker::abort);

        return st;
    }

    protected @EventListener(JobAborted.class) void onJobAborted(JobAborted event) {
        registry.setStatus(event.getJob().getId(), Status.ABORTED);
    }

    public List<CacheJobStatus> abortAllJobs() {
        return registry.getAllAlive().stream()
                .map(CacheJobStatus::jobId)
                .map(this::abortJob)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public @Override List<CacheJobStatus> pruneJobs() {
        return registry.getAllTerminated().stream()
                .map(CacheJobStatus::getJobInfo)
                .map(registry::remove)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }
}
