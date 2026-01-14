/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wcs;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class WcsApplicationTest {
    protected @Autowired ConfigurableApplicationContext context;

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
    }

    @Test
    void testWcsCoreBeans() {
        expectBean("legacyWcsLoader", org.geoserver.wcs.WCSLoader.class);
        expectBean("wcsLoader", org.geoserver.wcs.WCSXStreamLoader.class);
        expectBean("wcsFactoryExtension", org.geoserver.wcs.WCSFactoryExtension.class);
        expectBean("wcsURLMapping", org.geoserver.ows.OWSHandlerMapping.class);
        expectBean("wcsLocalWorkspaceURLManger", org.geoserver.ows.LocalWorkspaceURLMangler.class);
        expectBean("cqlKvpParser", org.geoserver.ows.kvp.CQLFilterKvpParser.class);
        expectBean("coverageResponseDelegateFactory", org.geoserver.wcs.responses.CoverageResponseDelegateFinder.class);
        expectBean(
                "geotiffCoverageResponseDelegate", org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.class);
        expectBean("imgCoverageResponseDelegate", org.geoserver.wcs.responses.IMGCoverageResponseDelegate.class);
        expectBean("debugCoverageResponseDelegate", org.geoserver.wcs.responses.DebugCoverageResponseDelegate.class);
        expectBean("coverageCleaner", org.geoserver.wcs.CoverageCleanerCallback.class);
        expectBean("wcsResourceVoter", org.geoserver.wcs.WCSResourceVoter.class);
        expectBean("legacyWcsLoader", org.geoserver.wcs.WCSLoader.class);
        expectBean("wcsFactoryExtension", org.geoserver.wcs.WCSFactoryExtension.class);
        expectBean("wcsURLMapping", org.geoserver.ows.OWSHandlerMapping.class);
        expectBean("wcsLocalWorkspaceURLManger", org.geoserver.ows.LocalWorkspaceURLMangler.class);
        expectBean("cqlKvpParser", org.geoserver.ows.kvp.CQLFilterKvpParser.class);
        expectBean("coverageResponseDelegateFactory", org.geoserver.wcs.responses.CoverageResponseDelegateFinder.class);
        expectBean(
                "geotiffCoverageResponseDelegate", org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.class);
        expectBean("imgCoverageResponseDelegate", org.geoserver.wcs.responses.IMGCoverageResponseDelegate.class);
        expectBean("debugCoverageResponseDelegate", org.geoserver.wcs.responses.DebugCoverageResponseDelegate.class);
        expectBean("coverageCleaner", org.geoserver.wcs.CoverageCleanerCallback.class);
        expectBean("wcsResourceVoter", org.geoserver.wcs.WCSResourceVoter.class);
    }

    @Test
    void testBeansWcs_1_0() {
        expectBeans(
                "wcs100ExceptionHandler",
                "wcs-1.0.0-configuration",
                "wcs100AxisSubsetKvpParser",
                "wcs100BBoxKvpParser",
                "wcs100CapabilitiesRequestReader",
                "wcs100CoverageKvpParser",
                "wcs100DescribeCoverageKvpReader",
                "wcs100DescribeCoverageRequestReader",
                "wcs100DescribeCoverageResponse",
                "wcs100ElevationKvpParser",
                "wcs100GetCapabilitiesKvpReader",
                "wcs100GetCapabilitiesResponse",
                "wcs100GetCoverageRequestReader",
                "wcs100GetCoverageRequestXMLReader",
                "wcs100GetCoverageResponse",
                "wcs100InterpolationKvpParser",
                "wcs100Logger",
                "wcs100SectionKvpParser",
                "wcs100Service",
                "wcs100ServiceTarget",
                "wcs100SourceCoverageKvpParser",
                "wcs100TimeKvpParser",
                "wcs10ExceptionHandler",
                "wcs10Extension",
                "wcsService-1.0.0",
                "workspaceQualifier");
    }

    @Test
    void testBeansWcs_1_1() {
        expectBeans(
                "ows11AcceptVersionsKvpParser",
                "ows11SectionsKvpParser",
                "wcs-1.1.1-configuration",
                "wcs111BoundingBoxKvpParser",
                "wcs111CapabilitiesRequestReader",
                "wcs111DescribeCoverageKvpReader",
                "wcs111DescribeCoverageRequestReader",
                "wcs111DescribeCoverageResponse",
                "wcs111DispatcherMapping",
                "wcs111ExceptionHandler",
                "wcs111GetCapabilitiesKvpReader",
                "wcs111GetCapabilitiesResponse",
                "wcs111GetCoverageMultipartResponse",
                "wcs111GetCoverageRequestReader",
                "wcs111GetCoverageRequestXMLReader",
                "wcs111GetCoverageStoreResponse",
                "wcs111GridBaseCRSKvpParser",
                "wcs111GridCSKvpParser",
                "wcs111GridOffsetsKvpParser",
                "wcs111GridOriginKvpParser",
                "wcs111GridTypeKvpParser",
                "wcs111IdentifierKvpParser",
                "wcs111Logger",
                "wcs111RangeSubsetKvpParser",
                "wcs111Service",
                "wcs111ServiceTarget",
                "wcs111StoreKvpParser",
                "wcs111TempStorageCleaner",
                "wcs111TempStorageCleanerTask",
                "wcs111TimeSequenceKvpParser",
                "wcs111timerFactory",
                "wcs11ExceptionHandler",
                "wcs11WorkspaceQualifier",
                "wcsService-1.1",
                "wcsService-1.1.0",
                "wcsService-1.1.1",
                "wcs_1_1_Extension");
    }

    @Test
    void testBeansWcs_2_0() {
        expectBeans(
                "wcs201interpolationKvpParser",
                "GMLCoverageResponseDelegate",
                "MIMETYPEMapper",
                "envelopeDimensionsMapper",
                "imgWcsMimeMapper",
                "overviewPolicyKvpParser",
                "wcs-2.0.1-configuration",
                "wcs200interpolationKvpParser",
                "wcs201CapabilitiesRequestReader",
                "wcs201DescribeCoverageRequestReader",
                "wcs201GetCoverageRequestReader",
                "wcs201GetCoverageResponse",
                "wcs201MultipartGetCoverageResponse",
                "wcs201SortByKvpParser",
                "wcs201rangeSubsetKvpParser",
                "wcs20DescribeCoverageKvpReader",
                "wcs20DescribeCoverageResponse",
                "wcs20ExceptionHandler",
                "wcs20Extension",
                "wcs20GetCapabilitiesKvpReader",
                "wcs20GetCapabilitiesResponse",
                "wcs20Logger",
                "wcs20Service",
                "wcs20ServiceTarget",
                "wcs20describeCoverageIdKvpParser",
                "wcs20getCoverageKvpParser",
                "wcs20rangeSubsetKvpParser",
                "wcs20scaleAxesKvpParser",
                "wcs20scaleExtentKvpParser",
                "wcs20scaleFactorKvpParser",
                "wcs20scaleSizeKvpParser",
                "wcs20subsetKvpParser",
                "wcsDefaultLocaleCallback",
                "wcsService-2.0");
    }

    protected void expectBeans(String... names) {
        Stream.of(names).forEach(name -> assertThat(context.getBean(name)).isNotNull());
    }

    protected void expectBean(String name, Class<?> type) {
        assertThat(context.getBean(name)).isInstanceOf(type);
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>
     * Verifies that only the WCS conditional bean is activated in this service,
     * based on the geoserver.service.wcs.enabled=true property set in bootstrap.yml.
     * This test relies on the ConditionalTestAutoConfiguration class from the
     * extensions-core test-jar, which contains beans conditionally activated
     * based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // This should exist in WCS service
        assertThat(context.containsBean("wcsConditionalBean")).isTrue();

        ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                context.getBean("wcsConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
        assertThat(bean.getServiceName()).isEqualTo("WCS");

        // These should not exist in WCS service
        assertThat(context.containsBean("wmsConditionalBean")).isFalse();
        assertThat(context.containsBean("wfsConditionalBean")).isFalse();
        assertThat(context.containsBean("wpsConditionalBean")).isFalse();
        assertThat(context.containsBean("restConditionalBean")).isFalse();
        assertThat(context.containsBean("webUiConditionalBean")).isFalse();
    }
}
