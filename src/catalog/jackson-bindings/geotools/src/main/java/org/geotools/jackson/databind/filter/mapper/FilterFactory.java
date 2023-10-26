/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.IncludeFilter;
import org.mapstruct.ObjectFactory;

public class FilterFactory {

    @ObjectFactory
    public IncludeFilter include() {
        return Filter.INCLUDE;
    }

    @ObjectFactory
    public ExcludeFilter exclude() {
        return Filter.EXCLUDE;
    }
}
