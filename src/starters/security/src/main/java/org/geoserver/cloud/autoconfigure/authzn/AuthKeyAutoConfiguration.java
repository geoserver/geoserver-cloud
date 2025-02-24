/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.authzn;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@AutoConfiguration
@Import({AuthKeyAutoConfiguration.Enabled.class, AuthKeyAutoConfiguration.WebUI.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.authzn")
public class AuthKeyAutoConfiguration {

    static final String GEOSERVER_SECURITY_AUTHKEY = "geoserver.security.authkey";

    private static final String WEB_UI_BEANS =
            "authKeyPanelInfo|authKeyRESTRoleServicePanelInfo|authKeyWebServiceBodyResponseUserGroupServicePanelInfo";

    @Bean(name = "authKeyExtension")
    ModuleStatus authKeyExtension(
            @Value("${" + GEOSERVER_SECURITY_AUTHKEY + ":" + ConditionalOnAuthKeyEnabled.ENABLED_BY_DEFAULT + "}")
                    boolean enabled) {

        ModuleStatusImpl module = new ModuleStatusImpl();
        module.setName("Authkey Extension");
        module.setModule("gs-authkey");
        module.setComponent("Authkey extension");
        module.setAvailable(true);
        module.setEnabled(enabled);
        if (!enabled) {
            module.setMessage("Authkey Extension disabled. " + GEOSERVER_SECURITY_AUTHKEY + "=false");
        }
        return module;
    }

    @ConditionalOnAuthKeyEnabled
    @ImportFilteredResource(Enabled.INCLUDE)
    static @Configuration class Enabled {
        static final String EXCLUDE = "authKeyExtension|" + WEB_UI_BEANS;
        static final String INCLUDE = "jar:gs-authkey-.*!/applicationContext.xml#name=^(?!" + EXCLUDE + ").*$";

        public @PostConstruct void log() {
            log.info("{} enabled", GEOSERVER_SECURITY_AUTHKEY);
        }
    }

    @ConditionalOnAuthKeyEnabled
    @ConditionalOnClass(AuthenticationFilterPanel.class)
    @ImportFilteredResource(WebUI.INCLUDE)
    static @Configuration class WebUI {
        static final String INCLUDE = "jar:gs-authkey-.*!/applicationContext.xml#name=^(" + WEB_UI_BEANS + ").*$";
    }
}
