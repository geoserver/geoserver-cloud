/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.geotools.jackson.databind.filter.FilterRoundtripTest;
import org.geotools.jackson.databind.filter.dto.FilterDto;
import org.geotools.jackson.databind.filter.dto.SortByDto;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;

class FilterMapperTest extends FilterRoundtripTest {

    private FilterMapper filterMapper;

    @BeforeEach
    void before() {
        filterMapper = Mappers.getMapper(FilterMapper.class);
    }

    @SuppressWarnings("unchecked")
    protected @Override <F extends FilterDto> F roundtripTest(F dto) throws Exception {
        org.geotools.api.filter.Filter ogcFilter = filterMapper.map(dto);
        assertNotNull(ogcFilter);

        FilterDto roundTrippedDto = filterMapper.map(ogcFilter);
        assertEquals(dto, roundTrippedDto);
        return (F) roundTrippedDto;
    }

    protected @Override void roundtripTest(SortByDto dto) throws Exception {
        org.geotools.api.filter.sort.SortBy ogcSortBy = filterMapper.dtoToSortBy(dto);
        assertNotNull(ogcSortBy);

        SortByDto roundTrippedDto = filterMapper.sortByToDto(ogcSortBy);
        assertEquals(dto, roundTrippedDto);
    }
}
