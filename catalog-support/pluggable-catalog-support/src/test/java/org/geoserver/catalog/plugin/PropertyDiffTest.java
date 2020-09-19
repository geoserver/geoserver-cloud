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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.junit.Test;

public class PropertyDiffTest {
    private PropertyDiffTestSupport support = new PropertyDiffTestSupport();

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
}
