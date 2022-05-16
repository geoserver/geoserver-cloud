/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.model.CacheJobStatus.Status;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @since 1.0
 */
public class CacheJobRegistry {

    @Accessors(chain = true, fluent = true)
    private static class CacheJob {
        private @Getter @Setter CacheJobInfo info;
        private @Getter @Setter CacheJobStatus status;

        CacheJob(@NonNull CacheJobInfo jobInfo) {
            this.info = jobInfo;
            this.status = CacheJobStatus.valueOf(jobInfo);
        }

        CacheJob(@NonNull CacheJobStatus status) {
            this.info = status.getJobInfo();
            this.status = status;
        }
    }

    private ConcurrentMap<String, CacheJob> jobs = new ConcurrentHashMap<>();

    /**
     * @throws IllegalArgumentException if a job with the same {@link CacheJobInfo#getId() id}
     *     already exists
     */
    public CacheJobStatus add(@NonNull CacheJobInfo job) {
        CacheJob jobHolder = new CacheJob(job);
        CacheJob existing = jobs.putIfAbsent(job.getId(), jobHolder);
        if (null != existing) {
            throw new IllegalArgumentException(
                    String.format(
                            "Job %s already exists: %s", existing.info().getId(), existing.info()));
        }
        return jobHolder.status();
    }

    /** Adds or replaces the provided {@link CacheJobStatus} */
    public void save(@NonNull CacheJobStatus jobStatus) {
        jobs.put(jobStatus.jobId(), new CacheJob(jobStatus));
    }

    public Optional<CacheJobStatus> remove(@NonNull CacheJobInfo info) {
        return remove(info.getId());
    }

    public Optional<CacheJobStatus> remove(@NonNull String jobId) {
        return Optional.ofNullable(jobs.remove(jobId)).map(CacheJob::status);
    }

    public List<CacheJobStatus> getAll() {
        return jobs.values().stream().map(CacheJob::status).toList();
    }

    public List<CacheJobStatus> getAllAlive() {
        return jobs.values().stream().map(CacheJob::status).filter(st -> !st.isFinished()).toList();
    }

    public List<CacheJobStatus> getAllTerminated() {
        return jobs.values().stream().map(CacheJob::status).filter(st -> st.isFinished()).toList();
    }

    public Optional<CacheJobStatus> getStatus(String jobId) {
        return find(jobId).map(CacheJob::status);
    }

    private Optional<CacheJob> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Optional<CacheJobStatus> setStatus(@NonNull String jobId, @NonNull Status status) {
        return setStatus(jobId, curr -> status);
    }

    public Optional<CacheJobStatus> setStatus(
            @NonNull String jobId, @NonNull Function<Status, Status> newStatusFunction) {

        CacheJob updated =
                jobs.computeIfPresent(
                        jobId,
                        (id, job) -> {
                            CacheJobStatus currentStatus = job.status();
                            Status newState = newStatusFunction.apply(currentStatus.getStatus());
                            CacheJobStatus newStatus = currentStatus.withStatus(newState);
                            return job.status(newStatus);
                        });
        return Optional.ofNullable(updated).map(CacheJob::status);
    }
}
