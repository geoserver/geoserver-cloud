/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import java.io.File;
import java.util.Objects;
import javax.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.DataDirectoryResourceStore;

@Slf4j
@SuppressWarnings("serial")
public class NoServletContextDataDirectoryResourceStore extends DataDirectoryResourceStore {

    public NoServletContextDataDirectoryResourceStore(File resourceDirectory) {
        Objects.requireNonNull(resourceDirectory, "root resource directory required");

        if (resourceDirectory.isFile()) {
            throw new IllegalArgumentException(
                    "Directory required, file present at this location " + resourceDirectory);
        } else if (!resourceDirectory.isDirectory() && !resourceDirectory.mkdirs()) {
            throw new IllegalArgumentException("Unable to create directory " + resourceDirectory);
        }
        this.baseDirectory = resourceDirectory;
    }

    public @Override void setServletContext(ServletContext servletContext) {
        log.debug("setServletContext(ServletContext) ignored, data directory explicitly provided.");
    }
}
