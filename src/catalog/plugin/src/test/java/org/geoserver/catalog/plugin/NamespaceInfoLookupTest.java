/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogInfoLookup.NamespaceInfoLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Test suite for {@link NamespaceInfoLookup} */
public class NamespaceInfoLookupTest {

    private static final String URI_1 = "http://gs.test.com/ns1";
    private static final String URI_2 = "http://gs.test.com/ns2";

    private NamespaceInfo uri1_1, uri1_2, uri2_1, uri2_2;

    private CatalogInfoLookup.NamespaceInfoLookup lookup;

    @BeforeEach
    public void setUp() {
        uri1_1 = create("uri1_1", URI_1);
        uri1_2 = create("uri1_2", URI_1);
        uri2_1 = create("uri2_1", URI_2);
        uri2_2 = create("uri2_2", URI_2);
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
        for (NamespaceInfo ns : values) lookup.add(ns);
    }

    @Test
    public void testAdd() {
        lookup.add(uri1_2);
        assertEquals(List.of(uri1_2), lookup.valueList(URI_1, false));

        lookup.add(uri1_1);
        assertEquals(List.of(uri1_1, uri1_2), lookup.valueList(URI_1, false));

        assertThat(lookup.findById(uri1_1.getId())).get().isEqualTo(uri1_1);
        assertThat(lookup.findById(uri1_2.getId())).get().isEqualTo(uri1_2);
    }

    @Test
    public void testClear() {
        addAll(uri1_1, uri1_2, uri2_1, uri2_2);
        lookup.clear();
        assertEquals(List.of(), lookup.findAll().toList());
    }

    @Test
    public void testRemove() {
        addAll(uri1_1, uri1_2, uri2_1, uri2_2);
        testRemove(uri1_1);
        testRemove(uri1_2);
        testRemove(uri2_1);
        testRemove(uri2_2);
        lookup.remove(uri1_1);
    }

    private void testRemove(NamespaceInfo ns) {
        assertThat(lookup.findById(ns.getId())).isPresent();
        lookup.remove(ns);
        assertThat(lookup.findById(ns.getId())).isEmpty();
    }

    @Test
    public void testUpdate() {
        addAll(uri1_1, uri1_2, uri2_1, uri2_2);

        testUpdate(uri1_1, URI_2, List.of(uri1_1, uri2_1, uri2_2));
        testUpdate(uri2_2, URI_1, List.of(uri1_2, uri2_2));
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
    public void testFindAllByUri() {
        assertThat(lookup.findAllByURI(URI_1)).isEmpty();
        assertThat(lookup.findAllByURI(URI_2)).isEmpty();

        lookup.add(uri1_1);
        assertEquals(List.of(uri1_1), lookup.findAllByURI(URI_1).toList());

        lookup.add(uri2_1);
        assertEquals(List.of(uri1_1), lookup.findAllByURI(URI_1).toList());
        assertEquals(List.of(uri2_1), lookup.findAllByURI(URI_2).toList());

        lookup.add(uri1_2);
        lookup.add(uri2_2);
        assertEquals(List.of(uri1_1, uri1_2), lookup.findAllByURI(URI_1).toList());
        assertEquals(List.of(uri2_1, uri2_2), lookup.findAllByURI(URI_2).toList());
    }

    @Test
    public void testFindAllByUri_stable_order() {
        addAll(uri1_1, uri1_2);

        final List<NamespaceInfo> expected = List.of(uri1_1, uri1_2);
        assertEquals(expected, lookup.findAllByURI(URI_1).toList());

        lookup.clear();
        assertThat(lookup.findAllByURI(URI_1)).isEmpty();

        addAll(uri1_2, uri1_1);
        assertEquals(expected, lookup.findAllByURI(URI_1).toList());
    }

    @Test
    public void testFindOneByURI() {
        addAll(uri1_1);
        assertThat(lookup.findOneByURI(URI_1)).get().isEqualTo(uri1_1);
        addAll(uri1_1, uri2_1);
        assertThat(lookup.findOneByURI(URI_1)).get().isEqualTo(uri1_1);
        assertThat(lookup.findOneByURI(URI_2)).get().isEqualTo(uri2_1);
        addAll(uri2_2);
        assertThat(lookup.findOneByURI(URI_1)).get().isEqualTo(uri1_1);
        assertThat(lookup.findOneByURI(URI_2)).get().isEqualTo(uri2_1);
    }
}
