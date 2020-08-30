/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.http.codec;

import java.io.IOException;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

public class CatalogInfoXmlDecoder extends AbstractDataBufferDecoder<CatalogInfo> {

    private XStreamPersisterFactory persisterFactory;
    private XStreamPersister xmlDecoder;
    private Catalog catalog;

    /**
     * @param persisterFactory
     * @param catalog needed to resolve CatalogInfo reference ids to actual objects
     */
    public CatalogInfoXmlDecoder(XStreamPersisterFactory persisterFactory, Catalog catalog) {
        super(MimeType.valueOf(MediaType.APPLICATION_XML_VALUE));
        this.persisterFactory = persisterFactory;
        this.catalog = catalog;
    }

    private XStreamPersister xmlDecoder() {
        if (xmlDecoder == null) {
            xmlDecoder = persisterFactory.createXMLPersister();
            xmlDecoder.setUnwrapNulls(false);
            // needed to resolve CatalogInfo reference ids to actual objects
            xmlDecoder.setCatalog(catalog);
        }
        return xmlDecoder;
    }

    public @Override boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
        Class<?> resolvedType = elementType.toClass();
        return CatalogInfo.class.isAssignableFrom(resolvedType)
                && super.canDecode(elementType, mimeType);
    }

    public @Override CatalogInfo decode(
            DataBuffer buffer,
            ResolvableType targetType,
            @Nullable MimeType mimeType,
            @Nullable Map<String, Object> hints)
            throws DecodingException {

        XStreamPersister decoder = xmlDecoder();
        Class<?> clazz = targetType.toClass();
        try {
            Object loaded = decoder.load(buffer.asInputStream(), clazz);
            return CatalogInfo.class.cast(loaded);
        } catch (IOException e) {
            throw new DecodingException(e.getMessage(), e);
        }
    }
}
