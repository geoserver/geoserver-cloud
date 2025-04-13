/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jackson.databind.catalog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Specialized map class for store connection parameters.
 *
 * <p>
 * This class extends LinkedHashMap to provide a concrete type for store connection parameters.
 * The specialized type allows us to register custom serializers and deserializers specifically
 * for connection parameters without affecting other Map&lt;String, Object&gt; instances.
 * </p>
 */
public class ConnectionParameters extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public ConnectionParameters() {
        super();
    }

    public ConnectionParameters(Map<String, ?> map) {
        super(map);
    }

    /**
     * Convert from a Map&lt;String, Serializable&gt; to ConnectionParameters.
     *
     * @param params the source map
     * @return a new ConnectionParameters instance
     */
    public static ConnectionParameters fromSerializableMap(Map<String, Serializable> params) {
        return (params == null) ? null : new ConnectionParameters(params);
    }

    /**
     * Convert to a Map&lt;String, Serializable&gt;.
     *
     * @return a new Map containing the same entries
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Serializable> toSerializableMap() {
        Map result = new HashMap<>();
        result.putAll(this);
        return result;
    }
}
