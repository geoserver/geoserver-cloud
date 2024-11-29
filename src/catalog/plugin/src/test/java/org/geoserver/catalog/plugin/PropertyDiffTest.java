/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.SimpleInternationalString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyDiffTest {
    private PropertyDiffTestSupport support = new PropertyDiffTestSupport();

    public CatalogTestData data;

    public @BeforeEach void setup() {
        Catalog catalog = new CatalogPlugin();
        data = CatalogTestData.empty(() -> catalog, () -> null)
                .initConfig(false)
                .initialize();
    }

    @Test
    void empty() {
        PropertyDiff diff = support.createTestDiff();
        assertTrue(diff.isEmpty());
        assertTrue(diff.getChanges().isEmpty());
    }

    @Test
    void simpleStringProp() {
        PropertyDiff diff = support.createTestDiff("prop1", "oldValue", "newValue");
        assertEquals(1, diff.size());
        Change change = diff.get(0);
        assertNotNull(change);
        assertEquals("prop1", change.getPropertyName());
        assertEquals("oldValue", change.getOldValue());
        assertEquals("newValue", change.getNewValue());
    }

    @Test
    void cleanToEmpty() {
        PropertyDiff diff = support.createTestDiff( //
                "leftListNull",
                null,
                new ArrayList<>(), //
                "rightListNull",
                new ArrayList<>(),
                null, //
                "bothListsEmpty",
                new ArrayList<>(),
                new ArrayList<>(), //
                "bothListsEqual",
                singletonList("val"),
                singletonList("val"), //
                "leftMapNull",
                null,
                new HashMap<>(), //
                "rightMapNull",
                new HashMap<>(),
                null, //
                "bothMapsEmpty",
                new HashMap<>(),
                new HashMap<>() //
                );
        assertEquals(7, diff.size());
        PropertyDiff clean = diff.clean();
        assertTrue(clean.isEmpty());
        assertEquals(0, clean.size());
        assertEquals(0, clean.getChanges().size());
    }

    @Test
    void cleanToEmpty_InternationalString() {
        PropertyDiff diff = support.createTestDiff( //
                "leftListNull",
                null,
                new SimpleInternationalString(""), //
                "rightListNull",
                new SimpleInternationalString(""), //
                null, //
                "bothEmpty",
                new SimpleInternationalString(""), //
                new GrowableInternationalString(), //
                "bothEmpty2",
                new GrowableInternationalString(), //
                new SimpleInternationalString("") //
                );

        PropertyDiff clean = diff.clean();
        assertTrue(clean.isEmpty());
        assertEquals(0, clean.size());
        assertEquals(0, clean.getChanges().size());
    }

    @Test
    void clean() {
        PropertyDiff diff = support.createTestDiff( //
                "prop1",
                null,
                new ArrayList<>(), //
                "prop2",
                new HashMap<>(),
                null, //
                "prop3",
                new HashSet<>(),
                new HashSet<String>(Arrays.asList("val1", "val2")), //
                "prop4",
                "val1",
                Integer.valueOf(2));

        assertEquals(4, diff.size());
        PropertyDiff clean = diff.clean();
        assertFalse(clean.isEmpty());
        assertEquals(2, clean.size());
        assertEquals(2, clean.getChanges().size());

        assertEquals("prop3", clean.get(0).getPropertyName());
        assertEquals(Collections.emptySet(), clean.get(0).getOldValue());
        assertEquals(
                new HashSet<String>(Arrays.asList("val1", "val2")), clean.get(0).getNewValue());

        assertEquals("prop4", clean.get(1).getPropertyName());
        assertEquals("val1", clean.get(1).getOldValue());
        assertEquals(Integer.valueOf(2), clean.get(1).getNewValue());
    }

    @Test
    void builderToEmpty() {
        WorkspaceInfo ws = data.workspaceA;
        ws.setDateCreated(new Date());

        PropertyDiff diff = PropertyDiff.builder(ws)
                .with("name", ws.getName())
                .with("isolated", ws.isIsolated())
                .with("dateCreated", ws.getDateCreated())
                .with("dateModified", ws.getDateModified())
                .build();
        assertEquals(4, diff.size());
        assertTrue(diff.clean().isEmpty());
    }

    @Test
    void applyWorkspace() {
        WorkspaceInfo ws = data.workspaceA;
        ws.setDateCreated(null);
        ws.setDateModified(null);

        WorkspaceInfo copy = new WorkspaceInfoImpl();
        copy.setName(ws.getName());

        PropertyDiff diff = PropertyDiff.builder(ws).with("name", "newname").build();
        diff.toPatch().applyTo(copy);
        assertEquals("newname", copy.getName());
        assertNotEquals(ws.getName(), copy.getName());

        Date created = new Date(10000);
        Date modified = new Date(10001);

        MetadataMap metadata = new MetadataMap();
        metadata.put("k1", "v1");
        metadata.put("k2", Long.MAX_VALUE);

        diff = PropertyDiff.builder(ws)
                .with("name", "newname2")
                .with("isolated", true)
                .with("dateCreated", created)
                .with("dateModified", modified)
                .with("metadata", metadata)
                .build();

        diff.toPatch().applyTo(copy);

        assertEquals("newname2", copy.getName());
        assertTrue(copy.isIsolated());
        assertEquals(created, copy.getDateCreated());
        assertEquals(modified, copy.getDateModified());
        assertEquals(metadata, copy.getMetadata());

        assertNotEquals(ws.getName(), copy.getName());
        assertNotEquals(ws.isIsolated(), copy.isIsolated());
        assertNotEquals(ws.getDateCreated(), copy.getDateCreated());
        assertNotEquals(ws.getDateModified(), copy.getDateModified());
        assertNotEquals(ws.getMetadata(), copy.getMetadata());
    }
}
