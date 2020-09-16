package org.geotools.jackson.databind.filter.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.jackson.databind.filter.FilterRoundtripTest;
import org.geotools.jackson.databind.filter.dto.Filter;
import org.junit.Before;
import org.mapstruct.factory.Mappers;

public class FilterMapperTest extends FilterRoundtripTest {

    private FilterMapper filterMapper;

    public @Before void before() {
        filterMapper = Mappers.getMapper(FilterMapper.class);
    }

    @SuppressWarnings("unchecked")
    protected @Override <F extends Filter> F roundtripTest(F dto) throws Exception {
        org.opengis.filter.Filter ogcFilter = filterMapper.map(dto);
        assertNotNull(ogcFilter);

        Filter roundTrippedDto = filterMapper.map(ogcFilter);
        assertEquals(dto, roundTrippedDto);
        return (F) roundTrippedDto;
    }
}
