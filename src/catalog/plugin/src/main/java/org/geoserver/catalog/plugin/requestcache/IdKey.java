/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import org.geoserver.catalog.impl.ClassMappings;

/**
 * The key of an object looked up by id in a {@link RequestCache}. The type is part of the key because the answer
 * depends on the type asked for: looked up as a data store, the id of a coverage store finds nothing.
 */
record IdKey(ClassMappings type, String id) {}
