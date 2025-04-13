/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;

/**
 * A {@link GeoServerResourcePersister} that unwraps the {@link CatalogModifyEvent#getSource()}'s
 * from a {@link ModificationProxy} before proceeding with {@link
 * GeoServerResourcePersister#handleModifyEvent super.handleModifyEvent()}.
 *
 * <p>Since it works only if the source is the real {@link Info}, as thrown by the legacy {@link
 * DefaultCatalogFacade#beforeSaved}, despite it having the following comment:
 *
 * <pre>
 * {@code // TO DO: protect this original object, perhaps with another proxy}
 * </pre>
 *
 * While {@link CatalogPlugin} fixes it by both using the modification proxy as the source, and by
 * taking full responsibility of event dispatching instead of mixing it up between catalog and
 * facade.
 */
@Slf4j
public class CatalogPluginStyleResourcePersister extends GeoServerResourcePersister {

    private Catalog theCatalog;
    private boolean backupSldFiles;

    public CatalogPluginStyleResourcePersister(Catalog catalog) {
        this(catalog, true);
    }

    public CatalogPluginStyleResourcePersister(Catalog catalog, boolean backupSldFiles) {
        super(catalog);
        this.theCatalog = catalog;
        this.backupSldFiles = backupSldFiles;
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) {
        CatalogModifyEvent e = CatalogPluginStyleResourcePersister.withRealSource(event);
        super.handleModifyEvent(e);
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) {
        Object source = event.getSource();
        if (source instanceof StyleInfo style) {
            doRemoveStyle(style);
        }
    }

    private void doRemoveStyle(StyleInfo s) {
        var dd = new GeoServerDataDirectory(theCatalog.getResourceLoader());
        Resource sld = dd.style(s);
        if (Resources.exists(sld) && sld.getType() == Type.RESOURCE) {
            if (this.backupSldFiles) {
                backupSld(dd, sld);
            } else {
                dd.getResourceLoader().remove(sld.path());
            }
        }
    }

    private void backupSld(GeoServerDataDirectory dd, Resource sld) {
        Resource sldBackup = dd.get(sld.path() + ".bak");
        int i = 1;
        while (Resources.exists(sldBackup)) {
            sldBackup = dd.get(sld.path() + ".bak." + i++);
        }
        log.debug("Removing the SLD as well but making backup " + sldBackup.name());
        sld.renameTo(sldBackup);
    }

    public static CatalogModifyEvent withRealSource(CatalogModifyEvent event) {
        CatalogInfo source = event.getSource();
        CatalogInfo real = ModificationProxy.unwrap(source);

        CatalogModifyEventImpl e = new CatalogModifyEventImpl();
        e.setSource(real);
        e.setPropertyNames(event.getPropertyNames());
        e.setOldValues(event.getOldValues());
        e.setNewValues(event.getNewValues());
        return e;
    }
}
