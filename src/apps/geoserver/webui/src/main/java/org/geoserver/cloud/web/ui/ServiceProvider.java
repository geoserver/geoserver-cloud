/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.ui;

import java.io.Serial;
import java.util.List;
import org.geoserver.cloud.web.service.ServiceInstance;
import org.geoserver.cloud.web.service.ServiceInstanceRegistry;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * @since 1.0
 */
class ServiceProvider extends GeoServerDataProvider<ServiceInstance> {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final Property<ServiceInstance> NAME = new BeanProperty<>("name", "name");
    public static final Property<ServiceInstance> INSTANCEID = new BeanProperty<>("instanceId", "instanceId");
    public static final Property<ServiceInstance> STATUS = new BeanProperty<>("status", "status");
    public static final Property<ServiceInstance> URI = new BeanProperty<>("uri", "uri");

    protected @Override List<Property<ServiceInstance>> getProperties() {
        return List.of(NAME, STATUS, INSTANCEID, URI);
    }

    protected @Override List<ServiceInstance> getItems() {
        GeoServerApplication webApp = GeoServerApplication.get();
        ServiceInstanceRegistry registry = webApp.getBeanOfType(ServiceInstanceRegistry.class);
        return registry.getServices().sorted().toList();
    }
}
