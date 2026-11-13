/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.github.luben.zstd.ZstdDictTrainer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ZstdCodecTest {

    private static final String WORKSPACE_TEMPLATE =
            """
            {"id": "WorkspaceInfo-%08x", "name": "ws%d", "@type": "WorkspaceInfo", "isolated": %b, \
            "metadata": {"MetadataMap": {}}, "dateCreated": "2026-11-0%dT00:31:35.411Z", "dateModified": null}\
            """;

    private final ZstdCodec codec = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL);

    @AfterEach
    void closeCodec() {
        codec.close();
    }

    @Test
    void roundTripsJson() throws IOException {
        byte[] json = layerSample();

        byte[] stored = codec.encode(json);

        assertThat(codec.decode(stored)).isEqualTo(json);
    }

    @Test
    void compressesSampleAtLeastTwofold() throws IOException {
        byte[] json = layerSample();

        byte[] stored = codec.encode(json);

        double ratio = (double) json.length / stored.length;
        assertThat(ratio).isGreaterThanOrEqualTo(2.0);
    }

    @Test
    void roundTripsWithOtherDictionary() throws IOException {
        byte[] json = layerSample();

        try (ZstdCodec other = new ZstdCodec(ZstdCodec.DEFAULT_LEVEL, trainOtherDictionary())) {
            byte[] stored = other.encode(json);

            assertThat(other.decode(stored)).isEqualTo(json);
        }
    }

    @Test
    void rejectsForeignDictionary() throws IOException {
        byte[] foreign = encodeWithOtherDictionary(layerSample());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> codec.decode(foreign))
                .withMessageContaining("Dictionary mismatch");
    }

    // decoding allocates the content size read from the frame header. This header holds the zstd magic number, a
    // descriptor for a 4-byte content size, and 64 MiB + 1 as that size, little-endian.
    @Test
    void rejectsContentAbove64MiB() {
        byte[] header = {0x28, (byte) 0xB5, 0x2F, (byte) 0xFD, (byte) 0xA0, 0x01, 0x00, 0x00, 0x04};

        assertThatIllegalArgumentException()
                .isThrownBy(() -> codec.decode(header))
                .withMessageContaining("content size 67108865");
    }

    @Test
    void rejectsGarbage() throws IOException {
        byte[] text = "not a zstd frame".getBytes(UTF_8);
        byte[] stored = codec.encode(layerSample());
        byte[] truncated = Arrays.copyOf(stored, stored.length - 3);

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(text));
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(new byte[0]));
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(truncated));
    }

    // without the zstd header a dictionary has no id, and its frames look like those of any other dictionary
    @Test
    void requiresTrainedDictionary() {
        byte[] rawContent = "raw content, no zstd dictionary header".getBytes(UTF_8);

        assertThatIllegalArgumentException().isThrownBy(() -> new ZstdCodec(ZstdCodec.DEFAULT_LEVEL, rawContent));
    }

    @Test
    void roundTripsAcrossThreads() throws IOException {
        byte[] json = layerSample();
        IntStream rounds = IntStream.range(0, 10_000).parallel();

        boolean allEqual = rounds.allMatch(round -> Arrays.equals(json, roundTrip(json)));

        assertThat(allEqual).isTrue();
    }

    private byte[] roundTrip(byte[] json) {
        byte[] stored = codec.encode(json);
        return codec.decode(stored);
    }

    private byte[] layerSample() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("layer.json")) {
            return in.readAllBytes();
        }
    }

    private static byte[] encodeWithOtherDictionary(byte[] json) {
        try (ZstdCodec other = new ZstdCodec(ZstdCodec.DEFAULT_LEVEL, trainOtherDictionary())) {
            return other.encode(json);
        }
    }

    private static byte[] trainOtherDictionary() {
        int samples = 200;
        ZstdDictTrainer trainer = new ZstdDictTrainer(samples * 1024, 4 * 1024);
        for (int i = 0; i < samples; i++) {
            trainer.addSample(workspaceJson(i));
        }
        return trainer.trainSamples();
    }

    private static byte[] workspaceJson(int index) {
        String json = WORKSPACE_TEMPLATE.formatted(index * 7919, index, index % 2 == 0, index % 9 + 1);
        return json.getBytes(UTF_8);
    }
}
