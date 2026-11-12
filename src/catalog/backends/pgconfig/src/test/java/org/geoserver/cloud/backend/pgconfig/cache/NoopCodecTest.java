/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoopCodecTest {

    @Test
    void roundTripsJson() {
        NoopCodec codec = new NoopCodec();
        byte[] json = "{\"name\": \"roads\"}".getBytes(UTF_8);

        byte[] stored = codec.encode(json);

        assertThat(stored).isEqualTo(json);
        assertThat(codec.decode(stored)).isEqualTo(json);
    }
}
