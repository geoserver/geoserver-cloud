/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.PUBLISHED;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;

import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.junit.jupiter.api.Test;

class CacheKeyTest {

    @Test
    void entityKeysEqualByTypeAndId() {
        CacheKey key = CacheKey.entity(LAYER, "layer-1");
        CacheKey sameTypeAndId = CacheKey.entity(LAYER, "layer-1");
        CacheKey otherId = CacheKey.entity(LAYER, "layer-2");
        CacheKey otherType = CacheKey.entity(STYLE, "layer-1");

        assertThat(key).isEqualTo(sameTypeAndId).hasSameHashCodeAs(sameTypeAndId);
        assertThat(key).isNotEqualTo(otherId).isNotEqualTo(otherType);
    }

    @Test
    void nameKeyDiffersFromEntityKey() {
        CacheKey name = CacheKey.name(LAYER, "topp:roads");
        CacheKey entity = CacheKey.entity(LAYER, "topp:roads");
        CacheKey lookup = CacheKey.lookup(LAYER, "topp:roads");

        assertThat(name.kind()).isEqualTo(Kind.NAME);
        assertThat(name).isNotEqualTo(entity).isNotEqualTo(lookup);
    }

    // same type and id: the kind alone tells the keys apart
    @Test
    void tileLayerKeyDiffersFromEntity() {
        CacheKey tileLayer = CacheKey.tileLayer("l1");

        assertThat(tileLayer.kind()).isEqualTo(Kind.TILE_LAYER);
        assertThat(tileLayer).isNotEqualTo(CacheKey.entity(PUBLISHED, "l1"));
    }

    @Test
    void rejectsNullParts() {
        assertThatNullPointerException().isThrownBy(() -> CacheKey.entity(null, "layer-1"));
        assertThatNullPointerException().isThrownBy(() -> CacheKey.name(LAYER, null));
    }
}
