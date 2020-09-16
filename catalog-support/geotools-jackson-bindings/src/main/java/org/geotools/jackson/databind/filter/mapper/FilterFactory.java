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
