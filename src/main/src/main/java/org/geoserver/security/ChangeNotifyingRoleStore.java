/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Decorates a {@link GeoServerRoleStore} to run a callback once {@link #store()} saved pending changes.
 *
 * <p>Storing an unmodified store is a no-op upstream and stays silent here too.
 */
@RequiredArgsConstructor
class ChangeNotifyingRoleStore implements GeoServerRoleStore {

    @Delegate(excludes = Store.class)
    private final @NonNull GeoServerRoleStore delegate;

    private final @NonNull Runnable onStored;

    @Override
    public void store() throws IOException {
        boolean modified = delegate.isModified();
        delegate.store();
        if (modified) {
            onStored.run();
        }
    }

    /** Methods implemented by this decorator itself rather than delegated */
    private interface Store {
        void store() throws IOException;
    }
}
