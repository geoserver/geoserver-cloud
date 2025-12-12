/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.authkey;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-authkey-.*!/applicationContext.xml",
        includes = AuthKeyWebUIConfiguration.WEB_UI_BEANS)
@Import(AuthKeyWebUIConfiguration_Generated.class)
public class AuthKeyWebUIConfiguration {
    static final String WEB_UI_BEANS =
            "authKeyPanelInfo|authKeyRESTRoleServicePanelInfo|authKeyWebServiceBodyResponseUserGroupServicePanelInfo";
}
