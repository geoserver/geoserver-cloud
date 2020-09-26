/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

public interface ConfigRepository {

    /** The global geoserver configuration. */
    Optional<GeoServerInfo> getGlobal();

    /** Sets the global configuration. */
    void setGlobal(GeoServerInfo global);

    /**
     * The settings configuration for the specified workspace, or <code>null</code> if non exists.
     */
    Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace);

    /** Adds a settings configuration for the specified workspace. */
    void add(SettingsInfo settings);

    /** Saves the settings configuration for the specified workspace. */
    void save(SettingsInfo settings);

    /** Removes the settings configuration for the specified workspace. */
    void remove(SettingsInfo settings);

    /** The logging configuration. */
    Optional<LoggingInfo> getLogging();

    /** Sets logging configuration. */
    void setLogging(LoggingInfo logging);

    /** Adds a service to the configuration. */
    void add(ServiceInfo service);

    /** Removes a service from the configuration. */
    void remove(ServiceInfo service);

    /** Saves a service that has been modified. */
    void save(ServiceInfo service);

    /** GeoServer services. */
    Stream<? extends ServiceInfo> getGlobalServices();

    /** GeoServer services specific to the specified workspace. */
    Stream<? extends ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace);

    /**
     * GeoServer global service filtered by class.
     *
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz);

    /**
     * GeoServer service specific to the specified workspace and filtered by class.
     *
     * @param workspaceId The workspace the service is specific to.
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> Optional<T> getServiceByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz);

    /**
     * Looks up a service by id.
     *
     * @param id The id of the service.
     * @param clazz The type of the service.
     * @return The service with the specified id, or <code>null</code> if no such service coud be
     *     found.
     */
    <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz);

    /**
     * Looks up a service by name.
     *
     * @param name The name of the service.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> Optional<T> getServiceByName(String name, Class<T> clazz);

    /**
     * Looks up a service by name, specific to the specified workspace.
     *
     * @param name The name of the service.
     * @param workspaceId The workspace the service is specific to.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz);

    /** Disposes the configuration, making this repository unusable after */
    void dispose();
}
