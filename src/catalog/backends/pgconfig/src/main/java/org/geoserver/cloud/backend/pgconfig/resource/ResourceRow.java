/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import lombok.NonNull;
import org.geoserver.platform.resource.Resource.Type;

/**
 * A row of the {@code resourcestore} table without its content. The root directory has the id {@code 0}, the empty
 * path, and the parent id {@code 0}.
 *
 * @param mtime the time of the last modification, in milliseconds since the epoch
 * @param size the length of the content in bytes, 0 for a directory
 */
public record ResourceRow(long id, long parentId, @NonNull Type type, @NonNull String path, long mtime, long size) {}
