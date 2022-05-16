/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gwc.tiling.model.CacheJobStatus.Status.COMPLETE;
import static org.gwc.tiling.model.CacheJobStatus.Status.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.geowebcache.mime.ImageMime;
import org.gwc.tiling.cluster.support.DistributedContextSupport;
import org.gwc.tiling.cluster.support.TestInstance;
import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobRequest.Action;
import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.model.CacheJobStatus.Status;
import org.gwc.tiling.model.TileLayerInfo;
import org.gwc.tiling.model.TileLayerMockSupport;
import org.gwc.tiling.service.CacheJobRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

/**
 * @since 1.0
 */
class ClusteringCacheJobManagerTest {

    private @TempDir File cacheDirectory;
    private DistributedContextSupport support;

    private TestInstance instance1;
    private TestInstance instance2;
    private TestInstance instance3;

    ClusteringCacheJobManager manager1;
    ClusteringCacheJobManager manager2;
    ClusteringCacheJobManager manager3;

    private TileLayerInfo layer3857PNG;
    private TileLayerInfo layer4326PNG;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        support = new DistributedContextSupport(cacheDirectory);
        instance1 = support.newInstance();
        instance2 = support.newInstance();
        instance3 = support.newInstance();
        manager1 = (ClusteringCacheJobManager) instance1.getJobManager();
        manager2 = (ClusteringCacheJobManager) instance2.getJobManager();
        manager3 = (ClusteringCacheJobManager) instance3.getJobManager();

        TileLayerMockSupport layers = support.mockLayers();
        layer3857PNG = layers.mockLayer("test:layer1", layers.subset3857, ImageMime.png);
        layer4326PNG = layers.mockLayer("test:layer2", layers.subset4326, ImageMime.png);
    }

    public @Test void testInstanceId() {
        assertNotNull(manager1.instanceId());
        assertNotNull(manager2.instanceId());
        assertNotNull(manager3.instanceId());

        assertNotEquals(manager1.instanceId(), manager2.instanceId());
        assertNotEquals(manager1.instanceId(), manager3.instanceId());
    }

    public @Test void testRequestBuilder() {
        CacheJobRequestBuilder rb1 = manager1.newRequestBuilder();
        CacheJobRequestBuilder rb2 = manager2.newRequestBuilder();
        assertNotNull(rb1);
        assertNotNull(rb2);
        assertNotSame(rb1, rb2);
    }

    public @Test void job_mutating_opts_unavailable_if_instance_is_not_running() {
        assertFalse(manager1.isRunning());

        Class<IllegalStateException> ise = IllegalStateException.class;
        assertThrows(ise, () -> manager1.launchJob(seedAll(layer3857PNG)));
        assertThrows(ise, () -> manager1.abortJob("someid"));
        assertThrows(ise, () -> manager1.pruneJobs());

        assertThat(manager1.getJobs()).isNotNull();
        assertThat(manager1.getJobs()).isEmpty();
        assertThat(manager1.getJobStatus("someid")).isNotNull();
        assertTrue(manager1.getJobStatus("someid").isEmpty());
    }

    public @Test void testLaunchJob_single_instance() {
        assertTrue(manager1.getJobs().isEmpty());
        manager1.joinCluster();

        CacheJobRequest request = seedAll(layer3857PNG);
        final CacheJobInfo jobInfo = manager1.launchJob(request);
        assertNotNull(jobInfo);
        assertNotNull(jobInfo.getId());
        assertEquals(request, jobInfo.getRequest());

        CacheJobStatus status = instance1.getStatus(jobInfo);
        assertThat(status.getStatus()).isIn(Status.SCHEDULED, Status.RUNNING);
        assertEquals(jobInfo, status.getJobInfo());
    }

    public @Test void testLaunchJob_propagates_to_all_nodes_in_cluster() {
        manager1.joinCluster();
        manager2.joinCluster();
        manager3.joinCluster();

        CacheJobRequest request = seedAll(layer3857PNG);
        final CacheJobInfo job1 = instance1.launchAndGet(request);
        final CacheJobInfo job2 = instance2.launchAndGet(request);
        final CacheJobInfo job3 = instance3.launchAndGet(request);

        awaitForNewJob(job1, instance1, instance2, instance3);
        awaitForNewJob(job2, instance1, instance2, instance3);
        awaitForNewJob(job3, instance1, instance2, instance3);
    }

    public @Test void testJoinCluster_receives_running_jobs() {
        manager1.joinCluster();

        CacheJobRequest request = seedAll(layer3857PNG);

        final CacheJobInfo scheduled = instance1.launchAndGet(request);
        final CacheJobInfo running = instance1.launchAndGet(request);
        final CacheJobInfo complete = instance1.launchAndGet(request);

        instance2.assertHasNoJobs();
        instance3.assertHasNoJobs();

        instance1 //
                .forceScheduled(scheduled) //
                .forceComplete(complete) //
                .forceRunning(running);

        // manager2 joins cluster, should get the scheduled/running jobs
        manager2.joinCluster();
        instance2.assertJobStatus(SCHEDULED, scheduled);
        instance2.assertJobStatus(SCHEDULED, running);

        // manager3 still didn't join the cluster
        instance3.assertHasNoJobs();

        // manager3 joins cluster, should get the scheduled/running jobs
        manager3.joinCluster();

        instance3.assertJobStatus(SCHEDULED, scheduled);
        instance3.assertJobStatus(SCHEDULED, running);
    }

    public @Test void testLeaveCluster_does_not_receive_more_jobs() {
        manager1.joinCluster();
        manager2.joinCluster();
        manager3.joinCluster();

        CacheJobRequest request = seedAll(layer3857PNG);
        final CacheJobInfo job1 = instance1.launchAndGet(request);
        instance2.awaitForNewJob(job1);
        instance3.awaitForNewJob(job1);

        manager1.leaveCluster();

        final CacheJobInfo job2 = instance2.launchAndGet(request);
        instance3.awaitForNewJob(job2);
        instance1.assertHasNoJobs();

        assertThat(manager1.getJobStatus(job2.getId())).isEmpty();
    }

    public @Test void testPruneJobs_single_instance() {
        manager1.joinCluster();

        final CacheJobInfo job1 = instance1.launchAndGet(seedAll(layer3857PNG));
        final CacheJobInfo job2 = instance1.launchAndGet(seedAll(layer4326PNG));
        final CacheJobInfo job3 = instance1.launchAndGet(seedAll(layer3857PNG));

        instance2.assertHasNoJobs();
        instance3.assertHasNoJobs();

        instance1.forceComplete(job1);
        instance1.assertJobStatus(COMPLETE, job1);

        List<CacheJobStatus> pruned = manager1.pruneJobs();
        assertEquals(1, pruned.size());
        assertEquals(job1, pruned.get(0).getJobInfo());
        assertThat(manager1.getJobs()).doesNotContain(job1);
        assertThat(manager1.getJobs()).containsExactlyInAnyOrder(job2, job3);
    }

    public @Test void testPruneJobs_propagates_to_all_instances() {
        manager1.joinCluster();
        manager2.joinCluster();
        manager3.joinCluster();

        // given three jobs launched by difference instances
        final CacheJobInfo job1 = instance1.launchAndGet(seedAll(layer3857PNG));
        final CacheJobInfo job2 = instance2.launchAndGet(seedAll(layer4326PNG));
        final CacheJobInfo job3 = instance3.launchAndGet(seedAll(layer3857PNG));

        // and once all instances have all jobs
        instance1.awaitForNewJobs(job2, job3);
        instance2.awaitForNewJobs(job1, job3);
        instance3.awaitForNewJobs(job1, job2);

        // and one job is terminated in all instances, either completed, failed, or cancelled
        instance1.forceComplete(job3);
        instance2.forceFailed(job3);
        instance3.forceAborted(job3);

        // when any instance's pruneJobs() is called
        List<CacheJobStatus> prunedJobs = manager2.pruneJobs();
        assertThat(prunedJobs).singleElement();
        assertThat(prunedJobs.get(0).getJobInfo()).isEqualTo(job3);

        // then all instances get the jobs pruned
        instance1.assertDoesNotHaveJob(job3);
        instance2.assertDoesNotHaveJob(job3);
        instance3.assertDoesNotHaveJob(job3);
    }

    public @Test void testCancelJobSingleInstance() {
        manager1.joinCluster();

        final CacheJobInfo job1 = instance1.launchAndGet(seedAll(layer3857PNG));
        final CacheJobInfo job2 = instance1.launchAndGet(seedAll(layer4326PNG));
        final CacheJobInfo job3 = instance1.launchAndGet(seedAll(layer3857PNG));

        instance1.awaitForNewJobs(job1, job2, job3);
        instance2.assertHasNoJobs();
        instance2.assertHasNoJobs();

        instance1.forceRunning(job1, job2);

        testAbort(instance1, job1);
        testAbort(instance1, job2);
        testAbort(instance1, job3);
    }

    public @Test void testCancelJob_cancels_on_all_instances() {
        manager1.joinCluster();
        manager2.joinCluster();
        manager3.joinCluster();

        // given three jobs launched by difference instances
        final CacheJobInfo job1 = instance1.launchAndGet(seedAll(layer3857PNG));
        final CacheJobInfo job2 = instance2.launchAndGet(seedAll(layer4326PNG));
        final CacheJobInfo job3 = instance3.launchAndGet(seedAll(layer3857PNG));

        // and once all instances have all jobs
        instance1.awaitForNewJobs(job2, job3);
        instance2.awaitForNewJobs(job1, job3);
        instance3.awaitForNewJobs(job1, job2);

        // and one job is terminated in all instances, either completed, failed, or cancelled
        instance1.forceRunning(job1, job2);
        instance2.forceRunning(job2, job3);
        instance3.forceRunning(job1, job3);

        testAbortsInAllInstances(instance1, job1);
        testAbortsInAllInstances(instance2, job2);
        testAbortsInAllInstances(instance3, job3);
    }

    private void testAbort(TestInstance instance, CacheJobInfo job) {
        ClusteringCacheJobManager manager = instance.getJobManager();
        CacheJobStatus aborting = manager.abortJob(job.getId()).orElseThrow();
        assertEquals(job, aborting.getJobInfo());
        assertEquals(Status.ABORTING, aborting.getStatus());
        instance.awaitForStatus(job, Status.ABORTED);
    }

    private void testAbortsInAllInstances(TestInstance instance, CacheJobInfo job) {
        testAbort(instance, job);
        instance1.assertJobStatus(Status.ABORTED, job);
        instance2.assertJobStatus(Status.ABORTED, job);
        instance3.assertJobStatus(Status.ABORTED, job);
    }

    private CacheJobRequest seedAll(TileLayerInfo layer) {
        return seed(layer).build().get(0);
    }

    private CacheJobRequestBuilder seed(TileLayerInfo layer) {
        return manager1.newRequestBuilder().action(Action.SEED).layer(layer.getName());
    }

    private void awaitForNewJob(CacheJobInfo job, TestInstance... instances) {
        for (TestInstance testInstance : instances) testInstance.awaitForNewJob(job);
    }
}
