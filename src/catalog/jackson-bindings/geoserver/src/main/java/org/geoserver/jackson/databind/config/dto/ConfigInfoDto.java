/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.geoserver.jackson.databind.catalog.dto.InfoDto;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConfigInfoDto extends InfoDto {}
