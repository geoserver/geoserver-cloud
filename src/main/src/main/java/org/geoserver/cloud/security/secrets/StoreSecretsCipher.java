/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.security.secrets;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.cloud.event.security.SecurityManagerReloaded;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Encrypts and decrypts the secrets of stores, such as database passwords, as GeoServer does when it writes and reads
 * the store files of a data directory. The secrets are encrypted with the encoder named in the security settings, and
 * the keys of the keystore.
 *
 * <p>The encoders are created once and shared by the threads of the application. GeoServer's
 * {@link ConfigurationPasswordEncryptionHelper} creates new ones for each value, and the first use of a new encoder
 * takes about 30 milliseconds, where a reused one takes 0.1 to 0.4 milliseconds. A password encoder creates its cipher
 * on its first use, and that first use is not safe to share: the encoders are made to create their ciphers here, under
 * a lock, before any thread gets them. A reload of the security configuration makes the next use create them again,
 * with the keys of the keystore as they are then.
 *
 * <p>Decryption needs the security manager, encryption the initialized security manager. Callers check
 * {@link #canDecrypt()} and {@link #canEncrypt()} first.
 */
@Slf4j(topic = "org.geoserver.cloud.security.secrets")
public class StoreSecretsCipher {

    /** The prefixes of GeoServer's reversible encoders, used while the security manager is not available. */
    private static final List<String> DEFAULT_PREFIXES = withDelimiter(List.of("crypt1", "crypt2", "crypt3"));

    /** The value encrypted and decrypted by a new encoder before it is shared. */
    private static final String PROBE = "store-secrets";

    private final Supplier<@Nullable GeoServerSecurityManager> securityManagerLookup;

    private final AtomicReference<@Nullable GeoServerSecurityManager> securityManager = new AtomicReference<>();

    /** The start of the values encrypted by the reversible encoders of the application, fixed by its beans. */
    private final AtomicReference<@Nullable List<String>> prefixes = new AtomicReference<>();

    /** The encoders ready for use by any thread, or null until a thread creates them under {@link #encodersLock}. */
    private final AtomicReference<@Nullable List<GeoServerPasswordEncoder>> encoders = new AtomicReference<>();

    private final Object encodersLock = new Object();

    private final AtomicBoolean warnedNoEncrypter = new AtomicBoolean();

    /**
     * @param securityManager returns the security manager, or null while it does not exist yet. Once it returns one,
     *     the instance is kept.
     */
    public StoreSecretsCipher(@NonNull Supplier<@Nullable GeoServerSecurityManager> securityManager) {
        this.securityManagerLookup = securityManager;
    }

    /**
     * Returns a cipher looking up the security manager in the application context. The lookup returns null while the
     * security manager is being created: a store may be read while the catalog is loaded, before the security manager
     * exists.
     */
    public static StoreSecretsCipher of(@NonNull ObjectProvider<GeoServerSecurityManager> securityManager) {
        return new StoreSecretsCipher(() -> availableSecurityManager(securityManager));
    }

    private static @Nullable GeoServerSecurityManager availableSecurityManager(
            ObjectProvider<GeoServerSecurityManager> securityManager) {
        try {
            return securityManager.getIfAvailable();
        } catch (BeansException creationInProgress) {
            log.debug("The security manager is not available yet: {}", creationInProgress.getMessage());
            return null;
        }
    }

    /**
     * Creates the encoders once the application is ready, outside the requests reading stores. If they cannot be
     * created, the first request needing them tries again.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        GeoServerSecurityManager manager = securityManager();
        if (null == manager) {
            return;
        }
        try {
            encoders(manager);
        } catch (RuntimeException e) {
            log.warn("The encoders of the store secrets could not be created at startup: {}", e.getMessage());
        }
    }

    /**
     * Forgets the encoders. The next value decrypted or encrypted gets new ones, with the keys of the keystore as the
     * security manager has them then.
     */
    @EventListener(SecurityManagerReloaded.class)
    public void reset() {
        encoders.set(null);
    }

    /** Whether the security manager exists. */
    public boolean canDecrypt() {
        return null != securityManager();
    }

    /** Whether the security manager is initialized. */
    public boolean canEncrypt() {
        GeoServerSecurityManager manager = securityManager();
        return null != manager && manager.isInitialized();
    }

    /**
     * The names of the connection parameters of a store holding secrets: the parameters marked as passwords by the
     * factory of a data store, and the parameters declared by the {@code EncryptedFieldsProvider} beans for other
     * stores. The password of a WMS or WMTS store is a property of the store, not a connection parameter.
     */
    public Set<String> secretParameters(@NonNull StoreInfo store) {
        ConfigurationPasswordEncryptionHelper helper = requireSecurityManager().getConfigPasswordEncryptionHelper();
        return Set.copyOf(helper.getEncryptedFields(store));
    }

    /** Whether a value is encrypted: whether it starts with the prefix of a reversible encoder. */
    public boolean isEncrypted(@Nullable String value) {
        if (null == value) {
            return false;
        }
        return encryptedPrefixes().stream().anyMatch(value::startsWith);
    }

    /**
     * The start of an encrypted value: the prefix of an encoder and its delimiter. Callers look for them to skip the
     * stores without encrypted values. The prefixes need no keystore, and are available before the security manager is
     * initialized.
     */
    public List<String> encryptedPrefixes() {
        List<String> current = prefixes.get();
        if (null == current) {
            current = loadPrefixes();
        }
        return current;
    }

    private List<String> loadPrefixes() {
        GeoServerSecurityManager manager = securityManager();
        if (null == manager) {
            return DEFAULT_PREFIXES;
        }
        List<String> loaded = withDelimiter(reversibleEncoders(manager).stream()
                .map(GeoServerPasswordEncoder::getPrefix)
                .toList());
        prefixes.set(loaded);
        return loaded;
    }

    private static List<String> withDelimiter(List<String> prefixes) {
        return prefixes.stream()
                .map(prefix -> prefix + GeoServerPasswordEncoder.PREFIX_DELIMTER)
                .toList();
    }

    /**
     * Decrypts a value. On a failure, the encoders are created again and the decryption tried once more: the keystore
     * may have changed since they were created, for example with a new master password.
     *
     * @return the value in clear text, or empty if it cannot be decrypted with the keys of the keystore
     */
    public Optional<String> decrypt(@NonNull String encrypted) {
        GeoServerSecurityManager manager = requireSecurityManager();
        try {
            return Optional.of(decryptWithEncoders(manager, encrypted));
        } catch (RuntimeException _) {
            reset();
            try {
                return Optional.of(decryptWithEncoders(manager, encrypted));
            } catch (RuntimeException secondFailure) {
                log.debug("Decryption failed", secondFailure);
                return Optional.empty();
            }
        }
    }

    private String decryptWithEncoders(GeoServerSecurityManager manager, String value) {
        GeoServerPasswordEncoder encoder = responsibleEncoder(manager, value).orElseThrow();
        return encoder.decode(value);
    }

    /**
     * Encrypts a value with the encoder named in the security settings.
     *
     * @return the encrypted value, or empty if the security settings name no encoder
     */
    public Optional<String> encrypt(@NonNull String clearText) {
        GeoServerSecurityManager manager = requireSecurityManager();
        return configEncoder(manager).map(encoder -> encoder.encodePassword(clearText, null));
    }

    /** The encoder named by the security settings to encrypt configuration passwords, empty if none is. */
    private Optional<GeoServerPasswordEncoder> configEncoder(GeoServerSecurityManager manager) {
        String name = manager.getSecurityConfig().getConfigPasswordEncrypterName();
        if (null == name) {
            if (warnedNoEncrypter.compareAndSet(false, true)) {
                log.warn("Store secrets are left in clear text: no password encrypter is configured");
            }
            return Optional.empty();
        }
        Optional<GeoServerPasswordEncoder> encoder = encoderNamed(encoders(manager), name);
        if (encoder.isEmpty()) {
            reset();
            encoder = encoderNamed(encoders(manager), name);
        }
        return encoder;
    }

    private static Optional<GeoServerPasswordEncoder> encoderNamed(
            List<GeoServerPasswordEncoder> encoders, String name) {
        return encoders.stream().filter(e -> name.equals(e.getName())).findFirst();
    }

    private Optional<GeoServerPasswordEncoder> responsibleEncoder(GeoServerSecurityManager manager, String value) {
        return encoders(manager).stream()
                .filter(encoder -> encoder.isResponsibleForEncoding(value))
                .findFirst();
    }

    /**
     * The shared encoders. One thread creates them, and the others wait for it: an encoder is shared only once it has
     * created its cipher, see {@link #initialize(GeoServerPasswordEncoder)}.
     */
    private List<GeoServerPasswordEncoder> encoders(GeoServerSecurityManager manager) {
        List<GeoServerPasswordEncoder> current = encoders.get();
        if (null == current) {
            synchronized (encodersLock) {
                current = encoders.get();
                if (null == current) {
                    current = createEncoders(manager);
                    encoders.set(current);
                }
            }
        }
        return current;
    }

    private static List<GeoServerPasswordEncoder> createEncoders(GeoServerSecurityManager manager) {
        List<GeoServerPasswordEncoder> created = reversibleEncoders(manager);
        created.forEach(StoreSecretsCipher::initialize);
        return created;
    }

    /** New instances of the reversible encoder beans, with the security manager set. */
    private static List<GeoServerPasswordEncoder> reversibleEncoders(GeoServerSecurityManager manager) {
        return List.copyOf(manager.loadPasswordEncoders(null, true, null));
    }

    /**
     * Makes a new encoder create its cipher, by encrypting and decrypting one value with the keys of the keystore. A
     * password encoder creates its cipher on its first use, and sets the key and the algorithm on a cipher already
     * visible to the other threads. A second thread decrypting at that moment uses the cipher before its key is set,
     * and the first thread fails to set them.
     *
     * @throws IllegalStateException if the encoder does not decrypt what it encrypted
     */
    private static void initialize(GeoServerPasswordEncoder encoder) {
        String encrypted = encoder.encodePassword(PROBE, null);
        String decrypted = encoder.decode(encrypted);
        if (!PROBE.equals(decrypted)) {
            throw new IllegalStateException(
                    "Password encoder %s does not decrypt what it encrypted".formatted(encoder.getName()));
        }
    }

    private GeoServerSecurityManager requireSecurityManager() {
        GeoServerSecurityManager manager = securityManager();
        if (null == manager) {
            throw new IllegalStateException("The security manager does not exist yet");
        }
        return manager;
    }

    private @Nullable GeoServerSecurityManager securityManager() {
        GeoServerSecurityManager manager = securityManager.get();
        if (null == manager) {
            manager = securityManagerLookup.get();
            securityManager.set(manager);
        }
        return manager;
    }
}
