/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.autoconfigure.cache;

import org.geoserver.acl.authorization.cache.CachingAuthorizationServiceConfiguration;
import org.geoserver.acl.plugin.autoconfigure.accessmanager.AclAccessManagerAutoConfiguration;
import org.geoserver.acl.plugin.autoconfigure.conditionals.ConditionalOnAclEnabled;
import org.geoserver.acl.plugin.config.cache.CachingAuthorizationServicePluginConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 * @see CachingAuthorizationServiceConfiguration
 */
@AutoConfiguration(after = AclAccessManagerAutoConfiguration.class)
@ConditionalOnAclEnabled
@ConditionalOnProperty(name = "geoserver.acl.client.caching", havingValue = "true", matchIfMissing = true)
@EnableCaching
@Import(CachingAuthorizationServicePluginConfiguration.class)
public class CachingAuthorizationServicePluginAutoConfiguration {}
