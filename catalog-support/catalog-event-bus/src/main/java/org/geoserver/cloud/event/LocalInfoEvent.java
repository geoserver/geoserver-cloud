/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event;

import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.springframework.context.ApplicationEvent;

public abstract class LocalInfoEvent<S, I extends Info> extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    private @Getter I object;

    public LocalInfoEvent(S source, @NonNull I object) {
        super(source);
        this.object = object;
    }

    @SuppressWarnings("unchecked")
    public @Override S getSource() {
        return (S) super.getSource();
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, source: %s)",
                getClass().getSimpleName(),
                ConfigInfoInfoType.valueOf(object),
                getObject().getId(),
                getSource() == null ? null : getSource().getClass().getSimpleName());
    }
}
