/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import org.geoserver.cloud.bus.GeoServerBusProperties;
import org.geoserver.cloud.bus.GeoServerRemoteEventBroadcaster;
import org.geoserver.cloud.bus.RemoteEventResourcePoolProcessor;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
import org.geoserver.cloud.test.TestConfigurationAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = {TestConfigurationAutoConfiguration.class, ApplicationEventCapturingListener.class},
    properties = "spring.cloud.bus.enabled=false"
)
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class RemoteApplicationEventsAutoConfigurationDisabledTest {
    private @Autowired ApplicationContext context;

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void geoServerBusProperties() {
        context.getBean(GeoServerBusProperties.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void remoteEventBroadcaster() {
        context.getBean(GeoServerRemoteEventBroadcaster.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void remoteEventResourcePoolProcessor() {
        context.getBean(RemoteEventResourcePoolProcessor.class);
    }
}
