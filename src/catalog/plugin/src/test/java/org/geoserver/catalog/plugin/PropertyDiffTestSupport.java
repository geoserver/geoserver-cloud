/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class PropertyDiffTestSupport {

    public PropertyDiff createTestDiff(Object... triplets) {
        assertTrue(triplets == null || triplets.length % 3 == 0);
        List<String> propertyNames = new ArrayList<>();
        List<Object> oldValues = new ArrayList<>();
        List<Object> newValues = new ArrayList<>();
        for (int i = 0; i < triplets.length; i++) {
            propertyNames.add((String) triplets[i]);
            oldValues.add(triplets[++i]);
            newValues.add(triplets[++i]);
        }
        return PropertyDiff.valueOf(propertyNames, oldValues, newValues);
    }
}
