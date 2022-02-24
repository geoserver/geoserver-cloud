/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import lombok.Data;

import org.geoserver.config.ContactInfo;

import java.util.Map;

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

    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddress;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalContactFacsimile;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalContactOrganization;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalContactPerson;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalContactPosition;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalContactVoice;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalOnlineResource;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddressCity;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddressCountry;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddressDeliveryPoint;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddressPostalCode;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddressState;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAddressType;
    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalContactEmail;
}
