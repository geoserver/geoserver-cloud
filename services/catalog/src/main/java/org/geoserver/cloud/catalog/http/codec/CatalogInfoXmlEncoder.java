/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.http.codec;

import java.io.IOException;
import java.util.Map;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractSingleValueEncoder;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CatalogInfoXmlEncoder extends AbstractSingleValueEncoder<CatalogInfo> {

    private XStreamPersisterFactory persisterFactory;
    private XStreamPersister xmlEncoder;

    public CatalogInfoXmlEncoder(XStreamPersisterFactory persisterFactory) {
        super(MimeType.valueOf(MediaType.APPLICATION_XML_VALUE));
        this.persisterFactory = persisterFactory;
    }

    private XStreamPersister xmlEncoder() {
        if (xmlEncoder == null) {
            xmlEncoder = persisterFactory.createXMLPersister();
        }
        return xmlEncoder;
    }

    public @Override boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
        final @NonNull Class<?> resolvedType = elementType.toClass();
        return CatalogInfo.class.isAssignableFrom(resolvedType)
                && super.canEncode(elementType, mimeType);
    }

    protected @Override Flux<DataBuffer> encode(
            CatalogInfo info,
            DataBufferFactory dataBufferFactory,
            ResolvableType type,
            MimeType mimeType,
            Map<String, Object> hints) {

        return Flux.defer(() -> encode(info, dataBufferFactory));
    }

    private Mono<DataBuffer> encode(CatalogInfo info, DataBufferFactory dataBufferFactory) {
        XStreamPersister encoder = xmlEncoder();
        DataBuffer dataBuffer = dataBufferFactory.allocateBuffer();
        try {
            encoder.save(info, dataBuffer.asOutputStream());
        } catch (IOException e) {
            throw new EncodingException(e.getMessage(), e);
        }
        return Mono.just(dataBuffer);
    }
}
