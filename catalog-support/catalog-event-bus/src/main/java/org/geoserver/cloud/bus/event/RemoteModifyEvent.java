/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.PropertyDiff;

@EqualsAndHashCode(callSuper = true)
// @JsonIgnoreProperties(value = {"object", "diff", "payloadCodec"})
public abstract class RemoteModifyEvent<S, I extends Info> extends RemoteInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    @JsonIgnore private PropertyDiff diff;

    private String serializedDiff;

    protected RemoteModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected RemoteModifyEvent(
            S source,
            @NonNull I object,
            @NonNull PropertyDiff diff,
            String originService,
            String destinationService) {
        super(source, object, originService, destinationService);
        this.diff = diff;
    }

    protected @PostConstruct void encodePayload() {
        super.encodePayload();
        if (payloadCodec.isIncludeDiff() && diff != null) {
            try {
                this.serializedDiff = super.payloadCodec.encode(diff);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.diff = null;
        }
    }

    @JsonInclude
    public String getSerializedDiff() {
        return serializedDiff;
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, diff: %s, event id: %s, from: %s, to: %s, ts: %d)",
                getClass().getSimpleName(),
                getInfoType(),
                this.getObjectId(),
                serializedDiff,
                super.getId(),
                super.getOriginService(),
                super.getDestinationService(),
                super.getTimestamp());
    }

    public Optional<PropertyDiff> diff() {
        return Optional.ofNullable(resolveDiff());
    }

    private PropertyDiff resolveDiff() {
        if (this.diff == null && this.serializedDiff != null && this.payloadCodec != null) {
            try {
                this.diff = super.payloadCodec.decode(serializedDiff);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this.diff;
    }
}
