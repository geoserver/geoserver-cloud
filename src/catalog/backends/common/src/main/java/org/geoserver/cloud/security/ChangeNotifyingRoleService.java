/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security;

import java.io.IOException;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;

/**
 * Decorates a {@link GeoServerRoleService} to have every store created through it announce the changes saved through
 * that store.
 *
 * <p>Every other method is delegated as-is; only {@link #createStore()} is intercepted, to wrap the returned store in a
 * {@link ChangeNotifyingRoleStore}.
 */
@RequiredArgsConstructor
class ChangeNotifyingRoleService implements GeoServerRoleService {

    @Delegate(excludes = StoreFactory.class)
    private final @NonNull GeoServerRoleService delegate;

    /** Called with the service name once a store created by this service saved its changes */
    private final @NonNull Consumer<String> onStored;

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        GeoServerRoleStore store = delegate.createStore();
        if (store == null) {
            return null;
        }
        return new ChangeNotifyingRoleStore(store, () -> onStored.accept(delegate.getName()));
    }

    /** Methods implemented by this decorator itself rather than delegated */
    private interface StoreFactory {
        GeoServerRoleStore createStore() throws IOException;
    }
}
