/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

/**
 * Raw data access API for GeoServer global configuration and per-workspace settings and services
 * configuration.
 *
 * <p>This is a null-free, DDD inspired "repository" API to provide storage and querying of plain
 * {@link Info} objects, with precise semantics. No method that receives an argument can receive
 * {@code null}. In cases where different semantics are required, semantically clear query methods
 * are provided. For example, {@link #getGlobalServices()} and {@link
 * #getServicesByWorkspace(WorkspaceInfo)} are self-explanatory.
 *
 * <p>All query methods that could return zero or one result, return {@link Optional}. All query
 * methods that could return zero or more results, return {@link Stream}.
 *
 * <p>Care shall be taken that {@code Stream} implements {@link AutoCloseable} and hence it is
 * expected for users of this api to properly close the received stream, may the implementation be
 * using live connections to some back-end storage and need to release resources.
 */
public interface ConfigRepository {

    /** The global geoserver configuration. */
    Optional<GeoServerInfo> getGlobal();

    /** Sets the global configuration, replacing the current one completely. */
    void setGlobal(GeoServerInfo global);

    /**
     * The settings configuration for the specified workspace, or {@code Optional.empty()} if non
     * exists.
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

    /** Saves logging configuration, replacing the current one comletely. */
    void setLogging(LoggingInfo logging);

    /** Adds a service to the configuration. */
    void add(ServiceInfo service);

    /** Removes a service from the configuration. */
    void remove(ServiceInfo service);

    /** Saves a service that has been modified. */
    void save(ServiceInfo service);

    /** GeoServer global services (not attached to any Workspace) */
    Stream<? extends ServiceInfo> getGlobalServices();

    /** GeoServer services specific to the specified workspace. */
    Stream<? extends ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace);

    /**
     * GeoServer global service filtered by type.
     *
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz);

    /**
     * GeoServer service specific to the specified workspace and filtered by type.
     *
     * @param workspace The workspace the service is specific to.
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> Optional<T> getServiceByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz);

    /**
     * Looks up a service by id.
     *
     * @param id The id of the service.
     * @param clazz The type of the service.
     * @return The service with the specified id, or {@code Optional.empty()} if no such service
     *     coud be found.
     */
    <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz);

    /**
     * Looks up a service by name and type.
     *
     * @param name The name of the service.
     * @param clazz The type of the service.
     * @return The service with the specified name or {@code Optional.empty()} if no such service
     *     could be found.
     */
    <T extends ServiceInfo> Optional<T> getServiceByName(String name, Class<T> clazz);

    /**
     * Looks up a service by name and type, specific to the specified workspace.
     *
     * @param name The name of the service.
     * @param workspaceId The workspace the service is specific to.
     * @param clazz The type of the service.
     * @return The service with the specified name or {@code Optional.empty()} if no such service
     *     could be found.
     */
    <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz);

    /** Disposes the repository, making it unusable after */
    void dispose();
}
