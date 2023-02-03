/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import lombok.Data;

/** */
@Data
public class CogSettings {
    public enum RangeReaderType {
        HTTP,
        S3,
        GS,
        Azure;
    }

    private RangeReaderType rangeReaderSettings = RangeReaderType.HTTP;
    private boolean useCachingStream;
}
