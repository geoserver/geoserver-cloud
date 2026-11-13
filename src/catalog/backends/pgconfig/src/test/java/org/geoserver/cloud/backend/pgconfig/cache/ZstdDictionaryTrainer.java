/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictTrainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Trains the zstd dictionary bundled with {@link ZstdCodec} on the catalogs of one or more pgconfig databases.
 *
 * <p>It reads the first 100 objects of each type, by id, and trains an 8 KiB dictionary on them. The samples alternate
 * between object types, because zstd left the last type out of a dictionary trained on samples grouped by type, and
 * that type compressed 2.8 times instead of 5.3. The same rows give the same dictionary. A dictionary keeps raw
 * fragments of its samples. Train it on synthetic catalogs only, never on a real one.
 *
 * <p>Run it from the repository root. The {@code currentSchema} parameter names the schema of the pgconfig tables when
 * it is not {@code public}:
 *
 * <pre>{@code
 * ./mvnw -q -pl src/catalog/backends/pgconfig test-compile exec:java -Dexec.classpathScope=test \
 *   -Dexec.mainClass=org.geoserver.cloud.backend.pgconfig.cache.ZstdDictionaryTrainer \
 *   -Dexec.args="jdbc:postgresql://host:5432/db1?currentSchema=pgconfig \
 *     jdbc:postgresql://host:5432/db2?currentSchema=pgconfig user password \
 *     src/catalog/backends/pgconfig/src/main/resources/org/geoserver/cloud/backend/pgconfig/cache/catalog.zstd-dict"
 * }</pre>
 */
public class ZstdDictionaryTrainer {

    private static final int DICTIONARY_BYTES = 8 * 1024;

    private static final List<String> SAMPLE_QUERIES = List.of(
            "SELECT info::text FROM workspaceinfo ORDER BY id LIMIT 100",
            "SELECT info::text FROM namespaceinfo ORDER BY id LIMIT 100",
            "SELECT info::text FROM storeinfo ORDER BY id LIMIT 100",
            "SELECT info::text FROM resourceinfo ORDER BY id LIMIT 100",
            "SELECT info::text FROM styleinfo ORDER BY id LIMIT 100",
            "SELECT publishedinfo::text FROM publishedinfos_mat WHERE \"@type\" = 'LayerInfo' ORDER BY id LIMIT 100",
            "SELECT publishedinfo::text FROM publishedinfos_mat WHERE \"@type\" = 'LayerGroupInfo' ORDER BY id LIMIT 100");

    public static void main(String[] args) throws SQLException, IOException {
        Arguments arguments = Arguments.parse(args);
        List<byte[]> samples = readInterleavedSamples(arguments);
        byte[] dictionary = train(samples);
        Files.write(arguments.output(), dictionary);
        printSummary(dictionary, samples, arguments.output());
    }

    record Arguments(List<String> jdbcUrls, String user, String password, Path output) {

        static Arguments parse(String[] args) {
            if (args.length < 4) {
                throw new IllegalArgumentException(
                        "Usage: ZstdDictionaryTrainer <jdbc-url>... <user> <password> <output-file>");
            }
            int last = args.length - 1;
            List<String> jdbcUrls = Arrays.asList(args).subList(0, last - 2);
            return new Arguments(jdbcUrls, args[last - 2], args[last - 1], Path.of(args[last]));
        }
    }

    private static List<byte[]> readInterleavedSamples(Arguments arguments) throws SQLException {
        List<List<byte[]>> samplesByType = new ArrayList<>();
        for (String jdbcUrl : arguments.jdbcUrls()) {
            samplesByType.addAll(readSamplesByType(jdbcUrl, arguments.user(), arguments.password()));
        }
        return interleave(samplesByType);
    }

    private static List<List<byte[]>> readSamplesByType(String jdbcUrl, String user, String password)
            throws SQLException {
        List<List<byte[]>> samplesByType = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            connection.setReadOnly(true);
            for (String query : SAMPLE_QUERIES) {
                samplesByType.add(readJsonColumn(connection, query));
            }
        }
        return samplesByType;
    }

    private static List<byte[]> readJsonColumn(Connection connection, String query) throws SQLException {
        List<byte[]> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String json = resultSet.getString(1);
                rows.add(json.getBytes(UTF_8));
            }
        }
        return rows;
    }

    // zstd builds the dictionary from the first three quarters of the samples, and tunes it on the last quarter.
    // Interleaving puts each object type in both parts.
    private static List<byte[]> interleave(List<List<byte[]>> samplesByType) {
        List<byte[]> interleaved = new ArrayList<>();
        int rows = longestSize(samplesByType);
        for (int row = 0; row < rows; row++) {
            for (List<byte[]> samples : samplesByType) {
                addIfPresent(samples, row, interleaved);
            }
        }
        return interleaved;
    }

    private static int longestSize(List<List<byte[]>> lists) {
        int longest = 0;
        for (List<byte[]> list : lists) {
            longest = Math.max(longest, list.size());
        }
        return longest;
    }

    private static void addIfPresent(List<byte[]> samples, int row, List<byte[]> target) {
        if (row < samples.size()) {
            target.add(samples.get(row));
        }
    }

    private static byte[] train(List<byte[]> samples) {
        ZstdDictTrainer trainer = new ZstdDictTrainer(totalBytes(samples), DICTIONARY_BYTES, ZstdCodec.DEFAULT_LEVEL);
        for (byte[] sample : samples) {
            trainer.addSample(sample);
        }
        return trainer.trainSamples();
    }

    private static int totalBytes(List<byte[]> samples) {
        int total = 0;
        for (byte[] sample : samples) {
            total += sample.length;
        }
        return total;
    }

    private static void printSummary(byte[] dictionary, List<byte[]> samples, Path output) {
        long dictionaryId = Zstd.getDictIdFromDict(dictionary);
        System.out.printf(
                "Wrote %d bytes, dictionary id %d, trained on %d samples, to %s%n",
                dictionary.length, dictionaryId, samples.size(), output);
    }
}
