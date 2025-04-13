/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogInfoLookup.NamespaceInfoLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test suite for {@link NamespaceInfoLookup} */
class NamespaceInfoLookupTest {

    private static final String URI_1 = "http://gs.test.com/ns1";
    private static final String URI_2 = "http://gs.test.com/ns2";

    private NamespaceInfo uri11, uri12, uri21, uri22;

    private CatalogInfoLookup.NamespaceInfoLookup lookup;

    @BeforeEach
    public void setUp() {
        uri11 = create("uri1_1", URI_1);
        uri12 = create("uri1_2", URI_1);
        uri21 = create("uri2_1", URI_2);
        uri22 = create("uri2_2", URI_2);
        lookup = new NamespaceInfoLookup();
    }

    private NamespaceInfo create(String prefix, String uri) {
        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setId(prefix + "-id");
        ns.setPrefix(prefix);
        ns.setURI(uri);
        return ns;
    }

    private void addAll(NamespaceInfo... values) {
        for (NamespaceInfo ns : values) {
            lookup.add(ns);
        }
    }

    @Test
    void testAdd() {
        lookup.add(uri12);
        assertEquals(List.of(uri12), lookup.valueList(URI_1, false));

        lookup.add(uri11);
        assertEquals(List.of(uri11, uri12), lookup.valueList(URI_1, false));

        assertThat(lookup.findById(uri11.getId())).get().isEqualTo(uri11);
        assertThat(lookup.findById(uri12.getId())).get().isEqualTo(uri12);
    }

    @Test
    void testClear() {
        addAll(uri11, uri12, uri21, uri22);
        lookup.clear();
        assertEquals(List.of(), lookup.findAll().toList());
    }

    @Test
    void testRemove() {
        addAll(uri11, uri12, uri21, uri22);
        testRemove(uri11);
        testRemove(uri12);
        testRemove(uri21);
        testRemove(uri22);
        lookup.remove(uri11);
    }

    private void testRemove(NamespaceInfo ns) {
        assertThat(lookup.findById(ns.getId())).isPresent();
        lookup.remove(ns);
        assertThat(lookup.findById(ns.getId())).isEmpty();
    }

    @Test
    void testUpdate() {
        addAll(uri11, uri12, uri21, uri22);

        testUpdate(uri11, URI_2, List.of(uri11, uri21, uri22));
        testUpdate(uri22, URI_1, List.of(uri12, uri22));
    }

    private void testUpdate(NamespaceInfo ns, String newUri, List<NamespaceInfo> expected) {
        String oldUri = ns.getURI();
        assertTrue(lookup.valueList(oldUri, false).contains(ns));

        Patch patch = PropertyDiff.builder(ns).with("uri", newUri).build().toPatch();
        lookup.update(ns, patch);

        assertEquals(expected, lookup.valueList(newUri, false));

        assertFalse(lookup.valueList(oldUri, false).contains(ns));
    }

    @Test
    void testFindAllByUri() {
        assertThat(lookup.findAllByURI(URI_1)).isEmpty();
        assertThat(lookup.findAllByURI(URI_2)).isEmpty();

        lookup.add(uri11);
        assertEquals(List.of(uri11), lookup.findAllByURI(URI_1).toList());

        lookup.add(uri21);
        assertEquals(List.of(uri11), lookup.findAllByURI(URI_1).toList());
        assertEquals(List.of(uri21), lookup.findAllByURI(URI_2).toList());

        lookup.add(uri12);
        lookup.add(uri22);
        assertEquals(List.of(uri11, uri12), lookup.findAllByURI(URI_1).toList());
        assertEquals(List.of(uri21, uri22), lookup.findAllByURI(URI_2).toList());
    }

    @Test
    void testFindAllByUri_stable_order() {
        addAll(uri11, uri12);

        final List<NamespaceInfo> expected = List.of(uri11, uri12);
        assertEquals(expected, lookup.findAllByURI(URI_1).toList());

        lookup.clear();
        assertThat(lookup.findAllByURI(URI_1)).isEmpty();

        addAll(uri12, uri11);
        assertEquals(expected, lookup.findAllByURI(URI_1).toList());
    }

    @Test
    void testFindOneByURI() {
        addAll(uri11);
        assertThat(lookup.findOneByURI(URI_1)).get().isEqualTo(uri11);
        addAll(uri11, uri21);
        assertThat(lookup.findOneByURI(URI_1)).get().isEqualTo(uri11);
        assertThat(lookup.findOneByURI(URI_2)).get().isEqualTo(uri21);
        addAll(uri22);
        assertThat(lookup.findOneByURI(URI_1)).get().isEqualTo(uri11);
        assertThat(lookup.findOneByURI(URI_2)).get().isEqualTo(uri21);
    }
}
