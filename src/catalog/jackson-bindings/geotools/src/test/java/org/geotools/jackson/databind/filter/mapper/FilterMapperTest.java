/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.geotools.jackson.databind.filter.FilterRoundtripTest;
import org.geotools.jackson.databind.filter.dto.Filter;
import org.geotools.jackson.databind.filter.dto.SortBy;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;

public class FilterMapperTest extends FilterRoundtripTest {

    private FilterMapper filterMapper;

    public @BeforeEach void before() {
        filterMapper = Mappers.getMapper(FilterMapper.class);
    }

    @SuppressWarnings("unchecked")
    protected @Override <F extends Filter> F roundtripTest(F dto) throws Exception {
        org.geotools.api.filter.Filter ogcFilter = filterMapper.map(dto);
        assertNotNull(ogcFilter);

        Filter roundTrippedDto = filterMapper.map(ogcFilter);
        assertEquals(dto, roundTrippedDto);
        return (F) roundTrippedDto;
    }

    protected @Override void roundtripTest(SortBy dto) throws Exception {
        org.geotools.api.filter.sort.SortBy ogcSortBy = filterMapper.map(dto);
        assertNotNull(ogcSortBy);

        SortBy roundTrippedDto = filterMapper.map(ogcSortBy);
        assertEquals(dto, roundTrippedDto);
    }
}
