/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.Info;

public abstract class LocalModifyEvent<S, I extends Info> extends LocalInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    private @Getter PropertyDiff diff;

    protected LocalModifyEvent(
            S source,
            @NonNull I object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        this(source, object, PropertyDiff.valueOf(propertyNames, oldValues, newValues));
    }

    protected LocalModifyEvent(S source, @NonNull I object, @NonNull PropertyDiff diff) {
        super(source, object);
        this.diff = diff;
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, diff: %s, source: %s]",
                getClass().getSimpleName(),
                ConfigInfoInfoType.valueOf(getObject()),
                getObject().getId(),
                getDiff(),
                getSource() == null ? null : getSource().getClass().getSimpleName());
    }
}
