/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.datadirectory;

import jakarta.servlet.ServletContext;
import java.io.File;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.FileLockProvider;

/**
 * {@link FileLockProvider} that expects the data directory file at its constructor, overriding
 * {@link #setServletContext} to be ignored.
 */
@Slf4j
public class NoServletContextFileLockProvider extends FileLockProvider {

    public NoServletContextFileLockProvider(@NonNull File dataDirectory) {
        super(dataDirectory);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        log.debug("setServletContext(ServletContext) ignored, data directory explicitly provided.");
    }
}
