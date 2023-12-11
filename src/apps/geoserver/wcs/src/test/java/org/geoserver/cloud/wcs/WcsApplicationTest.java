/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wcs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@SpringBootTest
@ActiveProfiles("test")
class WcsApplicationTest {
    protected @Autowired ConfigurableApplicationContext context;

    @Test
    void testWcsCoreBeans() {
        expectBean("legacyWcsLoader", org.geoserver.wcs.WCSLoader.class);
        expectBean("wcsLoader", org.geoserver.wcs.WCSXStreamLoader.class);
        expectBean("wcsFactoryExtension", org.geoserver.wcs.WCSFactoryExtension.class);
        expectBean("wcsURLMapping", org.geoserver.ows.OWSHandlerMapping.class);
        expectBean("wcsLocalWorkspaceURLManger", org.geoserver.ows.LocalWorkspaceURLMangler.class);
        expectBean("cqlKvpParser", org.geoserver.ows.kvp.CQLFilterKvpParser.class);
        expectBean(
                "coverageResponseDelegateFactory",
                org.geoserver.wcs.responses.CoverageResponseDelegateFinder.class);
        expectBean(
                "geotiffCoverageResponseDelegate",
                org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.class);
        expectBean(
                "imgCoverageResponseDelegate",
                org.geoserver.wcs.responses.IMGCoverageResponseDelegate.class);
        expectBean(
                "debugCoverageResponseDelegate",
                org.geoserver.wcs.responses.DebugCoverageResponseDelegate.class);
        expectBean("coverageCleaner", org.geoserver.wcs.CoverageCleanerCallback.class);
        expectBean("wcsResourceVoter", org.geoserver.wcs.WCSResourceVoter.class);
        expectBean("legacyWcsLoader", org.geoserver.wcs.WCSLoader.class);
        expectBean("wcsLoader", org.geoserver.wcs.WCSXStreamLoader.class);
        expectBean("wcsFactoryExtension", org.geoserver.wcs.WCSFactoryExtension.class);
        expectBean("wcsURLMapping", org.geoserver.ows.OWSHandlerMapping.class);
        expectBean("wcsLocalWorkspaceURLManger", org.geoserver.ows.LocalWorkspaceURLMangler.class);
        expectBean("cqlKvpParser", org.geoserver.ows.kvp.CQLFilterKvpParser.class);
        expectBean(
                "coverageResponseDelegateFactory",
                org.geoserver.wcs.responses.CoverageResponseDelegateFinder.class);
        expectBean(
                "geotiffCoverageResponseDelegate",
                org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.class);
        expectBean(
                "imgCoverageResponseDelegate",
                org.geoserver.wcs.responses.IMGCoverageResponseDelegate.class);
        expectBean(
                "debugCoverageResponseDelegate",
                org.geoserver.wcs.responses.DebugCoverageResponseDelegate.class);
        expectBean("coverageCleaner", org.geoserver.wcs.CoverageCleanerCallback.class);
        expectBean("wcsResourceVoter", org.geoserver.wcs.WCSResourceVoter.class);
    }

    @Test
    void testBeansWcs_1_0() {
        expectBeans(
                "wcs100ServiceTarget",
                "wcsLogger",
                "wcs100Service",
                "wcsService-1.0.0",
                "wcs100ExceptionHandler",
                "wcs100AxisSubsetKvpParser",
                "wcs100BBoxKvpParser",
                "wcs100InterpolationKvpParser",
                "wcs100CoverageKvpParser",
                "wcs100SourceCoverageKvpParser",
                "wcs100SectionKvpParser",
                "wcs100TimeKvpParser",
                "wcs100ElevationKvpParser",
                "wcs100GetCapabilitiesKvpReader",
                "wcs100DescribeCoverageKvpReader",
                "wcs100GetCoverageRequestReader",
                "wcs-1.0.0-configuration",
                "wcs100CapabilitiesRequestReader",
                "wcs100DescribeCoverageRequestReader",
                "wcs100GetCoverageRequestXMLReader",
                "wcs100GetCapabilitiesResponse",
                "wcs100DescribeCoverageResponse",
                "wcs100GetCoverageResponse",
                "workspaceQualifier",
                "wcs100ServiceTarget",
                "wcsLogger",
                "wcs100Service",
                "wcsService-1.0.0",
                "wcs100ExceptionHandler",
                "wcs100AxisSubsetKvpParser",
                "wcs100BBoxKvpParser",
                "wcs100InterpolationKvpParser",
                "wcs100CoverageKvpParser",
                "wcs100SourceCoverageKvpParser",
                "wcs100SectionKvpParser",
                "wcs100TimeKvpParser",
                "wcs100ElevationKvpParser",
                "wcs100GetCapabilitiesKvpReader",
                "wcs100DescribeCoverageKvpReader",
                "wcs100GetCoverageRequestReader",
                "wcs-1.0.0-configuration",
                "wcs100CapabilitiesRequestReader",
                "wcs100DescribeCoverageRequestReader",
                "wcs100GetCoverageRequestXMLReader",
                "wcs100GetCapabilitiesResponse",
                "wcs100DescribeCoverageResponse",
                "wcs100GetCoverageResponse",
                "workspaceQualifier");
    }

    protected void expectBeans(String... names) {
        Stream.of(names).forEach(name -> assertThat(context.getBean(name)).isNotNull());
    }

    protected void expectBean(String name, Class<?> type) {
        assertThat(context.getBean(name)).isInstanceOf(type);
    }
}
