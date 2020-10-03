/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/** Encapsulates default {@link Catalog} business rules for {@link StyleInfo} objects */
public class DefaultStyleInfoRules implements CatalogInfoBusinessRules<StyleInfo> {
    private static final Logger LOGGER = Logging.getLogger(DefaultStyleInfoRules.class);

    public @Override void onBeforeSave(Catalog catalog, StyleInfo style, PropertyDiff diff) {
        Optional<Change> nameChange = diff.get("name");
        nameChange.ifPresent(
                change -> {
                    try {
                        renameStyle(catalog, style, (String) nameChange.get().getNewValue());
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to rename style file along with name.", e);
                    }
                });
    }

    public @Override void onSaveError(
            Catalog catalog, StyleInfo style, PropertyDiff diff, Throwable error) {
        Optional<Change> nameChange = diff.get("name");
        nameChange.ifPresent(
                change -> {
                    try {
                        revertRenameStyle(catalog, style, (String) change.getOldValue());
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to revert style rename.", e);
                    }
                });
    }

    private void renameStyle(Catalog catalog, StyleInfo s, String newName) throws IOException {
        // rename style definition file
        GeoServerResourceLoader resourceLoader = catalog.getResourceLoader();
        Resource style = new GeoServerDataDirectory(resourceLoader).style(s);
        StyleHandler format = Styles.handler(s.getFormat());

        Resource target = Resources.uniqueResource(style, newName, format.getFileExtension());
        style.renameTo(target);
        s.setFilename(target.name());

        // rename generated sld if appropriate
        if (!SLDHandler.FORMAT.equals(format.getFormat())) {
            Resource sld = style.parent().get(FilenameUtils.getBaseName(style.name()) + ".sld");
            if (sld.getType() == Type.RESOURCE) {
                LOGGER.fine("Renaming style resource " + s.getName() + " to " + newName);

                Resource generated = Resources.uniqueResource(sld, newName, "sld");
                sld.renameTo(generated);
            }
        }
    }

    private void revertRenameStyle(Catalog catalog, StyleInfo style, String oldName)
            throws IOException {
        if (oldName != null) {
            renameStyle(catalog, style, oldName);
        }
    }
}
