/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import java.io.Serializable;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ServiceInfo;

/** */
record ServiceInfoKey(String key, String qualifier) implements Serializable {

    public static ServiceInfoKey byId(String id) {
        return new ServiceInfoKey(id, null);
    }

    public static ServiceInfoKey byName(WorkspaceInfo ws, String name) {
        String wsId = ws == null ? null : ws.getId();
        return new ServiceInfoKey(wsId, name);
    }

    @SuppressWarnings("unchecked")
    public static ServiceInfoKey byType(WorkspaceInfo ws, Class<? extends ServiceInfo> clazz) {
        String wsId = ws == null ? null : ws.getId();
        String typeName;
        if (clazz.isInterface()) typeName = clazz.getCanonicalName();
        else {
            Class<?>[] interfaces = clazz.getInterfaces();
            Class<? extends ServiceInfo> mostConcrete = ServiceInfo.class;
            for (Class<?> i : interfaces) {
                if (ServiceInfo.class.isAssignableFrom(i) && mostConcrete.isAssignableFrom(i)) {
                    mostConcrete = (Class<? extends ServiceInfo>) i;
                }
            }
            typeName = mostConcrete.getCanonicalName();
        }

        return new ServiceInfoKey(wsId, typeName);
    }
}
