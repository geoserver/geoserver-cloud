/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway;

import lombok.Getter;
import lombok.Setter;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.web.LoginFormInfo;

/**
 * @since 1.2
 */
@SuppressWarnings("serial")
public class PrioritizableLoginFormInfo extends LoginFormInfo implements ExtensionPriority {

    private @Getter @Setter int priority = ExtensionPriority.LOWEST;
}
