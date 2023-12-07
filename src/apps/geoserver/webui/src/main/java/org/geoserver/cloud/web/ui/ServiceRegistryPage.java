/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.ui;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.cloud.web.service.ServiceInstance;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleExternalLink;

/**
 * @since 1.0
 */
public class ServiceRegistryPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private ServiceRegistryTable table;

    public ServiceRegistryPage() {
        initUI();
    }

    private void initUI() {
        add(table = new ServiceRegistryTable("table"));
    }

    static class ServiceRegistryTable extends GeoServerTablePanel<ServiceInstance> {
        private static final long serialVersionUID = 1L;

        public ServiceRegistryTable(String id) {
            super(id, new ServiceProvider());
            setOutputMarkupId(true);
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<ServiceInstance> itemModel, Property<ServiceInstance> property) {

            @SuppressWarnings("unchecked")
            IModel<String> model = (IModel<String>) property.getModel(itemModel);
            if ("uri".equals(property.getName())) {
                SimpleExternalLink link = new SimpleExternalLink(id, model);
                // avoid adding ;jsessionid=xxx to the url
                link.getLink().add(new AttributeAppender("rel", "noreferrer"));
                return link;
            }
            return new Label(id, model);
        }
    }
}
