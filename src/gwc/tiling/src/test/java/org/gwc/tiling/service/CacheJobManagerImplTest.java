/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gwc.tiling.model.CacheJobStatus.Status.COMPLETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.NonNull;

import org.geowebcache.mime.ImageMime;
import org.gwc.tiling.integration.local.GeoWebCacheJobsConfiguration;
import org.gwc.tiling.model.CacheJobInfo;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobRequest.Action;
import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.model.CacheJobStatus.Status;
import org.gwc.tiling.model.TileLayerInfo;
import org.gwc.tiling.model.TileLayerMockSupport;
import org.gwc.tiling.service.support.MockTileLayersTestConfiguration;
import org.gwc.tiling.service.support.TileLayerMockSupportConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

/**
 * @since 1.0
 */
@SpringBootTest(
        classes = {
            GeoWebCacheJobsConfiguration.class,
            TileLayerMockSupportConfiguration.class,
            MockTileLayersTestConfiguration.class
        })
class CacheJobManagerImplTest {

    private @Autowired CacheJobRegistry jobRegistry;
    private @Autowired CacheJobManager manager;
    private @Autowired TileLayerMockSupport mockLayers;

    private TileLayerInfo layer3857PNG;
    private TileLayerInfo layer4326PNG;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        layer3857PNG = mockLayers.mockLayer("test:layer1", mockLayers.subset3857, ImageMime.png);
        layer4326PNG = mockLayers.mockLayer("test:layer2", mockLayers.subset4326, ImageMime.png);
    }

    public @Test void testRequestBuilder() {
        CacheJobRequestBuilder rb1 = manager.newRequestBuilder();
        assertNotNull(rb1);
        assertNotSame(rb1, manager.newRequestBuilder());
    }

    @DirtiesContext
    public @Test void testLaunchJob() {
        assertTrue(manager.getJobs().isEmpty());

        CacheJobRequest request = seedAll(layer3857PNG);
        final CacheJobInfo jobInfo = manager.launchJob(request);
        assertNotNull(jobInfo);
        assertNotNull(jobInfo.getId());
        assertEquals(request, jobInfo.getRequest());

        CacheJobStatus status = manager.getJobStatus(jobInfo.getId()).orElseThrow();
        assertThat(status.getStatus()).isIn(Status.SCHEDULED, Status.RUNNING);
        assertEquals(jobInfo, status.getJobInfo());
    }

    @DirtiesContext
    public @Test void testPruneJobs() {
        final CacheJobInfo job1 = manager.launchJob(seedAll(layer3857PNG));
        final CacheJobInfo job2 = manager.launchJob(seedAll(layer4326PNG));
        final CacheJobInfo job3 = manager.launchJob(seedAll(layer3857PNG));

        // force status
        this.jobRegistry.setStatus(job1.getId(), COMPLETE);

        List<CacheJobStatus> pruned = manager.pruneJobs();
        List<@NonNull CacheJobInfo> prunedJobs =
                pruned.stream().map(CacheJobStatus::getJobInfo).toList();
        assertEquals(List.of(job1), prunedJobs);
        assertThat(manager.getJobs()).doesNotContain(job1);
        assertThat(manager.getJobs().stream().map(CacheJobInfo::getId).toList())
                .containsExactlyInAnyOrder(job2.getId(), job3.getId());
        assertThat(manager.getJobs()).containsExactlyInAnyOrder(job2, job3);
        assertEquals(COMPLETE, pruned.get(0).getStatus());
    }

    public @Test void testCancelJob() {
        final CacheJobInfo job1 = manager.launchJob(seedAll(layer3857PNG));
        final CacheJobInfo job2 = manager.launchJob(seedAll(layer4326PNG));
        final CacheJobInfo job3 = manager.launchJob(seedAll(layer3857PNG));

        Optional<CacheJobStatus> aborted = manager.abortJob(job3.getId());
        assertEquals(job3, aborted.map(CacheJobStatus::getJobInfo).orElseThrow());
        assertThat(aborted.map(CacheJobStatus::getStatus).orElseThrow()).isEqualTo(Status.ABORTING);
        // test idempotency
        assertThat(manager.abortJob(job3.getId()).map(CacheJobStatus::getStatus).orElseThrow())
                .isEqualTo(Status.ABORTING);
        assertFalse(manager.getJobStatus(job1.getId()).orElseThrow().isFinished());
        assertFalse(manager.getJobStatus(job2.getId()).orElseThrow().isFinished());
    }

    private CacheJobRequest seedAll(TileLayerInfo layer) {
        return seed(layer).build().get(0);
    }

    private CacheJobRequestBuilder seed(TileLayerInfo layer) {
        return manager.newRequestBuilder().action(Action.SEED).layer(layer.getName());
    }
}
