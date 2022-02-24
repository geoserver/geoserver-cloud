/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

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

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Encapsulates default {@link Catalog} business rules for {@link StyleInfo} objects */
public class DefaultStyleInfoRules implements CatalogInfoBusinessRules<StyleInfo> {
    private static final Logger LOGGER = Logging.getLogger(DefaultStyleInfoRules.class);

    /**
     * Checks the {@link CatalogOpContext#getDiff() diff} to see of the style's name is going to be
     * changed, and renames it's {@link StyleInfo#getFilename() file name} to match the new style
     * name.
     */
    public @Override void beforeSave(CatalogOpContext<StyleInfo> context) {
        PropertyDiff diff = context.getDiff();
        Optional<Change> nameChange = diff.get("name");
        nameChange.ifPresent(
                change -> {
                    try {
                        renameStyle(
                                context.getCatalog(),
                                context.getObject(),
                                (String) nameChange.get().getNewValue());
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to rename style file along with name.", e);
                    }
                });
    }

    /** Reverts the style file rename performed in {@link #beforeAdd} if the operation has failed */
    public @Override void afterSave(CatalogOpContext<StyleInfo> context) {
        if (context.isSuccess()) {
            return;
        }
        PropertyDiff diff = context.getDiff();
        Optional<Change> nameChange = diff.get("name");
        nameChange.ifPresent(
                change -> {
                    try {
                        revertRenameStyle(
                                context.getCatalog(),
                                context.getObject(),
                                (String) change.getOldValue());
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
