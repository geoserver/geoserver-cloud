/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event;

import lombok.NonNull;
import org.geoserver.catalog.Info;

public abstract class LocalRemoveEvent<S, I extends Info> extends LocalInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    public LocalRemoveEvent(S source, @NonNull I object) {
        super(source, object);
    }
}
