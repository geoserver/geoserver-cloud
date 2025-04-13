/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.ParamFilter;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.StyleParameterFilter;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizer.Case;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.IntegerParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.ExpirationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * @since 1.7
 */
class GeoServerTileLayerInfoMapperTest {

    GeoServerTileLayerInfoMapper mapper;
    GeoServerTileLayerInfo info;
    GeoServerTileLayer tileLayer;

    @BeforeEach
    void before() {
        mapper = Mappers.getMapper(GeoServerTileLayerInfoMapper.class);
        LayerInfo layerInfo = new TileLayerMocking(new CatalogPlugin(), new GeoServerImpl()).layerInfo();
        GWCConfig defaults = new GWCConfig();
        info = TileLayerInfoUtil.loadOrCreate(layerInfo, defaults);
        info.setId(layerInfo.getId());
        info.setName(layerInfo.prefixedName());
        info.setBlobStoreId("defaultBlobStore");
        info.setParameterFilters(Set.of());
        info.setExpireCacheList(List.of());
        GridSetBroker gridsets = new GridSetBroker(List.of(new DefaultGridsets(false, false)));
        tileLayer = new GeoServerTileLayer(layerInfo, gridsets, info);
    }

    @Test
    void testDefaults() {
        GeoServerTileLayerInfoImpl roundtripped = roundtripTest();
        assertThat(roundtripped).isEqualTo(info);
    }

    @Test
    void testScalars() {
        info.setExpireCache(1000);
        info.setExpireClients(60);
        info.setGutter(20);
        info.setMetaTilingX(10);
        info.setMetaTilingY(20);
        GeoServerTileLayerInfoImpl roundtripped = roundtripTest();
        assertThat(roundtripped).isEqualTo(info);
    }

    @Test
    void testRegexParameterFilter() {
        testParameterFilters(Set.of(regexParameterFilter()));
    }

    @Test
    void testStringParameterFilter() {
        testParameterFilters(Set.of(stringParameterFilter()));
    }

    @Test
    void testFloatParameterFilter() {
        testParameterFilters(Set.of(floatParameterFilter()));
    }

    @Test
    void testIntegerParameterFilter() {
        testParameterFilters(Set.of(integerParameterFilter()));
    }

    @Test
    void testStyleParameterFilter() {
        testParameterFilters(Set.of(styleParameterFilter()));
    }

    @Test
    void testParameterFilters() {
        testParameterFilters(Set.of(
                regexParameterFilter(),
                stringParameterFilter(),
                floatParameterFilter(),
                integerParameterFilter(),
                styleParameterFilter()));
    }

    @Test
    void testUnknownParameterFilter() {
        ParameterFilter unknownFilter = mock(ParameterFilter.class);
        when(unknownFilter.getKey()).thenReturn("fake");

        Set<ParameterFilter> filters = Set.of(unknownFilter);
        IllegalArgumentException expected =
                assertThrows(IllegalArgumentException.class, () -> testParameterFilters(filters));
        assertThat(expected.getMessage()).contains("Unknown ParameterFilter type");

        TileLayerInfo pgTileLayer = new TileLayerInfo();
        pgTileLayer.setMetaTilingX(1);
        pgTileLayer.setMetaTilingY(1);
        pgTileLayer.setParameterFilters(Set.of(mock(ParamFilter.class)));

        expected = assertThrows(IllegalArgumentException.class, () -> mapper.map(pgTileLayer));
        assertThat(expected.getMessage()).contains("Unknown ParamFilter type");
    }

    @Test
    void testGridSubsets() {
        List<XMLGridSubset> gridSubsets = List.copyOf(info.getGridSubsets());
        XMLGridSubset set1 = gridSubsets.get(0);
        set1.setExtent(new BoundingBox(-180, -90, 0, 0));
        set1.setMinCachedLevel(3);
        set1.setMaxCachedLevel(12);
        set1.setZoomStart(1);
        set1.setZoomStop(16);

        XMLGridSubset set2 = gridSubsets.get(1);
        set2.setExtent(new BoundingBox(0, 0, 10000, 20000));
        set2.setMinCachedLevel(1);
        set2.setMaxCachedLevel(4);
        set2.setZoomStart(1);
        set2.setZoomStop(12);

        info.setGridSubsets(Set.copyOf(gridSubsets));

        GeoServerTileLayerInfoImpl roundtripped = roundtripTest();
        assertThat(roundtripped.getGridSubsets()).isEqualTo(info.getGridSubsets());
    }

    @Test
    void testExpireCacheList() {
        List<ExpirationRule> rules = List.of(new ExpirationRule(1, 100), new ExpirationRule(12, 200));
        info.setExpireCacheList(rules);

        // ExpirationRule does not implement equals()
        GeoServerTileLayerInfoImpl roundtripped = roundtripTest();
        List<ExpirationRule> actual = roundtripped.getExpireCacheList();
        assertThat(actual).hasSameSizeAs(rules);
        assertThat(actual.get(0).getExpiration()).isEqualTo(rules.get(0).getExpiration());
        assertThat(actual.get(0).getMinZoom()).isEqualTo(rules.get(0).getMinZoom());
        assertThat(actual.get(1).getExpiration()).isEqualTo(rules.get(1).getExpiration());
        assertThat(actual.get(1).getMinZoom()).isEqualTo(rules.get(1).getMinZoom());
    }

    private void testParameterFilters(Set<ParameterFilter> filters) {
        info.setParameterFilters(filters);
        GeoServerTileLayerInfoImpl roundtripped = roundtripTest();
        assertThat(roundtripped).isEqualTo(info);
        assertThat(roundtripped.getParameterFilters()).isEqualTo(info.getParameterFilters());
    }

    private RegexParameterFilter regexParameterFilter() {
        RegexParameterFilter regex = new RegexParameterFilter();
        regex.setKey("REGEX_PARAM");
        regex.setRegex("test.*");
        regex.setDefaultValue("testme");
        regex.setNormalize(new CaseNormalizer(Case.LOWER, Locale.getDefault()));
        return regex;
    }

    private StringParameterFilter stringParameterFilter() {
        StringParameterFilter str = new StringParameterFilter();
        str.setKey("STRING_PARAM");
        str.setDefaultValue("string default");
        str.setValues(List.of("val1", "val2", "val3"));
        str.setNormalize(new CaseNormalizer(Case.LOWER, Locale.getDefault()));
        return str;
    }

    private FloatParameterFilter floatParameterFilter() {
        FloatParameterFilter filter = new FloatParameterFilter();
        filter.setKey("FLOAT_PARAM");
        filter.setDefaultValue("0.5");
        filter.setValues(List.of(10.1f, 9.2f, 8.3f));
        filter.setThreshold(0.2f);
        return filter;
    }

    private IntegerParameterFilter integerParameterFilter() {
        IntegerParameterFilter filter = new IntegerParameterFilter();
        filter.setKey("INT_PARAM");
        filter.setDefaultValue("-1");
        filter.setValues(List.of(10, 9, 8));
        filter.setThreshold(1);
        return filter;
    }

    private StyleParameterFilter styleParameterFilter() {
        StyleParameterFilter filter = new StyleParameterFilter();
        filter.setKey("STYLES");
        filter.setDefaultValue("string default");
        filter.setStyles(Set.of("style1", "style2"));

        // avoid exception if layer not set
        LayerInfo layer = mock(LayerInfo.class);
        filter.setLayer(layer);
        return filter;
    }

    private GeoServerTileLayerInfoImpl roundtripTest() {
        return roundtripTest(this.tileLayer);
    }

    private GeoServerTileLayerInfoImpl roundtripTest(GeoServerTileLayer layer) {
        TileLayerInfo pgInfo = mapper.map(layer);
        return mapper.map(pgInfo);
    }
}
