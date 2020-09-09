/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class RemoteInfoEvent<S, I extends Info> extends RemoteApplicationEvent {
    private static final long serialVersionUID = 1L;

    @Setter(value = AccessLevel.PACKAGE)
    protected @Autowired RemoteEventPayloadCodec payloadCodec;

    /**
     * Identifier of the catalog or config object this event refers to, from {@link Info#getId()}
     */
    private @Getter String objectId;

    private @Getter ConfigInfoInfoType infoType;

    private I object;

    private String serializedObject;

    /** Deserialization-time constructor, {@link #getSource()} will be {@code null} */
    protected RemoteInfoEvent() {
        // default constructor, needed for deserialization
    }

    /** Publish-time constructor, {@link #getSource()} won't be {@code null} */
    protected RemoteInfoEvent(
            @NonNull S source, @NonNull I info, String originService, String destinationService) {

        super(source, originService, destinationService);
        this.objectId = info.getId();
        this.infoType = ConfigInfoInfoType.valueOf(info);
        if (info instanceof org.geoserver.catalog.Catalog) {
            log.trace(
                    "changed object is Catalog, setting remote event's object property to null. Catalog can't transferred as payload.");
        } else {
            this.object = info;
        }
    }

    protected @PostConstruct void encodePayload() {
        if (this.object != null && payloadCodec.isIncludePayload()) {
            try {
                this.serializedObject = this.payloadCodec.encode(this.object);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.object = null;
        }
    }

    @JsonInclude
    public String getSerializedObject() {
        return this.serializedObject;
    }

    public Optional<I> object() {
        return Optional.ofNullable(resolveObject());
    }

    @SuppressWarnings("unchecked")
    private I resolveObject() {
        if (this.object == null && this.serializedObject != null && this.payloadCodec != null) {
            try {
                this.object =
                        this.payloadCodec.decode(serializedObject, (Class<I>) infoType.getType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this.object;
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, event id: %s, from: %s, to: %s, ts: %d]",
                getClass().getSimpleName(),
                getInfoType(),
                this.getObjectId(),
                super.getId(),
                super.getOriginService(),
                super.getDestinationService(),
                super.getTimestamp());
    }
}
