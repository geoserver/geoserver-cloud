/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.secrets;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigObjectMapper;
import org.geoserver.cloud.security.secrets.StoreSecretsCipher;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Encrypts the secrets of a store when pgconfig writes the store to its row, and decrypts them when pgconfig reads the
 * row, as GeoServer does for the store files of a data directory. The rows then hold the values written by vanilla
 * GeoServer in its XML files, and the catalog returns the secrets in clear text. The values are encrypted and decrypted
 * by a {@link StoreSecretsCipher}.
 *
 * <p>A decryption costs 100 to 400 microseconds, 25 to 100 times the decoding of the store, and the encoders handle one
 * value at a time. The entity cache of pgconfig decodes a store on each lookup missing the cache of the request, and
 * the row mapper decrypts the JSON before it enters the entity cache: a cached store decodes with its secrets in clear
 * text, and a store is decrypted once per instance until a change evicts it.
 *
 * <p>Decryption needs the security manager, encryption the initialized security manager. Without it, a row is read and
 * written as it is, with a warning, see {@link #decrypt(String)}, {@link #decryptForCache(byte[])} and
 * {@link #encrypt(StoreInfo)}.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.secrets")
public class StoreSecrets {

    private static final String CONNECTION_PARAMETERS = "connectionParameters";
    private static final String PASSWORD = "password";

    private static final ObjectMapper MAPPER = PgconfigObjectMapper.newObjectMapper();

    private static final StoreSecrets NONE = new StoreSecrets(new StoreSecretsCipher(() -> null), false);

    private final StoreSecretsCipher cipher;
    private final boolean enabled;

    private final LongAdder decryptedValues = new LongAdder();
    private final LongAdder decryptNanos = new LongAdder();

    private final AtomicBoolean warnedReadNotReady = new AtomicBoolean();
    private final AtomicBoolean warnedWriteNotReady = new AtomicBoolean();

    public StoreSecrets(@NonNull StoreSecretsCipher cipher) {
        this(cipher, true);
    }

    private StoreSecrets(StoreSecretsCipher cipher, boolean enabled) {
        this.cipher = cipher;
        this.enabled = enabled;
    }

    /** Store secrets left as they are: rows are read and written without encryption. For tests and tools. */
    public static StoreSecrets none() {
        return NONE;
    }

    /** How many secret values were decrypted. */
    public long decryptedValues() {
        return decryptedValues.sum();
    }

    /** The time spent decrypting secret values, in nanoseconds. */
    public long decryptNanos() {
        return decryptNanos.sum();
    }

    /**
     * Returns the JSON of a store read from its row, with its secrets decrypted. Without a security manager, the JSON
     * is returned as it is, with a warning: the store then holds its secrets encrypted, as with vanilla GeoServer
     * reading a store without one.
     */
    public String decrypt(@NonNull String storeJson) {
        if (!enabled || !maySecretsBeEncrypted(storeJson)) {
            return storeJson;
        }
        if (!cipher.canDecrypt()) {
            warnOnce(warnedReadNotReady, "Store secrets read before the security manager exists are left encrypted");
            return storeJson;
        }
        ObjectNode tree = (ObjectNode) MAPPER.readTree(storeJson);
        boolean changed = decryptSecrets(tree);
        return changed ? MAPPER.writeValueAsString(tree) : storeJson;
    }

    /**
     * Returns the JSON of a store read from a row, with its secrets decrypted, for the entity cache. A store entering
     * the cache with encrypted secrets would keep them encrypted until a change evicts it: without a security manager,
     * a store with an encrypted secret throws {@link SecretsNotReadyException}, and the caller reads the row without
     * the cache.
     */
    public byte[] decryptForCache(byte @NonNull [] storeJson) {
        if (!enabled || !maySecretsBeEncrypted(storeJson)) {
            return storeJson;
        }
        if (!cipher.canDecrypt()) {
            throw new SecretsNotReadyException();
        }
        ObjectNode tree = (ObjectNode) MAPPER.readTree(storeJson);
        boolean changed = decryptSecrets(tree);
        return changed ? MAPPER.writeValueAsBytes(tree) : storeJson;
    }

    /**
     * Returns the JSON of a store for its row, with its secrets encrypted. The store is not modified. Before the
     * security manager is initialized, the secrets are written in clear text, with a warning, and the next save of the
     * store encrypts them.
     */
    public String encrypt(@NonNull StoreInfo store) {
        if (!enabled) {
            return MAPPER.writeValueAsString(store);
        }
        if (!cipher.canEncrypt()) {
            warnOnce(
                    warnedWriteNotReady,
                    "Store secrets written before the security manager is initialized"
                            + " are stored in clear text until the store is saved again");
            return MAPPER.writeValueAsString(store);
        }
        ObjectNode tree = MAPPER.valueToTree(store);
        encryptSecrets(store, tree);
        return MAPPER.writeValueAsString(tree);
    }

    /**
     * Decrypts the secrets of the JSON tree of a store. A value that cannot be decrypted is kept as it is, and the
     * store is disabled, as GeoServer's data directory loader does: a database restored without its keystore must not
     * make the listings of the catalog fail.
     *
     * @return whether the tree changed
     */
    private boolean decryptSecrets(ObjectNode tree) {
        StoreInfo store = MAPPER.treeToValue(tree, StoreInfo.class);
        boolean changed = false;
        for (SecretValue secret : secretValues(store, tree)) {
            changed |= decrypt(store, secret);
        }
        return changed;
    }

    private boolean decrypt(StoreInfo store, SecretValue secret) {
        String value = secret.value();
        if (!cipher.isEncrypted(value)) {
            return false;
        }
        Optional<String> decrypted = timedDecrypt(value);
        if (decrypted.isPresent()) {
            secret.set(decrypted.get());
        } else {
            log.error(
                    "Store {} ({}) is disabled: its {} cannot be decrypted with the keys of the keystore",
                    store.getName(),
                    store.getId(),
                    secret.name());
            secret.tree().put("enabled", false);
        }
        return true;
    }

    private Optional<String> timedDecrypt(String value) {
        long start = System.nanoTime();
        try {
            return cipher.decrypt(value);
        } finally {
            decryptNanos.add(System.nanoTime() - start);
            decryptedValues.increment();
        }
    }

    /**
     * Encrypts the secrets held in clear text in the JSON tree of a store. A value already encrypted is kept, with a
     * warning, where vanilla GeoServer fails the save.
     */
    private void encryptSecrets(StoreInfo store, ObjectNode tree) {
        for (SecretValue secret : secretValues(store, tree)) {
            encrypt(store, secret);
        }
    }

    private void encrypt(StoreInfo store, SecretValue secret) {
        String value = secret.value();
        if (null == value || value.isEmpty()) {
            return;
        }
        if (cipher.isEncrypted(value)) {
            log.warn("The {} of store {} is written as it is: it is already encrypted", secret.name(), store.getName());
            return;
        }
        cipher.encrypt(value).ifPresent(secret::set);
    }

    /**
     * The secret values of the JSON tree of a store: its secret connection parameters, and the password of an HTTP
     * store.
     */
    private List<SecretValue> secretValues(StoreInfo store, ObjectNode tree) {
        List<SecretValue> values = new ArrayList<>();
        JsonNode parameters = tree.get(CONNECTION_PARAMETERS);
        if (parameters instanceof ObjectNode parametersNode) {
            for (String parameter : cipher.secretParameters(store)) {
                values.add(new SecretValue(parametersNode, parameter));
            }
        }
        if (store instanceof HTTPStoreInfo) {
            values.add(new SecretValue(tree, PASSWORD));
        }
        return values;
    }

    /**
     * Whether the JSON may hold an encrypted value: whether a string in it starts with the prefix of a reversible
     * encoder. Most stores have none, and their JSON is not parsed.
     */
    private boolean maySecretsBeEncrypted(String json) {
        for (String start : encryptedStrings()) {
            if (json.contains(start)) {
                return true;
            }
        }
        return false;
    }

    private boolean maySecretsBeEncrypted(byte[] json) {
        for (String start : encryptedStrings()) {
            if (contains(json, start.getBytes(UTF_8))) {
                return true;
            }
        }
        return false;
    }

    /** The start of a JSON string holding an encrypted value: a quote and the prefix of an encoder. */
    private List<String> encryptedStrings() {
        return cipher.encryptedPrefixes().stream().map(prefix -> "\"" + prefix).toList();
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        int last = haystack.length - needle.length;
        for (int i = 0; i <= last; i++) {
            if (matchesAt(haystack, needle, i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(byte[] haystack, byte[] needle, int offset) {
        for (int j = 0; j < needle.length; j++) {
            if (haystack[offset + j] != needle[j]) {
                return false;
            }
        }
        return true;
    }

    private static void warnOnce(AtomicBoolean warned, String message) {
        if (warned.compareAndSet(false, true)) {
            log.warn(message);
        }
    }

    /** A secret field of the JSON tree of a store, and the object holding it. */
    private record SecretValue(ObjectNode tree, String name) {

        @Nullable
        String value() {
            JsonNode node = tree.get(name);
            return null != node && node.isString() ? node.asString() : null;
        }

        void set(String value) {
            tree.put(name, value);
        }
    }
}
