/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.geoserver.config.CoverageAccessInfo;

/** DTO for {@link CoverageAccessInfo} */
@Data
@JsonTypeName("CoverageAccessInfo")
public class CoverageAccessInfoDto {
    /** DTO for {@link org.geoserver.config.CoverageAccessInfo.QueueType} */
    public enum QueueTypeDto {
        UNBOUNDED,
        DIRECT
    }

    private int corePoolSize;
    private int keepAliveTime;
    private int maxPoolSize;
    private QueueTypeDto queueType;
    private long imageIOCacheThreshold;
}
