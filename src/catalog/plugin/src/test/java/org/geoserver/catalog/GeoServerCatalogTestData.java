/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog;

import lombok.experimental.UtilityClass;

import org.apache.commons.io.FileUtils;
import org.geotools.test.TestData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * The purpose of this class is to provide a geoserver datadir which is not fake (it is actually a
 * copy of the one from GeoServer upstream), which could be used for testing.
 *
 * <p>It copies a zip file into a directory - generally a temporary one - an unzip it, so that it is
 * ready for use.
 */
@UtilityClass
public class GeoServerCatalogTestData {

    /**
     * This method copies the zipped datadir into the Path object given as argument and unzip it at
     * the same place.
     *
     * <p>It is the caller's responsability to clean up when the datadir is not needed anymore.
     *
     * <p>Note: we have to copy the resource into the directory first, because the method from
     * GeoTools which is being used does not support zip URIs nested into jar files.
     *
     * @param tmpPath the temporary path where the datadir has to be unzipped to.
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void unzipGeoserverCatalogTestData(Path tmpPath)
            throws URISyntaxException, IOException {
        InputStream zippedDatadir =
                GeoServerCatalogTestData.class.getResourceAsStream("/test-data-directory.zip");
        File tmpDir = tmpPath.toFile();
        File destFile = new File(tmpDir, "test-data-directory.zip");
        FileUtils.copyToFile(zippedDatadir, destFile);
        TestData.unzip(destFile, tmpDir);
        destFile.delete();
    }
}
