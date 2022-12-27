/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.ui;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.boot.info.BuildProperties;

/**
 * @since 1.0
 */
public class GeoServerCloudStatusPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public GeoServerCloudStatusPanel(String id, BuildProperties buildInfo) {
        super(id);
        this.add(new Label("gsCloudVersion", buildInfo.getVersion()));
    }
}
