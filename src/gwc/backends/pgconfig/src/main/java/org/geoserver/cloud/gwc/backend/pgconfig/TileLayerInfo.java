/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geowebcache.util.GWCVars;

/**
 * @since 1.7
 */
@Data
@Accessors(chain = true)
public class TileLayerInfo {

    @JsonIgnore
    private PublishedInfo published;

    private boolean enabled;

    private Boolean inMemoryCached;

    private String blobStoreId;

    private Set<String> mimeFormats = Set.of();

    private Set<GridSubset> gridSubsets = Set.of();

    private int metaTilingX;
    private int metaTilingY;

    /**
     * @see GWCVars#CACHE_DISABLE_CACHE
     * @see GWCVars#CACHE_NEVER_EXPIRE
     * @see GWCVars#CACHE_USE_WMS_BACKEND_VALUE
     * @see GWCVars#CACHE_VALUE_UNSET
     */
    private int expireCache = GWCVars.CACHE_VALUE_UNSET;

    private List<ExpirationRule> expireCacheList;

    private int expireClients;

    // Just used for serialize/deserialize to make xstream keep the same format it
    // used to.
    private Set<ParamFilter> parameterFilters = Set.of();

    // //// GeoServerTileLayer specific properties //////
    private int gutter;

    // Set of cache warnings that would cause caching being skipped
    Set<WarningType> cacheWarningSkips;

    @SuppressWarnings("java:S115")
    public enum WarningType {
        Default,
        Nearest,
        FailedNearest
    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpirationRule {
        private int minZoom;
        private int expiration;
    }

    @Data
    @Accessors(chain = true)
    public static class GridSubset {
        private String gridSetName;
        private Bounds extent;
        private Integer zoomStart;
        private Integer zoomStop;
        private Integer minCachedLevel;
        private Integer maxCachedLevel;
    }

    @Data
    @Accessors(chain = true)
    public static class Bounds {
        private double minX;
        private double minY;
        private double maxX;
        private double maxY;
    }

    @Data
    @Accessors(chain = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = RegexParamFilter.class, name = "RegexParameterFilter"),
        @JsonSubTypes.Type(value = StringParamFilter.class, name = "StringParameterFilter"),
        @JsonSubTypes.Type(value = FloatParamFilter.class, name = "FloatParameterFilter"),
        @JsonSubTypes.Type(value = IntegerParamFilter.class, name = "IntegerParameterFilter"),
        @JsonSubTypes.Type(value = StyleParamFilter.class, name = "StyleParameterFilter")
    })
    public abstract static class ParamFilter {
        private String key;
        private String defaultValue;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class CaseNormalizingParameterFilter extends ParamFilter {
        public enum Case {
            NONE,
            UPPER,
            LOWER;
        }

        private Case normalizerCase;
        private Locale normalizerLocale;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class RegexParamFilter extends CaseNormalizingParameterFilter {
        private String regex;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class StringParamFilter extends CaseNormalizingParameterFilter {
        private List<String> values;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class FloatParamFilter extends ParamFilter {
        private List<Float> values;
        private Float threshold;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class IntegerParamFilter extends ParamFilter {
        private List<Integer> values;
        private Integer threshold;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class StyleParamFilter extends ParamFilter {
        private Set<String> styles;
    }

    public String id() {
        return getPublished().getId();
    }

    public Optional<String> workspace() {
        PublishedInfo publishedInfo = getPublished();
        return Optional.ofNullable(
                switch (publishedInfo) {
                    case LayerInfo layer -> layer.getResource().getNamespace().getPrefix();
                    case LayerGroupInfo lg ->
                        lg.getWorkspace() == null ? null : lg.getWorkspace().getName();
                    default -> throw new IllegalStateException("published: " + publishedInfo);
                });
    }

    public String name() {
        return getPublished().getName();
    }
}
