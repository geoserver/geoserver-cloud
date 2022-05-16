/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.gwc.tiling.model.CacheJobStatus.Status.ABORTED;
import static org.gwc.tiling.model.CacheJobStatus.Status.COMPLETE;
import static org.gwc.tiling.model.CacheJobStatus.Status.FAILED;
import static org.gwc.tiling.model.CacheJobStatus.Status.RUNNING;
import static org.gwc.tiling.model.CacheJobStatus.Status.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.model.CacheJobStatus.Status;
import org.gwc.tiling.service.CacheJobManager;
import org.gwc.tiling.service.CacheJobRegistry;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class TestInstance {

    private static final Duration MIN_WAIT_FOR_STATUS = Duration.ofMillis(100);
    private static final Duration MAX_WAIT_FOR_STATUS = Duration.ofSeconds(1);

    private final @NonNull @Getter ConfigurableApplicationContext context;
    private final @NonNull @Getter ClusteringCacheJobManager jobManager;
    private final @NonNull @Getter CacheJobRegistry jobRegistry;

    public String instanceId() {
        return jobManager.instanceId();
    }

    public CacheJobInfo launchAndGet(CacheJobRequest request) {
        ClusteringCacheJobManager manager = getJobManager();
        CacheJobInfo job = manager.launchJob(request);
        awaitForNewJob(job);
        return job;
    }

    public void awaitForNewJobs(@NonNull CacheJobInfo... jobs) {
        for (CacheJobInfo job : jobs) awaitForNewJob(job);
    }

    public CacheJobStatus awaitForNewJob(@NonNull CacheJobInfo job) {
        awaitForStatus(job, SCHEDULED, RUNNING);
        CacheJobStatus status = getJobManager().getJobStatus(job.getId()).orElseThrow();
        assertEquals(
                job.getId(),
                status.getJobInfo().getId(),
                () -> String.format("Job info id mismatch on %s ", getJobManager().instanceId()));
        assertEquals(
                job,
                status.getJobInfo(),
                () -> String.format("Job info mismatch on %s ", getJobManager().instanceId()));
        return status;
    }

    public void awaitForStatus(@NonNull CacheJobInfo job, Status... validStauses) {
        ClusteringCacheJobManager manager = getJobManager();
        String jobId = job.getId();
        await().atMost(MAX_WAIT_FOR_STATUS)
                .untilAsserted(
                        () ->
                                assertThat(manager.getJobStatus(jobId))
                                        .isPresent()
                                        .map(CacheJobStatus::getStatus)
                                        .get()
                                        .isIn(Set.of(validStauses)));
    }

    public CacheJobStatus getStatus(@NonNull CacheJobInfo job) {
        return getStatus(job.getId());
    }

    public CacheJobStatus getStatus(String jobId) {
        ClusteringCacheJobManager manager = getJobManager();
        return await().atMost(MAX_WAIT_FOR_STATUS)
                .alias(String.format("manager: %s, job: %s", manager.instanceId(), jobId))
                .until(() -> manager.getJobStatus(jobId), Optional::isPresent)
                .orElseThrow();
    }

    /**
     * Forces {@code job} to be {@link Status#COMPLETE complete} on the given job managers for the
     * sake for simulating a test scenario
     *
     * @return
     */
    public TestInstance forceComplete(CacheJobInfo... jobs) {
        return forceStatus(COMPLETE, jobs);
    }

    public TestInstance forceRunning(CacheJobInfo... jobs) {
        return forceStatus(RUNNING, jobs);
    }

    public TestInstance forceScheduled(CacheJobInfo... jobs) {
        return forceStatus(SCHEDULED, jobs);
    }

    public TestInstance forceFailed(CacheJobInfo... jobs) {
        return forceStatus(FAILED, jobs);
    }

    public TestInstance forceAborted(CacheJobInfo... jobs) {
        return forceStatus(ABORTED, jobs);
    }

    /**
     * Bypasses the events system and access this instance's {@link CacheJobRegistry job registry}
     * directly, {@link CacheJobRegistry#setStatus setting} the status for the job directly.
     *
     * @return
     */
    public TestInstance forceStatus(Status status, CacheJobInfo... jobs) {
        CacheJobRegistry registry = this.jobRegistry;
        for (CacheJobInfo job : jobs) {
            assertThat(registry.getStatus(job.getId())).isPresent();

            CacheJobStatus updated = registry.setStatus(job.getId(), status).orElseThrow();
            assertEquals(job, updated.getJobInfo());
            assertEquals(status, updated.getStatus());
        }
        return this;
    }

    public TestInstance assertHasJob(CacheJobInfo job) {
        assertEquals(job, getStatus(job).getJobInfo());
        return this;
    }

    public TestInstance assertDoesNotHaveJob(CacheJobInfo job) {
        CacheJobManager manager = getJobManager();
        await().between(MIN_WAIT_FOR_STATUS, MAX_WAIT_FOR_STATUS)
                .untilAsserted(() -> assertThat(manager.getJobStatus(job.getId()).isEmpty()));
        return this;
    }

    public void assertHasNoJobs() {
        CacheJobManager manager = getJobManager();
        await().between(MIN_WAIT_FOR_STATUS, MAX_WAIT_FOR_STATUS)
                .untilAsserted(() -> assertThat(manager.getJobs()).isEmpty());
    }

    public void assertJobStatus(Status expected, @NonNull CacheJobInfo job) {
        assertJobStatus(expected, job.getId());
    }

    public void assertJobStatus(Status expected, @NonNull String jobId) {
        CacheJobStatus status = awaitForStatus(jobId, expected);
        assertEquals(jobId, status.getJobInfo().getId());
    }

    private CacheJobStatus awaitForStatus(@NonNull String jobId, Status... validStauses) {
        ClusteringCacheJobManager manager = getJobManager();
        CacheJobStatus status = getStatus(manager, jobId);
        assertThat(status.getStatus()).isIn(Set.of(validStauses));
        return status;
    }

    private CacheJobStatus getStatus(ClusteringCacheJobManager manager, String jobId) {

        return await().atMost(MAX_WAIT_FOR_STATUS)
                .alias(String.format("manager: %s, job: %s", manager.instanceId(), jobId))
                .until(
                        () -> {
                            return manager.getJobStatus(jobId);
                        },
                        Optional::isPresent)
                .orElseThrow();
    }

    public @Override String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getJobManager().instanceId());
    }
}
