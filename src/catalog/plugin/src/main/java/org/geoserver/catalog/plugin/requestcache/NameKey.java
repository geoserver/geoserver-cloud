/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import org.geoserver.catalog.impl.ClassMappings;

/**
 * The key of an object looked up by name in a {@link RequestCache}, with the name as passed to the lookup, prefixed or
 * not. The type is part of the key, as in {@link IdKey}.
 */
record NameKey(ClassMappings type, String name) {}
