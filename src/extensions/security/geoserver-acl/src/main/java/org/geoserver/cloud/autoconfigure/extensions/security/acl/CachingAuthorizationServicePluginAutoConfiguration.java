/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import org.geoserver.acl.config.authorization.cache.CachingAuthorizationServiceConfiguration;
import org.geoserver.acl.plugin.config.cache.CachingAuthorizationServicePluginConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 * @see CachingAuthorizationServiceConfiguration
 */
@ConditionalOnAclExtensionEnabled
@ConditionalOnProperty(name = "geoserver.acl.client.caching", havingValue = "true", matchIfMissing = true)
@EnableCaching
@Import(CachingAuthorizationServicePluginConfiguration.class)
public class CachingAuthorizationServicePluginAutoConfiguration {}
