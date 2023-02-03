/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** */
@Data
@EqualsAndHashCode(callSuper = true)
public class CogSettingsStore extends CogSettings {

    private String username;
    private String password;
}
