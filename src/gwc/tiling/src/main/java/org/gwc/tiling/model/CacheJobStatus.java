/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

/**
 * Mutable state tracker for a {@link CacheJobInfo job}
 *
 * @since 1.0
 */
@Value
@With
@Builder
@Jacksonized
public class CacheJobStatus {

    public static enum Status {
        SCHEDULED(false),
        RUNNING(false),
        COMPLETE(true),
        ABORTING(false),
        ABORTED(true),
        FAILED(true);

        private final @Getter boolean terminal;

        Status(boolean terminal) {
            this.terminal = terminal;
        }
    }

    private @NonNull CacheJobInfo jobInfo;

    private @NonNull Status status;

    private @NonNull CacheJobStatistics stats;

    public static CacheJobStatus valueOf(CacheJobInfo jobInfo) {
        return new CacheJobStatus(jobInfo, Status.SCHEDULED, CacheJobStatistics.newInstance());
    }

    @JsonIgnore
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    /**
     * @return {@code true} if {@link #getStatus()} is either {@link Status#COMPLETE complete} or
     *     {@link Status#ABORTED aborted}
     */
    @JsonIgnore
    public boolean isFinished() {
        return status.isTerminal();
    }

    public String jobId() {
        return getJobInfo().getId();
    }

    public CacheJobStatus merge(@NonNull CacheJobStatus other) {
        checkSameJobId(other);
        return this.withStats(getStats().merge(other.getStats()));
    }

    protected void checkSameJobId(CacheJobStatus status) {
        if (!jobId().equals(status.jobId()))
            throw new IllegalArgumentException(
                    "job ids differ. this: " + jobId() + ", instance: " + status.jobId());
    }
}
