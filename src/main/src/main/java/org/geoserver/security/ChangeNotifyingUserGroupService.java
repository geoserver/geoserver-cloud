/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Decorates a {@link GeoServerUserGroupService} to have every store created through it announce the changes saved
 * through that store.
 *
 * <p>Every other method is delegated as-is; only {@link #createStore()} is intercepted, to wrap the returned store in a
 * {@link ChangeNotifyingUserGroupStore}.
 */
@RequiredArgsConstructor
class ChangeNotifyingUserGroupService implements GeoServerUserGroupService {

    @Delegate(excludes = StoreFactory.class)
    private final @NonNull GeoServerUserGroupService delegate;

    /** Called with the service name once a store created by this service saved its changes */
    private final @NonNull Consumer<String> onStored;

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        GeoServerUserGroupStore store = delegate.createStore();
        if (store == null) {
            return null;
        }
        return new ChangeNotifyingUserGroupStore(store, () -> onStored.accept(delegate.getName()));
    }

    /** Methods implemented by this decorator itself rather than delegated */
    private interface StoreFactory {
        GeoServerUserGroupStore createStore() throws IOException;
    }
}
