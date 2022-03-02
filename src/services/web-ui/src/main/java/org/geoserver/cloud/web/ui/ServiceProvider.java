/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.ui;

import org.geoserver.cloud.web.service.ServiceInstance;
import org.geoserver.cloud.web.service.ServiceInstanceRegistry;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
class ServiceProvider extends GeoServerDataProvider<ServiceInstance> {
    private static final long serialVersionUID = 1L;

    public static Property<ServiceInstance> NAME = new BeanProperty<>("name", "name");
    public static Property<ServiceInstance> INSTANCEID =
            new BeanProperty<>("instanceId", "instanceId");
    public static Property<ServiceInstance> STATUS = new BeanProperty<>("status", "status");
    public static Property<ServiceInstance> URI = new BeanProperty<>("uri", "uri");

    protected @Override List<Property<ServiceInstance>> getProperties() {
        return List.of(NAME, STATUS, INSTANCEID, URI);
    }

    protected @Override List<ServiceInstance> getItems() {
        GeoServerApplication webApp = GeoServerApplication.get();
        ServiceInstanceRegistry registry = webApp.getBeanOfType(ServiceInstanceRegistry.class);
        return registry.getServices().sorted().collect(Collectors.toList());
    }
}
