/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.bus.GeoServerBusProperties;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.util.OwsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;

public class RemoteEventPayloadCodec {

    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private @Autowired XStreamPersisterFactory persisterFactory;

    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;

    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private @Autowired GeoServer geoServer;

    /**
     * Tells whether to encode the configuration {@link GeoServerBusProperties#isSendObject object}
     * payload and the {@link GeoServerBusProperties#isSendDiff diff} of {@link
     * RemoteModifyEvent#diff RemoteModifyEvents}
     */
    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private @Autowired GeoServerBusProperties geoServerBusProperties;

    private XStreamPersister codec;

    @VisibleForTesting
    @PostConstruct
    void initializeCodec() {
        codec = persisterFactory.createXMLPersister();
        codec.setCatalog(rawCatalog);
        codec.setGeoServer(geoServer);
        codec.setUnwrapNulls(false);
        XStream xStream = codec.getXStream();
        xStream.alias("PropetyDiff", PropertyDiff.class);
        xStream.alias("change", PropertyDiff.Change.class);
        xStream.allowTypes(new Class[] {PropertyDiff.class, PropertyDiff.Change.class});
        xStream.registerConverter(new XStreamPersister.CRSConverter());
    }

    @EventListener(classes = {RemoteInfoEvent.class})
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void initIncomingMessage(RemoteInfoEvent<?, ?> event) {
        event.setPayloadCodec(this);
    }

    public PropertyDiff decode(@NonNull String serializedDiff) throws IOException {
        ByteArrayInputStream in;
        in = new ByteArrayInputStream(serializedDiff.getBytes(UTF_8));
        return codec.load(in, PropertyDiff.class);
    }

    /**
     * @return the serialized object iif {@code geoserver.bus.send-object=true}, {@code null}
     *     otherwise
     */
    public @Nullable String encode(PropertyDiff diff) throws IOException {
        if (this.geoServerBusProperties.isSendDiff()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            codec.save(diff, out);
            return out.toString(UTF_8);
        }
        return null;
    }

    public <I extends Info> I decode(@NonNull String serializedObject, @NonNull Class<I> type)
            throws IOException {
        ByteArrayInputStream in;
        in = new ByteArrayInputStream(serializedObject.getBytes(UTF_8));
        I parsed = codec.load(in, type);
        OwsUtils.resolveCollections(parsed);
        return parsed;
    }

    /**
     * @return the serialized object iif {@code geoserver.bus.send-object=true}, {@code null}
     *     otherwise
     */
    public @Nullable String encode(Info infoObject) throws IOException {
        if (this.geoServerBusProperties.isSendObject()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            codec.save(infoObject, out);
            return out.toString(UTF_8);
        }
        return null;
    }

    public boolean isIncludePayload() {
        return this.geoServerBusProperties.isSendObject();
    }

    public boolean isIncludeDiff() {
        return this.geoServerBusProperties.isSendDiff();
    }
}
