/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

/**
 * The key of a lookup by neither id nor name, such as the lookup of the default workspace or of the settings of a
 * workspace. The discriminator names the lookup and its arguments.
 */
record LookupKey(String discriminator) {}
