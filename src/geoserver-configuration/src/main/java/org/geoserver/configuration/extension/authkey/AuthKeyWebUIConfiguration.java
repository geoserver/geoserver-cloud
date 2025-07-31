/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.authkey;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportFilteredResource(AuthKeyWebUIConfiguration.INCLUDE)
public class AuthKeyWebUIConfiguration {
    static final String WEB_UI_BEANS =
            "authKeyPanelInfo|authKeyRESTRoleServicePanelInfo|authKeyWebServiceBodyResponseUserGroupServicePanelInfo";

    static final String INCLUDE = "jar:gs-authkey-.*!/applicationContext.xml#name=^(" + WEB_UI_BEANS + ").*$";
}
