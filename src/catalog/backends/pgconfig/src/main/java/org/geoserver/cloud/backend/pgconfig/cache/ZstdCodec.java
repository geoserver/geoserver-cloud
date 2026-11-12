/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.github.luben.zstd.ZstdDictCompress;
import com.github.luben.zstd.ZstdDictDecompress;
import com.github.luben.zstd.ZstdException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import lombok.NonNull;

/**
 * Compresses the JSON of catalog objects with zstd and a dictionary.
 *
 * <p>The bundled dictionary holds JSON fragments common to catalog objects, and was trained by
 * {@code ZstdDictionaryTrainer} (test sources) on 100 objects of each type from three synthetic catalogs. Bytes encoded
 * with one dictionary cannot be decoded with another, and that is harmless because the cache lives in one process.
 *
 * <p>The bytes are a standard zstd frame with the dictionary id and the content size, without checksum. The dictionary
 * id lets decoding reject the frame of another dictionary, and the content size gives the size of the output. Decoding
 * rejects a content size above 64 MiB before allocating the output.
 *
 * <p>Compression and decompression contexts come from two pools, {@code ContextPool}, and share the dictionary loaded
 * by the constructor. Contexts are not thread-safe, and creating one per call made decoding about 10% slower. Each pool
 * keeps at most twice as many idle contexts as there are processors.
 */
public final class ZstdCodec implements EntityCodec, AutoCloseable {

    /** The compression level for the catalog cache, also the default level of zstd. */
    public static final int DEFAULT_LEVEL = 3;

    private static final String BUNDLED_DICTIONARY = "catalog.zstd-dict";

    private static final int MAX_IDLE_CONTEXTS = 2 * Runtime.getRuntime().availableProcessors();

    // The JSON of a catalog object is far smaller. A larger size comes from a corrupt or foreign frame, and allocating
    // it could exhaust the heap.
    private static final int MAX_CONTENT_SIZE = 64 * 1024 * 1024;

    private final long dictionaryId;
    private final ContextPool<ZstdCompressCtx> compressContexts;
    private final ContextPool<ZstdDecompressCtx> decompressContexts;

    /**
     * @param level the zstd compression level
     * @param dictionary a dictionary trained by zstd, such as the bundled one
     * @throws IllegalArgumentException if the dictionary has no zstd dictionary id
     */
    public ZstdCodec(int level, @NonNull byte[] dictionary) {
        this.dictionaryId = requireDictionaryId(dictionary);
        ZstdDictCompress compressDictionary = new ZstdDictCompress(dictionary, level);
        ZstdDictDecompress decompressDictionary = new ZstdDictDecompress(dictionary);
        this.compressContexts = new ContextPool<>(
                MAX_IDLE_CONTEXTS, () -> newCompressContext(compressDictionary), ZstdCompressCtx::close);
        this.decompressContexts = new ContextPool<>(
                MAX_IDLE_CONTEXTS, () -> newDecompressContext(decompressDictionary), ZstdDecompressCtx::close);
    }

    // Without a dictionary id the frames have none either, and another dictionary could decode them to garbage
    private static long requireDictionaryId(byte[] dictionary) {
        long id = Zstd.getDictIdFromDict(dictionary);
        if (id == 0) {
            throw new IllegalArgumentException("Not a dictionary trained by zstd: it has no dictionary id");
        }
        return id;
    }

    private static ZstdCompressCtx newCompressContext(ZstdDictCompress dictionary) {
        ZstdCompressCtx context = new ZstdCompressCtx();
        context.setDictID(true);
        context.setContentSize(true);
        context.setChecksum(false);
        context.loadDict(dictionary);
        return context;
    }

    private static ZstdDecompressCtx newDecompressContext(ZstdDictDecompress dictionary) {
        ZstdDecompressCtx context = new ZstdDecompressCtx();
        context.loadDict(dictionary);
        return context;
    }

    /** Creates a codec with the dictionary bundled with this module. */
    public static ZstdCodec withBundledDictionary(int level) {
        byte[] dictionary = readBundledDictionary();
        return new ZstdCodec(level, dictionary);
    }

    private static byte[] readBundledDictionary() {
        try (InputStream in = ZstdCodec.class.getResourceAsStream(BUNDLED_DICTIONARY)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Missing resource %s next to %s".formatted(BUNDLED_DICTIONARY, ZstdCodec.class.getName()));
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] encode(byte[] json) {
        ZstdCompressCtx context = compressContexts.acquire();
        try {
            return context.compress(json);
        } finally {
            compressContexts.release(context);
        }
    }

    @Override
    public byte[] decode(byte[] stored) {
        int contentSize = contentSize(stored);
        ZstdDecompressCtx context = decompressContexts.acquire();
        try {
            return context.decompress(stored, contentSize);
        } catch (ZstdException e) {
            throw cannotDecode(stored, e);
        } finally {
            decompressContexts.release(context);
        }
    }

    private static int contentSize(byte[] stored) {
        long size = frameContentSize(stored);
        if (size < 0 || size > MAX_CONTENT_SIZE) {
            throw notAFrame(stored, size);
        }
        return (int) size;
    }

    // Zstd.getFrameContentSize throws on an empty array, and returns a negative error code for the other non-frames
    private static long frameContentSize(byte[] stored) {
        if (stored.length == 0) {
            return -1;
        }
        return Zstd.getFrameContentSize(stored);
    }

    private static IllegalArgumentException notAFrame(byte[] stored, long contentSize) {
        String message = "Not a zstd frame with a content size up to %d bytes: %d bytes, content size %d"
                .formatted(MAX_CONTENT_SIZE, stored.length, contentSize);
        return new IllegalArgumentException(message);
    }

    private IllegalArgumentException cannotDecode(byte[] stored, ZstdException cause) {
        String message = "Cannot decode %d bytes with zstd dictionary %d: %s"
                .formatted(stored.length, dictionaryId, cause.getMessage());
        return new IllegalArgumentException(message, cause);
    }

    @Override
    public String name() {
        return "zstd";
    }

    /** Closes the idle contexts of both pools, at shutdown. */
    @Override
    public void close() {
        compressContexts.close();
        decompressContexts.close();
    }
}
