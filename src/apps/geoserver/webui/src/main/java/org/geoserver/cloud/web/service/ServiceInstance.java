/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.service;

import java.io.Serializable;
import lombok.Data;

/**
 * @since 1.0
 */
public @Data class ServiceInstance implements Serializable, Comparable<ServiceInstance> {
    private static final long serialVersionUID = 1L;

    private String name;
    private String instanceId;
    private String uri;
    private String status;

    @Override
    public int compareTo(ServiceInstance o) {
        int c = getName().compareTo(o.getName());
        if (c == 0) {
            c = getInstanceId().compareTo(o.getInstanceId());
        }
        return c;
    }
}
