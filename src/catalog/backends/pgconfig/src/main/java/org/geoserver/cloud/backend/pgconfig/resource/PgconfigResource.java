/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

/**
 * @since 1.4
 */
@EqualsAndHashCode(exclude = {"store", "lastChecked"})
class PgconfigResource implements Resource {

    @Getter
    long id;

    @Getter
    long parentId;

    Resource.Type type;

    String path;

    long lastmodified;

    private PgconfigResourceStore store;

    Instant lastChecked;

    PgconfigResource(
            @NonNull PgconfigResourceStore store,
            long id,
            long parentId,
            @NonNull Resource.Type type,
            @NonNull String path,
            long lastmodified) {
        this.store = store;
        this.id = id;
        this.parentId = parentId;
        this.type = type;
        this.path = path;
        this.lastmodified = lastmodified;
        this.lastChecked = Instant.now();
    }

    /**
     * Factory method to create an undefined resource.
     *
     * <p>
     * Creates a PgconfigResource with Type.UNDEFINED and UNDEFINED_ID for both
     * id and parentId. This is used when a resource doesn't exist in the database.
     * </p>
     *
     * @param store the resource store
     * @param path the path for the undefined resource
     * @return a new PgconfigResource instance with Type.UNDEFINED
     */
    static PgconfigResource undefined(@NonNull PgconfigResourceStore store, @NonNull String path) {
        return new PgconfigResource(
                store,
                PgconfigResourceStore.UNDEFINED_ID,
                PgconfigResourceStore.UNDEFINED_ID,
                Type.UNDEFINED,
                path,
                0L);
    }

    /**
     * Copies all state from another PgconfigResource into this one.
     *
     * <p>
     * This is used by the {@link PgconfigResourceStore#updateState} method to update a resource
     * with the latest information from the database. It's particularly important for maintaining
     * consistency of long-lived resource references that are held by other components.
     * </p>
     *
     * @param other the resource whose state should be copied to this resource
     * @see PgconfigResourceStore#updateState
     */
    void reset(PgconfigResource other) {
        this.id = other.id;
        this.parentId = other.parentId;
        this.type = other.type;
        this.path = other.path;
        this.lastmodified = other.lastmodified;
        this.lastChecked = Instant.now();
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String name() {
        return Paths.name(path);
    }

    @Override
    public InputStream in() {
        byte[] contents = store.contents(this);
        return new ByteArrayInputStream(contents);
    }

    @Override
    public OutputStream out() {
        return store.out(this);
    }

    @Override
    public Lock lock() {
        return store.getLockProvider().acquire(path());
    }

    @Override
    public File file() {
        return store.asFile(this);
    }

    @Override
    public File dir() {
        return store.asDir(this);
    }

    @Override
    public PgconfigResource parent() {
        return store.getParent(this);
    }

    @Override
    public Resource get(@NonNull String childPath) {
        if ("".equals(childPath)) {
            return this;
        }
        String resourcePath = Paths.path(path(), childPath);
        return store.get(resourcePath);
    }

    @Override
    public List<Resource> list() {
        return store.list(this);
    }

    @Override
    public Type getType() {
        updateState();
        return type;
    }

    @Override
    public long lastmodified() {
        updateState();
        return lastmodified;
    }

    /**
     * Updates the resource state if it's been held for longer than a threshold.
     *
     * <p>
     * This method is critical for compatibility with GeoServer components that hold
     * resource references as instance variables, such as AbstractAccessRuleDAO and RESTAccessRuleDAO.
     * Since pgconfig resources are backed by database entries, long-lived references can become stale.
     * </p>
     *
     * <p>
     * By refreshing the state periodically when getType() or lastmodified() are called,
     * we ensure that these long-lived references remain valid even if the underlying resource
     * has been modified in the database by another process or service instance.
     * </p>
     *
     * <p>
     * This prevents issues such as 403 errors when REST resources appear to be missing
     * because a stale resource reference doesn't reflect the current state in the database.
     * </p>
     *
     * @see #getType()
     * @see #lastmodified()
     */
    private void updateState() {
        if (lastChecked.plusMillis(500).isBefore(Instant.now())) {
            store.updateState(this);
            lastChecked = Instant.now();
        }
    }

    @Override
    public boolean delete() {
        return store.delete(this);
    }

    @Override
    public boolean renameTo(@NonNull Resource dest) {
        return store.move(this, ((PgconfigResource) dest));
    }

    @Override
    public void addListener(ResourceListener listener) {
        // no-op
    }

    @Override
    public void removeListener(ResourceListener listener) {
        // no-op
    }

    /**
     * Returns a string representation of this resource, which is its path.
     *
     * @return the path of this resource
     */
    @Override
    public String toString() {
        return path;
    }

    /**
     * Gets the path of the parent resource.
     *
     * <p>
     * This is a convenience method that calls {@link Paths#parent(String)}
     * on the result of {@link #path()}.
     * </p>
     *
     * @return the parent path, or null if this is the root resource
     */
    public String parentPath() {
        return Paths.parent(path());
    }

    /**
     * Creates this resource as a directory, including any necessary parent directories.
     *
     * <p>
     * This is a convenience method that delegates to {@link PgconfigResourceStore#mkdirs(PgconfigResource)}.
     * </p>
     *
     * @return this resource, updated with the database ID of the created directory
     */
    public PgconfigResource mkdirs() {
        return store.mkdirs(this);
    }

    /**
     * Checks if this resource exists in the database.
     *
     * <p>
     * A resource exists if its ID is not {@link PgconfigResourceStore#UNDEFINED_ID}.
     * </p>
     *
     * @return true if the resource exists, false otherwise
     */
    public boolean exists() {
        return id != PgconfigResourceStore.UNDEFINED_ID;
    }

    /**
     * Checks if this resource is a file.
     *
     * <p>
     * A resource is a file if its type is {@link Type#RESOURCE}.
     * </p>
     *
     * @return true if the resource is a file, false otherwise
     */
    public boolean isFile() {
        return getType() == Type.RESOURCE;
    }

    /**
     * Checks if this resource is a directory.
     *
     * <p>
     * A resource is a directory if its type is {@link Type#DIRECTORY}.
     * </p>
     *
     * @return true if the resource is a directory, false otherwise
     */
    public boolean isDirectory() {
        return getType() == Type.DIRECTORY;
    }

    /**
     * Checks if this resource is undefined.
     *
     * <p>
     * A resource is undefined if its type is {@link Type#UNDEFINED},
     * which typically means it doesn't exist in the database.
     * </p>
     *
     * @return true if the resource is undefined, false otherwise
     */
    public boolean isUndefined() {
        return getType() == Type.UNDEFINED;
    }
}
