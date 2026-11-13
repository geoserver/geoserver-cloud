/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.geoserver.cloud.event.info.ConfigInfoType.GEOSERVER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LOGGING;
import static org.geoserver.cloud.event.info.ConfigInfoType.SERVICE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SETTINGS;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.jspecify.annotations.Nullable;

/**
 * The keys of configuration objects in the {@link EntityCache}: the global settings, the logging settings, the settings
 * of the workspaces and the services.
 *
 * <p>The configuration repository and the listener of remote events must build their keys here: keys built in two
 * places drift apart.
 */
public final class ConfigInfoKeys {

    private ConfigInfoKeys() {
        // static methods only
    }

    /** The key of the global settings. */
    public static CacheKey global() {
        return CacheKey.entity(GEOSERVER, "global");
    }

    /** The key of the logging settings. */
    public static CacheKey logging() {
        return CacheKey.entity(LOGGING, "logging");
    }

    /** The key of the settings of a workspace, by the id of the settings. */
    public static CacheKey settings(String id) {
        return CacheKey.entity(SETTINGS, id);
    }

    /** The key holding the id of the settings of a workspace. */
    public static CacheKey settingsByWorkspace(String workspaceId) {
        return CacheKey.lookup(SETTINGS, "settings:" + workspaceId);
    }

    /** The key of a service, by its id. */
    public static CacheKey service(String id) {
        return CacheKey.entity(SERVICE, id);
    }

    /**
     * The key holding the id of the service of a workspace looked up by class, such as {@code WMSInfo}.
     *
     * @param workspaceId the id of the workspace, or null for the global services
     */
    public static CacheKey serviceByWorkspaceAndClass(@Nullable String workspaceId, Class<?> serviceClass) {
        String workspace = null == workspaceId ? "global" : workspaceId;
        return CacheKey.lookup(SERVICE, "service:" + workspace + ":" + serviceClass.getSimpleName());
    }

    /**
     * The key holding the id of a service looked up by name alone. The lookup finds a global service, or the service of
     * a workspace.
     */
    public static CacheKey serviceByName(String name) {
        return CacheKey.lookup(SERVICE, "service-name:" + name);
    }

    /** The key holding the id of the service of a workspace, by its name. */
    public static CacheKey serviceByNameAndWorkspace(String workspaceId, String name) {
        return CacheKey.lookup(SERVICE, "service-name:" + workspaceId + ":" + name);
    }

    /**
     * Matches the keys of the settings of the workspaces and of the services, of each kind. The database deletes the
     * settings and the services of a removed workspace, and the event of the removal does not tell their ids.
     */
    public static Predicate<CacheKey> settingsAndServices() {
        return key -> key.type() == SETTINGS || key.type() == SERVICE;
    }

    /** Returns the keys of the settings of a workspace: their own key, and the key of their workspace. */
    public static List<CacheKey> keysOf(SettingsInfo settings) {
        List<CacheKey> keys = new ArrayList<>();
        keys.add(settings(settings.getId()));
        String workspaceId = idOf(settings.getWorkspace());
        if (null != workspaceId) {
            keys.add(settingsByWorkspace(workspaceId));
        }
        return keys;
    }

    /**
     * Returns the keys of a service: its own key, the keys of the lookups by class in its workspace, and the keys of
     * its name.
     *
     * <p>A lookup by class keeps the id of the service under the class asked by the caller. The service then has a key
     * for each of its classes of services, such as {@code WMSInfoImpl}, {@code WMSInfo} and {@code ServiceInfo}. A
     * {@link ModificationProxy} is unwrapped: its class is not the class of the service.
     */
    public static List<CacheKey> keysOf(ServiceInfo service) {
        ServiceInfo unwrapped = ModificationProxy.unwrap(service);
        String workspaceId = idOf(unwrapped.getWorkspace());
        List<CacheKey> keys = new ArrayList<>();
        keys.add(service(unwrapped.getId()));
        for (Class<?> serviceClass : serviceClasses(unwrapped.getClass())) {
            keys.add(serviceByWorkspaceAndClass(workspaceId, serviceClass));
        }
        keys.addAll(nameKeys(workspaceId, unwrapped.getName()));
        return keys;
    }

    // The lookup by name alone also finds the services of the workspaces
    private static List<CacheKey> nameKeys(@Nullable String workspaceId, @Nullable String name) {
        if (null == name) {
            return List.of();
        }
        if (null == workspaceId) {
            return List.of(serviceByName(name));
        }
        return List.of(serviceByName(name), serviceByNameAndWorkspace(workspaceId, name));
    }

    /** The class of a service, with its superclasses and interfaces of services. */
    private static Set<Class<?>> serviceClasses(Class<?> serviceClass) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        addServiceClasses(serviceClass, classes);
        return classes;
    }

    private static void addServiceClasses(@Nullable Class<?> type, Set<Class<?>> classes) {
        boolean serviceType = null != type && ServiceInfo.class.isAssignableFrom(type);
        if (serviceType && classes.add(type)) {
            addServiceClasses(type.getSuperclass(), classes);
            for (Class<?> implemented : type.getInterfaces()) {
                addServiceClasses(implemented, classes);
            }
        }
    }

    // An unresolved workspace is a proxy, and still tells its id
    private static @Nullable String idOf(@Nullable WorkspaceInfo workspace) {
        return null == workspace ? null : workspace.getId();
    }
}
