/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.geoserver.cloud.test.CatalogTestData;
import org.junit.Before;
import org.junit.Test;

public class PropertyDiffTest {
    private PropertyDiffTestSupport support = new PropertyDiffTestSupport();

    public CatalogTestData data;

    public @Before void setup() {
        Catalog catalog = new CatalogImpl();
        data = CatalogTestData.empty(() -> catalog).createCatalogObjects().createConfigObjects();
    }

    public @Test void empty() {
        PropertyDiff diff = support.createTestDiff();
        assertTrue(diff.isEmpty());
        assertTrue(diff.getChanges().isEmpty());
    }

    public @Test void simpleStringProp() {
        PropertyDiff diff = support.createTestDiff("prop1", "oldValue", "newValue");
        assertEquals(1, diff.size());
        Change change = diff.get(0);
        assertNotNull(change);
        assertEquals("prop1", change.getPropertyName());
        assertEquals("oldValue", change.getOldValue());
        assertEquals("newValue", change.getNewValue());
    }

    public @Test void cleanToEmpty() {
        PropertyDiff diff =
                support.createTestDiff( //
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

    public @Test void clean() {
        PropertyDiff diff =
                support.createTestDiff( //
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

    public @Test void builderToEmpty() {
        WorkspaceInfo ws = data.workspaceA;
        ws.setDateCreated(new Date());

        PropertyDiff diff =
                PropertyDiff.builder(ws)
                        .with("name", ws.getName())
                        .with("isolated", ws.isIsolated())
                        .with("dateCreated", ws.getDateCreated())
                        .with("dateModified", ws.getDateModified())
                        .build();
        assertTrue(diff.isEmpty());
    }

    public @Test void applyWorkspace() {
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

        diff =
                PropertyDiff.builder(ws)
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
        assertNotEquals(ws.isIsolated(), copy.getName());
        assertNotEquals(ws.getDateCreated(), copy.getDateCreated());
        assertNotEquals(ws.getDateModified(), copy.getDateModified());
        assertNotEquals(ws.getMetadata(), copy.getMetadata());
    }
}
