/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.junit.jupiter.api.Test;

class ResolvingCatalogFacadeDecoratorTest {

    // the facades above it, such as the isolation facade, forward the typed adds to this one
    @Test
    void genericAddResolvesOutbound() {
        ExtendedCatalogFacade subject = mock(ExtendedCatalogFacade.class);
        StoreInfo added = new DataStoreInfoImpl(null, "ds-id");
        StoreInfo resolved = new DataStoreInfoImpl(null, "ds-id");
        when(subject.add((CatalogInfo) added)).thenReturn(added);
        ResolvingCatalogFacadeDecorator decorator = new ResolvingCatalogFacadeDecorator(subject);
        decorator.setOutboundResolver(info -> info == added ? resolved : info);

        CatalogInfo result = decorator.add((CatalogInfo) added);

        assertThat(result).isSameAs(resolved);
    }
}
