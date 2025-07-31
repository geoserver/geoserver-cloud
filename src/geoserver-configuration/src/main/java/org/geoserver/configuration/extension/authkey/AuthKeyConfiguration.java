/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.authkey;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportFilteredResource(AuthKeyConfiguration.INCLUDE)
public class AuthKeyConfiguration {
    static final String EXCLUDE = "authKeyExtension|" + AuthKeyWebUIConfiguration.WEB_UI_BEANS;
    static final String INCLUDE = "jar:gs-authkey-.*!/applicationContext.xml#name=^(?!" + EXCLUDE + ").*$";
}
