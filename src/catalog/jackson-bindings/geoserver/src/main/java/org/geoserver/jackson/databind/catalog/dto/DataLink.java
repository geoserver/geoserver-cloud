/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

@Data
public class DataLink {
    private String id;
    private String about;
    private String type;
    private String content;
}
