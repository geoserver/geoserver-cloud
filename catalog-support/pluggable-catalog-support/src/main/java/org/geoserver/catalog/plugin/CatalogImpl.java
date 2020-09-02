package org.geoserver.catalog.plugin;

import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.CatalogFacade;

/**
 * Extends {@link org.geoserver.catalog.impl.CatalogImpl} to allow decorating the {@link
 * CatalogFacade} with an {@link IsolatedCatalogFacade}
 */
@SuppressWarnings("serial")
public class CatalogImpl extends org.geoserver.catalog.impl.CatalogImpl {

    private @Getter @NonNull CatalogFacade rawCatalogFacade;

    public CatalogImpl() {
        this(new org.geoserver.catalog.plugin.DefaultCatalogFacade());
    }

    public CatalogImpl(CatalogFacade rawCatalogFacade) {
        // just to stress out the parent's default constructor is called nonetheless and we need to
        // completely replace its facade
        super();
        init(rawCatalogFacade);
    }

    private void init(@NonNull CatalogFacade rawCatalogFacade) {
        this.rawCatalogFacade = rawCatalogFacade;
        // wrap the default catalog facade with the facade capable of handling isolated workspaces
        // behavior
        setFacade(new IsolatedCatalogFacade(rawCatalogFacade));
        Objects.requireNonNull(super.resourcePool);
    }
}
