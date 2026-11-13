/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Change;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.ResourceNotification;
import org.springframework.context.event.EventListener;

/**
 * Applies a resource change made through another instance to this instance's caches, then tells the listeners.
 *
 * <p>The store applies its own changes once they commit. A change made through another instance arrives as a remote
 * {@link ResourceChanged} event. The listeners of this instance learn about it here: upstream's {@code ResourcePool}
 * listens to the resource of each parsed style, and drops the parsed style when told.
 */
@Slf4j
public class ResourceChangeListener {

    private final PgconfigResourceStore store;
    private final FileSystemResourceStoreCache files;

    public ResourceChangeListener(@NonNull PgconfigResourceStore store, @NonNull FileSystemResourceStoreCache files) {
        this.store = store;
        this.files = files;
    }

    /**
     * Refreshes the caches and tells the listeners of a remote change. The store handled a local one. Each step runs
     * even if the step before it failed: the removal of a local copy needs no database, and runs first.
     */
    @EventListener(ResourceChanged.class)
    public void onResourceChanged(ResourceChanged event) {
        if (event.isRemote()) {
            runGuarded("remove the local copy", event, () -> removeLocalCopy(event));
            runGuarded("refresh the index", event, () -> store.refresh(changedPaths(event)));
            runGuarded("notify the listeners", event, () -> notifyListeners(event));
        }
    }

    /**
     * Removes the local copy of a deleted resource. The local copy of a modified resource needs no action: its mtime
     * differs from the mtime of the refreshed row, and the next read of the file writes it again.
     */
    private void removeLocalCopy(ResourceChanged event) {
        if (event.getKind() == Kind.DELETED) {
            files.deleted(event.getPath());
        }
    }

    // The triggers set the mtime of the parent of the main path. The changes of a deletion are the deleted descendants
    private static Set<String> changedPaths(ResourceChanged event) {
        Set<String> paths = new LinkedHashSet<>();
        paths.add(event.getPath());
        String parent = Paths.parent(event.getPath());
        if (parent != null) {
            paths.add(parent);
        }
        for (Change change : event.getChanges()) {
            paths.add(change.getPath());
        }
        return paths;
    }

    private void notifyListeners(ResourceChanged event) {
        ResourceNotification notification = ResourceChangePublisher.toNotification(event);
        store.getResourceNotificationDispatcher().changed(notification);
    }

    // A failure must not stop the other listeners of the event, nor make the event bus deliver it again
    private static void runGuarded(String step, ResourceChanged event, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Could not {} upon {}", step, event.toShortString(), e);
        }
    }
}
