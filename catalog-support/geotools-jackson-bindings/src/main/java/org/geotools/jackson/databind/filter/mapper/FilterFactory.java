/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.IncludeFilter;

public class FilterFactory {

    public IncludeFilter include() {
        return Filter.INCLUDE;
    }

    public ExcludeFilter exclude() {
        return Filter.EXCLUDE;
    }
}
