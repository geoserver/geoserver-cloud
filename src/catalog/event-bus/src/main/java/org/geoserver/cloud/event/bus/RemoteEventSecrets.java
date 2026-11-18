/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.event.bus;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Patch.Property;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoAdded;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.cloud.security.secrets.StoreSecretsCipher;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * Encrypts the secrets of stores in the events sent to the other service instances, and decrypts them in the events
 * received from them. The secrets travel encrypted as in the store files of a data directory, and the listeners of each
 * instance get them in clear text.
 *
 * <p>An event with store secrets is the event of a store added, or of a store modified with new connection parameters
 * or a new password. The event of a store added holds the store of the catalog: the event sent is a copy, encrypted
 * without changing the catalog. The instances share the keystore, and decrypt with the same keys.
 *
 * <p>The receiver decrypts the values encrypted by a known encoder, without looking up the secret parameters of the
 * store: the store of a modified event may not exist yet in the catalog of the receiver.
 */
@Slf4j(topic = "org.geoserver.cloud.event.bus")
class RemoteEventSecrets {

    private static final String CONNECTION_PARAMETERS = "connectionParameters";
    private static final String PASSWORD = "password";

    private static final Set<ConfigInfoType> STORE_TYPES = EnumSet.of(
            ConfigInfoType.DATASTORE, ConfigInfoType.COVERAGESTORE, ConfigInfoType.WMSSTORE, ConfigInfoType.WMTSSTORE);

    private final StoreSecretsCipher cipher;
    private final Catalog catalog;
    private final ObjectMapper mapper;

    /**
     * @param cipher encrypts and decrypts the secrets
     * @param catalog the raw catalog, holding the modified stores
     * @param mapper copies the events, configured as the mapper of the event bus
     */
    RemoteEventSecrets(@NonNull StoreSecretsCipher cipher, @NonNull Catalog catalog, @NonNull ObjectMapper mapper) {
        this.cipher = cipher;
        this.catalog = catalog;
        this.mapper = mapper;
    }

    /**
     * Returns the event to send for a local event: the event itself if it holds no store secrets, or a copy with its
     * secrets encrypted. Before the security manager is initialized, the secrets are sent as they are.
     */
    GeoServerEvent encrypt(@NonNull GeoServerEvent event) {
        Optional<StoreInfo> store = storeWithSecrets(event);
        if (store.isEmpty() || !cipher.canEncrypt()) {
            return event;
        }
        Set<String> secretParameters = cipher.secretParameters(store.get());
        GeoServerEvent copy = copy(event);
        transformSecrets(copy, secretParameters, this::encryptValue);
        return copy;
    }

    /**
     * Decrypts the store secrets of a received event, in place. A value that cannot be decrypted is kept as it is, with
     * an error.
     */
    void decrypt(@NonNull GeoServerEvent event) {
        if (!(event instanceof InfoEvent infoEvent) || !STORE_TYPES.contains(infoEvent.getObjectType())) {
            return;
        }
        if (!cipher.canDecrypt()) {
            log.warn("The secrets of store event {} are left as received: no security manager", event.toShortString());
            return;
        }
        UnaryOperator<Object> decryptor = value -> decryptValue(infoEvent, value);
        transformSecrets(event, null, decryptor);
    }

    /** The store holding secrets changed by the event, empty if the event changes none. */
    private Optional<StoreInfo> storeWithSecrets(GeoServerEvent event) {
        if (event instanceof InfoAdded<?> added && added.getObject() instanceof StoreInfo store) {
            return Optional.of(store);
        }
        if (event instanceof InfoModified modified
                && STORE_TYPES.contains(modified.getObjectType())
                && changesSecrets(modified.getPatch())) {
            return Optional.ofNullable(catalog.getStore(modified.getObjectId(), StoreInfo.class));
        }
        return Optional.empty();
    }

    private static boolean changesSecrets(Patch patch) {
        List<String> names = patch.getPropertyNames();
        return names.contains(CONNECTION_PARAMETERS) || names.contains(PASSWORD);
    }

    private GeoServerEvent copy(GeoServerEvent event) {
        byte[] json = mapper.writeValueAsBytes(event);
        GeoServerEvent copy = mapper.readValue(json, GeoServerEvent.class);
        copy.setOrigin(event.getOrigin());
        return copy;
    }

    /**
     * Replaces the secret values of the store or patch of an event.
     *
     * @param secretParameters the connection parameters to transform, or null for all of them
     */
    private void transformSecrets(
            GeoServerEvent event, @Nullable Set<String> secretParameters, UnaryOperator<Object> transform) {
        if (event instanceof InfoAdded<?> added && added.getObject() instanceof StoreInfo store) {
            transformStore(store, secretParameters, transform);
        } else if (event instanceof InfoModified modified) {
            modified.setPatch(transformPatch(modified.getPatch(), secretParameters, transform));
        }
    }

    private void transformStore(
            StoreInfo store, @Nullable Set<String> secretParameters, UnaryOperator<Object> transform) {
        Map<String, Serializable> parameters = store.getConnectionParameters();
        if (null != parameters) {
            parameters.replaceAll(
                    (name, value) -> isSecret(name, secretParameters) ? (Serializable) transform.apply(value) : value);
        }
        if (store instanceof HTTPStoreInfo httpStore) {
            httpStore.setPassword((String) transform.apply(httpStore.getPassword()));
        }
    }

    private Patch transformPatch(Patch patch, @Nullable Set<String> secretParameters, UnaryOperator<Object> transform) {
        Patch transformed = new Patch();
        for (Property property : patch.getPatches()) {
            Object value = property.getValue();
            if (CONNECTION_PARAMETERS.equals(property.getName()) && value instanceof Map<?, ?> parameters) {
                value = transformParameters(parameters, secretParameters, transform);
            } else if (PASSWORD.equals(property.getName())) {
                value = transform.apply(value);
            }
            transformed.add(property.getName(), value);
        }
        return transformed;
    }

    private Map<String, Object> transformParameters(
            Map<?, ?> parameters, @Nullable Set<String> secretParameters, UnaryOperator<Object> transform) {
        Map<String, Object> transformed = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            String name = String.valueOf(key);
            Object newValue = isSecret(name, secretParameters) ? transform.apply(value) : value;
            transformed.put(name, newValue);
        });
        return transformed;
    }

    private static boolean isSecret(String parameter, @Nullable Set<String> secretParameters) {
        return null == secretParameters || secretParameters.contains(parameter);
    }

    /** Encrypts a value in clear text. Other values, such as null or an encrypted string, are returned as they are. */
    private Object encryptValue(Object value) {
        if (!(value instanceof String clearText) || clearText.isEmpty() || cipher.isEncrypted(clearText)) {
            return value;
        }
        return cipher.encrypt(clearText).orElse(clearText);
    }

    /** Decrypts an encrypted value. Other values are returned as they are. */
    private Object decryptValue(InfoEvent event, Object value) {
        if (!(value instanceof String encrypted) || !cipher.isEncrypted(encrypted)) {
            return value;
        }
        Optional<String> clearText = cipher.decrypt(encrypted);
        if (clearText.isEmpty()) {
            log.error(
                    "A secret of store {} ({}) received from {} cannot be decrypted with the keys of the keystore",
                    event.getObjectName(),
                    event.getObjectId(),
                    event.getOrigin());
            return value;
        }
        return clearText.get();
    }
}
