/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import lombok.Data;
import org.geoserver.config.ContactInfo;

/** DTO for {@link ContactInfo} */
public @Data class Contact {
    private String id;
    private String address;
    private String addressCity;
    private String addressCountry;
    private String addressDeliveryPoint;
    private String addressPostalCode;
    private String addressState;
    private String addressType;
    private String contactEmail;
    private String contactFacsimile;
    private String contactOrganization;
    private String contactPerson;
    private String contactPosition;
    private String contactVoice;
    private String onlineResource;
}
