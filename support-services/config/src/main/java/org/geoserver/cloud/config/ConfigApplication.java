/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableConfigServer
@EnableRetry
@NativeHint(
    // these type hints are not needed since we've configured config-service not to fetch the eureka
    // registry
    //    types =
    //            @TypeHint(
    //                types = {
    //                    com.netflix.discovery.shared.Application.class, //
    //                    com.netflix.appinfo.InstanceInfo.class, //
    //                    com.netflix.appinfo.InstanceInfo.PortWrapper.class, //
    //                    com.netflix.appinfo.MyDataCenterInfo.class
    //                }
    //            ), //
    resources = @ResourceHint(patterns = "gs_cloud_bootstrap_profiles.yml")
)
public class ConfigApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ConfigApplication.class) //
                // .properties("spring.config.name=config-service")
                .run(args);
    }
}
