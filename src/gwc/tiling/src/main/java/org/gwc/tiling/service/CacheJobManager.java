/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobStatus;

import java.util.List;
import java.util.Optional;

/**
 * Primary interface to interact with cache jobs, allows to launch, inspect, and manage (abort,
 * prune) tile cache jobs.
 *
 * @since 1.0
 */
public interface CacheJobManager {

    CacheJobRequestBuilder newRequestBuilder();

    List<CacheJobInfo> getJobs();

    Optional<CacheJobStatus> getJobStatus(String jobId);

    /**
     * Removes terminated (normally or abnormally) jobs and return their known status
     *
     * @throws IllegalStateException if not {@link #isRunning() running}
     */
    List<CacheJobStatus> pruneJobs();

    /**
     * @param request
     * @return
     * @throws IllegalStateException if not {@link #isRunning() running}
     */
    CacheJobInfo launchJob(CacheJobRequest request);

    /**
     * @param jobId
     * @return
     * @throws IllegalStateException if not {@link #isRunning() running}
     */
    Optional<CacheJobStatus> abortJob(String jobId);
}
