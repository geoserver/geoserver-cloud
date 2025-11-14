/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.autoconfigure.accessmanager;

import org.geoserver.acl.plugin.accessmanager.ACLResourceAccessManager;
import org.geoserver.acl.plugin.autoconfigure.conditionals.ConditionalOnAclEnabled;
import org.geoserver.acl.plugin.config.accessmanager.AclAccessManagerConfiguration;
import org.geoserver.security.ResourceAccessManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link AutoConfiguration @AutoConfiguration} for the GeoServer Access Control List {@link ACLResourceAccessManager}.
 *
 * <p>{@link ACLResourceAccessManager} implements GeoServer {@link ResourceAccessManager} by delegating resource access
 * requests to the GeoServer ACL service.
 *
 * @since 1.0
 * @see AclAccessManagerConfiguration
 */
@AutoConfiguration
@ConditionalOnAclEnabled
@Import(AclAccessManagerConfiguration.class)
public class AclAccessManagerAutoConfiguration {}
