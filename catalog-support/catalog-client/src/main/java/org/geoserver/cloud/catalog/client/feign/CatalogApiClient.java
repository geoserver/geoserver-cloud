/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.opengis.filter.Filter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface CatalogApiClient<CI extends CatalogInfo> {

    public static final String XML = "application/xml";

    @PostMapping(path = "/", consumes = XML, produces = XML)
    CI create(CI info);

    @PutMapping(path = "/", consumes = XML, produces = XML)
    CI update(CI info);

    @DeleteMapping(path = "/", consumes = XML, produces = XML)
    void delete(CI value);

    @GetMapping(path = "/find/id/{id}", consumes = XML)
    <U extends CI> U findById(
            @PathVariable("id") String id,
            @RequestParam(name = "subtype", required = false) ClassMappings subType);

    @GetMapping(path = "find/name/{name}", consumes = XML)
    <U extends CI> U findByFirstByName(
            @PathVariable(name = "name", required = true) String name,
            @RequestParam(name = "subtype", required = false) ClassMappings subType);

    @GetMapping(path = "/query/all", consumes = XML)
    <U extends CI> List<U> findAll(
            @RequestParam(name = "subtype", required = false) ClassMappings subType);

    @PostMapping(path = "/query/filter", consumes = XML)
    <U extends CI> List<U> query(
            @RequestParam(name = "subtype", required = false) ClassMappings subType,
            @RequestBody Filter filter);
}
