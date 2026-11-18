/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.secrets;

/**
 * Thrown when the JSON of a store holds an encrypted secret and the security manager does not exist yet. The store must
 * not enter the entity cache: it would keep the secret encrypted once the security manager exists.
 */
public class SecretsNotReadyException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public SecretsNotReadyException() {
        super("The security manager does not exist yet: store secrets cannot be decrypted");
    }
}
