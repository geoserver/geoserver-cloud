--
-- PostgreSQL database dump
-- docker-compose exec database pg_dump -U geoserver --disable-triggers  -a -b -n public -O --inserts -F p -v -f /tmp/empty_db.sql geoserver_config
--

-- Dumped from database version 13.2 (Debian 13.2-1.pgdg100+1)
-- Dumped by pg_dump version 13.2 (Debian 13.2-1.pgdg100+1)

-- Started on 2021-06-15 05:14:28 UTC

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 4297 (class 0 OID 16426)
-- Dependencies: 214
-- Data for Name: default_object; Type: TABLE DATA; Schema: public; Owner: -
--

SET SESSION AUTHORIZATION DEFAULT;

ALTER TABLE public.default_object DISABLE TRIGGER ALL;



ALTER TABLE public.default_object ENABLE TRIGGER ALL;

--
-- TOC entry 4294 (class 0 OID 16406)
-- Dependencies: 211
-- Data for Name: type; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.type DISABLE TRIGGER ALL;

INSERT INTO public.type VALUES (1, 'org.geoserver.catalog.WorkspaceInfo');
INSERT INTO public.type VALUES (2, 'org.geoserver.catalog.NamespaceInfo');
INSERT INTO public.type VALUES (3, 'org.geoserver.catalog.DataStoreInfo');
INSERT INTO public.type VALUES (4, 'org.geoserver.catalog.CoverageStoreInfo');
INSERT INTO public.type VALUES (5, 'org.geoserver.catalog.WMSStoreInfo');
INSERT INTO public.type VALUES (6, 'org.geoserver.catalog.WMTSStoreInfo');
INSERT INTO public.type VALUES (7, 'org.geoserver.catalog.StoreInfo');
INSERT INTO public.type VALUES (8, 'org.geoserver.catalog.FeatureTypeInfo');
INSERT INTO public.type VALUES (9, 'org.geoserver.catalog.CoverageInfo');
INSERT INTO public.type VALUES (10, 'org.geoserver.catalog.WMSLayerInfo');
INSERT INTO public.type VALUES (11, 'org.geoserver.catalog.WMTSLayerInfo');
INSERT INTO public.type VALUES (12, 'org.geoserver.catalog.ResourceInfo');
INSERT INTO public.type VALUES (13, 'org.geoserver.catalog.PublishedInfo');
INSERT INTO public.type VALUES (14, 'org.geoserver.catalog.LayerInfo');
INSERT INTO public.type VALUES (15, 'org.geoserver.catalog.LayerGroupInfo');
INSERT INTO public.type VALUES (16, 'org.geoserver.catalog.MapInfo');
INSERT INTO public.type VALUES (17, 'org.geoserver.catalog.StyleInfo');
INSERT INTO public.type VALUES (18, 'org.geoserver.config.GeoServerInfo');
INSERT INTO public.type VALUES (19, 'org.geoserver.config.LoggingInfo');
INSERT INTO public.type VALUES (20, 'org.geoserver.config.SettingsInfo');
INSERT INTO public.type VALUES (21, 'org.geoserver.config.ServiceInfo');


ALTER TABLE public.type ENABLE TRIGGER ALL;

--
-- TOC entry 4291 (class 0 OID 16387)
-- Dependencies: 208
-- Data for Name: object; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.object DISABLE TRIGGER ALL;

INSERT INTO public.object VALUES (1, 17, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', '<style>
  <id>StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c</id>
  <name>point</name>
  <format>sld</format>
  <languageVersion>
    <version>1.0.0</version>
  </languageVersion>
  <filename>default_point.sld</filename>
</style>');
INSERT INTO public.object VALUES (2, 17, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', '<style>
  <id>StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391</id>
  <name>line</name>
  <format>sld</format>
  <languageVersion>
    <version>1.0.0</version>
  </languageVersion>
  <filename>default_line.sld</filename>
</style>');
INSERT INTO public.object VALUES (3, 17, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', '<style>
  <id>StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95</id>
  <name>polygon</name>
  <format>sld</format>
  <languageVersion>
    <version>1.0.0</version>
  </languageVersion>
  <filename>default_polygon.sld</filename>
</style>');
INSERT INTO public.object VALUES (4, 17, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', '<style>
  <id>StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761</id>
  <name>raster</name>
  <format>sld</format>
  <languageVersion>
    <version>1.0.0</version>
  </languageVersion>
  <filename>default_raster.sld</filename>
</style>');
INSERT INTO public.object VALUES (5, 17, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', '<style>
  <id>StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912</id>
  <name>generic</name>
  <format>sld</format>
  <languageVersion>
    <version>1.0.0</version>
  </languageVersion>
  <filename>default_generic.sld</filename>
</style>');
INSERT INTO public.object VALUES (6, 20, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', '<settings>
  <id>SettingsInfo.-1ec455f5:17a0e145460:-8000</id>
  <contact>
    <id>contact</id>
  </contact>
  <charset>UTF-8</charset>
  <numDecimals>4</numDecimals>
  <verbose>true</verbose>
  <verboseExceptions>false</verboseExceptions>
  <localWorkspaceIncludesPrefix>false</localWorkspaceIncludesPrefix>
  <showCreatedTimeColumnsInAdminList>false</showCreatedTimeColumnsInAdminList>
  <showModifiedTimeColumnsInAdminList>false</showModifiedTimeColumnsInAdminList>
</settings>');
INSERT INTO public.object VALUES (8, 19, 'LoggingInfo.global', '<logging>
  <id>LoggingInfo.global</id>
  <stdOutLogging>false</stdOutLogging>
</logging>');
INSERT INTO public.object VALUES (9, 21, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', '<org.geoserver.wfs.WFSInfoImpl>
  <id>ServiceInfo.-1ec455f5:17a0e145460:-7fff</id>
  <enabled>true</enabled>
  <name>WFS</name>
  <citeCompliant>false</citeCompliant>
  <schemaBaseURL>http://schemas.opengis.net</schemaBaseURL>
  <verbose>false</verbose>
  <gml>
    <entry>
      <org.geoserver.wfs.WFSInfo_-Version>V_10</org.geoserver.wfs.WFSInfo_-Version>
      <org.geoserver.wfs.GMLInfoImpl>
        <srsNameStyle>XML</srsNameStyle>
        <overrideGMLAttributes>true</overrideGMLAttributes>
      </org.geoserver.wfs.GMLInfoImpl>
    </entry>
    <entry>
      <org.geoserver.wfs.WFSInfo_-Version>V_20</org.geoserver.wfs.WFSInfo_-Version>
      <org.geoserver.wfs.GMLInfoImpl>
        <srsNameStyle>URN2</srsNameStyle>
        <overrideGMLAttributes>false</overrideGMLAttributes>
      </org.geoserver.wfs.GMLInfoImpl>
    </entry>
    <entry>
      <org.geoserver.wfs.WFSInfo_-Version>V_11</org.geoserver.wfs.WFSInfo_-Version>
      <org.geoserver.wfs.GMLInfoImpl>
        <srsNameStyle>URN</srsNameStyle>
        <overrideGMLAttributes>false</overrideGMLAttributes>
      </org.geoserver.wfs.GMLInfoImpl>
    </entry>
  </gml>
  <serviceLevel>COMPLETE</serviceLevel>
  <maxFeatures>1000000</maxFeatures>
  <featureBounding>true</featureBounding>
  <canonicalSchemaLocation>false</canonicalSchemaLocation>
  <encodeFeatureMember>false</encodeFeatureMember>
  <hitsIgnoreMaxFeatures>false</hitsIgnoreMaxFeatures>
  <includeWFSRequestDumpFile>true</includeWFSRequestDumpFile>
  <allowGlobalQueries>true</allowGlobalQueries>
  <simpleConversionEnabled>false</simpleConversionEnabled>
</org.geoserver.wfs.WFSInfoImpl>');
INSERT INTO public.object VALUES (10, 21, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', '<org.geoserver.wms.WMSInfoImpl>
  <id>ServiceInfo.-1ec455f5:17a0e145460:-7ffe</id>
  <enabled>true</enabled>
  <name>WMS</name>
  <citeCompliant>false</citeCompliant>
  <schemaBaseURL>http://schemas.opengis.net</schemaBaseURL>
  <verbose>false</verbose>
  <watermark class="org.geoserver.wms.WatermarkInfoImpl">
    <enabled>false</enabled>
    <position>BOT_RIGHT</position>
    <transparency>100</transparency>
  </watermark>
  <interpolation>Nearest</interpolation>
  <getFeatureInfoMimeTypeCheckingEnabled>false</getFeatureInfoMimeTypeCheckingEnabled>
  <getMapMimeTypeCheckingEnabled>false</getMapMimeTypeCheckingEnabled>
  <dynamicStylingDisabled>false</dynamicStylingDisabled>
  <featuresReprojectionDisabled>false</featuresReprojectionDisabled>
  <maxBuffer>0</maxBuffer>
  <maxRequestMemory>0</maxRequestMemory>
  <maxRenderingTime>0</maxRenderingTime>
  <maxRenderingErrors>0</maxRenderingErrors>
  <cacheConfiguration>
    <enabled>false</enabled>
    <maxEntries>1000</maxEntries>
    <maxEntrySize>51200</maxEntrySize>
  </cacheConfiguration>
</org.geoserver.wms.WMSInfoImpl>');
INSERT INTO public.object VALUES (11, 21, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', '<org.geoserver.wcs.WCSInfoImpl>
  <id>ServiceInfo.-1ec455f5:17a0e145460:-7ffd</id>
  <enabled>true</enabled>
  <name>WCS</name>
  <citeCompliant>false</citeCompliant>
  <schemaBaseURL>http://schemas.opengis.net</schemaBaseURL>
  <verbose>false</verbose>
  <gmlPrefixing>false</gmlPrefixing>
  <latLon>false</latLon>
  <maxInputMemory>-1</maxInputMemory>
  <maxOutputMemory>-1</maxOutputMemory>
  <subsamplingEnabled>true</subsamplingEnabled>
</org.geoserver.wcs.WCSInfoImpl>');
INSERT INTO public.object VALUES (7, 18, 'GeoServerInfo.global', '<global>
  <id>GeoServerInfo.global</id>
  <settings>
    <id>SettingsInfo.-1ec455f5:17a0e145460:-8000</id>
    <contact>
      <id>contact</id>
    </contact>
    <charset>UTF-8</charset>
    <numDecimals>4</numDecimals>
    <verbose>true</verbose>
    <verboseExceptions>false</verboseExceptions>
    <localWorkspaceIncludesPrefix>false</localWorkspaceIncludesPrefix>
    <showCreatedTimeColumnsInAdminList>false</showCreatedTimeColumnsInAdminList>
    <showModifiedTimeColumnsInAdminList>false</showModifiedTimeColumnsInAdminList>
  </settings>
  <jai>
    <allowInterpolation>false</allowInterpolation>
    <recycling>false</recycling>
    <tilePriority>5</tilePriority>
    <tileThreads>7</tileThreads>
    <memoryCapacity>0.5</memoryCapacity>
    <memoryThreshold>0.75</memoryThreshold>
    <imageIOCache>false</imageIOCache>
    <pngAcceleration>false</pngAcceleration>
    <jpegAcceleration>false</jpegAcceleration>
    <allowNativeMosaic>false</allowNativeMosaic>
    <allowNativeWarp>false</allowNativeWarp>
    <pngEncoderType>PNGJ</pngEncoderType>
    <jaiext>
      <jaiExtOperations class="sorted-set">
        <string>Affine</string>
        <string>BandCombine</string>
        <string>BandMerge</string>
        <string>BandSelect</string>
        <string>Binarize</string>
        <string>Border</string>
        <string>ColorConvert</string>
        <string>Crop</string>
        <string>ErrorDiffusion</string>
        <string>Format</string>
        <string>ImageFunction</string>
        <string>Lookup</string>
        <string>Mosaic</string>
        <string>Null</string>
        <string>OrderedDither</string>
        <string>Rescale</string>
        <string>Scale</string>
        <string>Stats</string>
        <string>Translate</string>
        <string>Warp</string>
        <string>algebric</string>
        <string>operationConst</string>
      </jaiExtOperations>
    </jaiext>
  </jai>
  <coverageAccess>
    <maxPoolSize>5</maxPoolSize>
    <corePoolSize>5</corePoolSize>
    <keepAliveTime>30000</keepAliveTime>
    <queueType>UNBOUNDED</queueType>
    <imageIOCacheThreshold>10240</imageIOCacheThreshold>
  </coverageAccess>
  <updateSequence>1</updateSequence>
  <featureTypeCacheSize>0</featureTypeCacheSize>
  <globalServices>true</globalServices>
  <useHeadersProxyURL>false</useHeadersProxyURL>
  <xmlPostRequestLogBufferSize>1024</xmlPostRequestLogBufferSize>
  <xmlExternalEntitiesEnabled>false</xmlExternalEntitiesEnabled>
  <webUIMode>DO_NOT_REDIRECT</webUIMode>
  <allowStoredQueriesPerWorkspace>true</allowStoredQueriesPerWorkspace>
</global>');


ALTER TABLE public.object ENABLE TRIGGER ALL;

--
-- TOC entry 4296 (class 0 OID 16417)
-- Dependencies: 213
-- Data for Name: property_type; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.property_type DISABLE TRIGGER ALL;

INSERT INTO public.property_type VALUES (1, NULL, 1, 'id', false, false);
INSERT INTO public.property_type VALUES (2, NULL, 1, 'isolated', false, false);
INSERT INTO public.property_type VALUES (3, NULL, 1, 'name', false, true);
INSERT INTO public.property_type VALUES (4, NULL, 2, 'id', false, false);
INSERT INTO public.property_type VALUES (5, NULL, 2, 'isolated', false, false);
INSERT INTO public.property_type VALUES (6, NULL, 2, 'name', false, true);
INSERT INTO public.property_type VALUES (7, NULL, 2, 'prefix', false, true);
INSERT INTO public.property_type VALUES (8, NULL, 2, 'URI', false, true);
INSERT INTO public.property_type VALUES (9, NULL, 3, 'description', false, true);
INSERT INTO public.property_type VALUES (10, NULL, 3, 'enabled', false, false);
INSERT INTO public.property_type VALUES (11, NULL, 3, 'id', false, false);
INSERT INTO public.property_type VALUES (12, NULL, 3, 'name', false, true);
INSERT INTO public.property_type VALUES (13, NULL, 3, 'type', false, true);
INSERT INTO public.property_type VALUES (14, NULL, 4, 'description', false, true);
INSERT INTO public.property_type VALUES (15, NULL, 4, 'enabled', false, false);
INSERT INTO public.property_type VALUES (16, NULL, 4, 'id', false, false);
INSERT INTO public.property_type VALUES (17, NULL, 4, 'name', false, true);
INSERT INTO public.property_type VALUES (18, NULL, 4, 'type', false, true);
INSERT INTO public.property_type VALUES (19, NULL, 4, 'URL', false, true);
INSERT INTO public.property_type VALUES (20, NULL, 5, 'capabilitiesURL', false, true);
INSERT INTO public.property_type VALUES (21, NULL, 5, 'connectTimeout', false, false);
INSERT INTO public.property_type VALUES (22, NULL, 5, 'description', false, true);
INSERT INTO public.property_type VALUES (23, NULL, 5, 'enabled', false, false);
INSERT INTO public.property_type VALUES (24, NULL, 5, 'id', false, false);
INSERT INTO public.property_type VALUES (25, NULL, 5, 'maxConnections', false, false);
INSERT INTO public.property_type VALUES (26, NULL, 5, 'name', false, true);
INSERT INTO public.property_type VALUES (27, NULL, 5, 'password', false, true);
INSERT INTO public.property_type VALUES (28, NULL, 5, 'readTimeout', false, false);
INSERT INTO public.property_type VALUES (29, NULL, 5, 'type', false, true);
INSERT INTO public.property_type VALUES (30, NULL, 5, 'useConnectionPooling', false, false);
INSERT INTO public.property_type VALUES (31, NULL, 5, 'username', false, true);
INSERT INTO public.property_type VALUES (32, NULL, 6, 'capabilitiesURL', false, true);
INSERT INTO public.property_type VALUES (33, NULL, 6, 'connectTimeout', false, false);
INSERT INTO public.property_type VALUES (34, NULL, 6, 'description', false, true);
INSERT INTO public.property_type VALUES (35, NULL, 6, 'enabled', false, false);
INSERT INTO public.property_type VALUES (36, NULL, 6, 'headerName', false, true);
INSERT INTO public.property_type VALUES (37, NULL, 6, 'headerValue', false, true);
INSERT INTO public.property_type VALUES (38, NULL, 6, 'id', false, false);
INSERT INTO public.property_type VALUES (39, NULL, 6, 'maxConnections', false, false);
INSERT INTO public.property_type VALUES (40, NULL, 6, 'name', false, true);
INSERT INTO public.property_type VALUES (41, NULL, 6, 'password', false, true);
INSERT INTO public.property_type VALUES (42, NULL, 6, 'readTimeout', false, false);
INSERT INTO public.property_type VALUES (43, NULL, 6, 'type', false, true);
INSERT INTO public.property_type VALUES (44, NULL, 6, 'useConnectionPooling', false, false);
INSERT INTO public.property_type VALUES (45, NULL, 6, 'username', false, true);
INSERT INTO public.property_type VALUES (46, NULL, 7, 'description', false, true);
INSERT INTO public.property_type VALUES (47, NULL, 7, 'enabled', false, false);
INSERT INTO public.property_type VALUES (48, NULL, 7, 'id', false, false);
INSERT INTO public.property_type VALUES (49, NULL, 7, 'name', false, true);
INSERT INTO public.property_type VALUES (50, NULL, 7, 'type', false, true);
INSERT INTO public.property_type VALUES (51, NULL, 8, 'abstract', false, true);
INSERT INTO public.property_type VALUES (52, NULL, 8, 'advertised', false, false);
INSERT INTO public.property_type VALUES (53, NULL, 8, 'circularArcPresent', false, false);
INSERT INTO public.property_type VALUES (54, NULL, 8, 'cqlFilter', false, true);
INSERT INTO public.property_type VALUES (55, NULL, 8, 'description', false, true);
INSERT INTO public.property_type VALUES (56, NULL, 8, 'enabled', false, false);
INSERT INTO public.property_type VALUES (57, NULL, 8, 'encodeMeasures', false, false);
INSERT INTO public.property_type VALUES (58, NULL, 8, 'forcedDecimal', false, false);
INSERT INTO public.property_type VALUES (59, NULL, 8, 'id', false, false);
INSERT INTO public.property_type VALUES (60, NULL, 8, 'maxFeatures', false, false);
INSERT INTO public.property_type VALUES (61, NULL, 8, 'name', false, true);
INSERT INTO public.property_type VALUES (62, NULL, 8, 'nativeName', false, true);
INSERT INTO public.property_type VALUES (63, NULL, 8, 'numDecimals', false, false);
INSERT INTO public.property_type VALUES (64, NULL, 8, 'overridingServiceSRS', false, false);
INSERT INTO public.property_type VALUES (65, NULL, 8, 'padWithZeros', false, false);
INSERT INTO public.property_type VALUES (66, NULL, 8, 'projectionPolicy', false, true);
INSERT INTO public.property_type VALUES (67, NULL, 8, 'SRS', false, true);
INSERT INTO public.property_type VALUES (68, NULL, 8, 'serviceConfiguration', false, false);
INSERT INTO public.property_type VALUES (69, NULL, 8, 'simpleConversionEnabled', false, false);
INSERT INTO public.property_type VALUES (70, NULL, 8, 'skipNumberMatched', false, false);
INSERT INTO public.property_type VALUES (71, NULL, 8, 'title', false, true);
INSERT INTO public.property_type VALUES (72, NULL, 8, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (73, NULL, 9, 'abstract', false, true);
INSERT INTO public.property_type VALUES (74, NULL, 9, 'advertised', false, false);
INSERT INTO public.property_type VALUES (75, NULL, 9, 'defaultInterpolationMethod', false, true);
INSERT INTO public.property_type VALUES (76, NULL, 9, 'description', false, true);
INSERT INTO public.property_type VALUES (77, NULL, 9, 'enabled', false, false);
INSERT INTO public.property_type VALUES (78, NULL, 9, 'id', false, false);
INSERT INTO public.property_type VALUES (79, NULL, 9, 'name', false, true);
INSERT INTO public.property_type VALUES (80, NULL, 9, 'nativeCoverageName', false, true);
INSERT INTO public.property_type VALUES (81, NULL, 9, 'nativeFormat', false, true);
INSERT INTO public.property_type VALUES (82, NULL, 9, 'nativeName', false, true);
INSERT INTO public.property_type VALUES (83, NULL, 9, 'projectionPolicy', false, true);
INSERT INTO public.property_type VALUES (84, NULL, 9, 'SRS', false, true);
INSERT INTO public.property_type VALUES (85, NULL, 9, 'serviceConfiguration', false, false);
INSERT INTO public.property_type VALUES (86, NULL, 9, 'simpleConversionEnabled', false, false);
INSERT INTO public.property_type VALUES (87, NULL, 9, 'title', false, true);
INSERT INTO public.property_type VALUES (88, NULL, 9, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (89, NULL, 10, 'abstract', false, true);
INSERT INTO public.property_type VALUES (90, NULL, 10, 'advertised', false, false);
INSERT INTO public.property_type VALUES (91, NULL, 10, 'description', false, true);
INSERT INTO public.property_type VALUES (92, NULL, 10, 'enabled', false, false);
INSERT INTO public.property_type VALUES (93, NULL, 10, 'forcedRemoteStyle', false, true);
INSERT INTO public.property_type VALUES (94, NULL, 10, 'id', false, false);
INSERT INTO public.property_type VALUES (95, NULL, 10, 'maxScale', false, false);
INSERT INTO public.property_type VALUES (96, NULL, 10, 'metadataBBoxRespected', false, false);
INSERT INTO public.property_type VALUES (97, NULL, 10, 'minScale', false, false);
INSERT INTO public.property_type VALUES (98, NULL, 10, 'name', false, true);
INSERT INTO public.property_type VALUES (99, NULL, 10, 'nativeName', false, true);
INSERT INTO public.property_type VALUES (100, NULL, 10, 'preferredFormat', false, true);
INSERT INTO public.property_type VALUES (101, NULL, 10, 'projectionPolicy', false, true);
INSERT INTO public.property_type VALUES (102, NULL, 10, 'SRS', false, true);
INSERT INTO public.property_type VALUES (103, NULL, 10, 'serviceConfiguration', false, false);
INSERT INTO public.property_type VALUES (104, NULL, 10, 'simpleConversionEnabled', false, false);
INSERT INTO public.property_type VALUES (105, NULL, 10, 'title', false, true);
INSERT INTO public.property_type VALUES (106, NULL, 10, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (107, NULL, 11, 'abstract', false, true);
INSERT INTO public.property_type VALUES (108, NULL, 11, 'advertised', false, false);
INSERT INTO public.property_type VALUES (109, NULL, 11, 'description', false, true);
INSERT INTO public.property_type VALUES (110, NULL, 11, 'enabled', false, false);
INSERT INTO public.property_type VALUES (111, NULL, 11, 'id', false, false);
INSERT INTO public.property_type VALUES (112, NULL, 11, 'name', false, true);
INSERT INTO public.property_type VALUES (113, NULL, 11, 'nativeName', false, true);
INSERT INTO public.property_type VALUES (114, NULL, 11, 'projectionPolicy', false, true);
INSERT INTO public.property_type VALUES (115, NULL, 11, 'SRS', false, true);
INSERT INTO public.property_type VALUES (116, NULL, 11, 'serviceConfiguration', false, false);
INSERT INTO public.property_type VALUES (117, NULL, 11, 'simpleConversionEnabled', false, false);
INSERT INTO public.property_type VALUES (118, NULL, 11, 'title', false, true);
INSERT INTO public.property_type VALUES (119, NULL, 11, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (120, NULL, 12, 'abstract', false, true);
INSERT INTO public.property_type VALUES (121, NULL, 12, 'advertised', false, false);
INSERT INTO public.property_type VALUES (122, NULL, 12, 'description', false, true);
INSERT INTO public.property_type VALUES (123, NULL, 12, 'enabled', false, false);
INSERT INTO public.property_type VALUES (124, NULL, 12, 'id', false, false);
INSERT INTO public.property_type VALUES (125, NULL, 12, 'name', false, true);
INSERT INTO public.property_type VALUES (126, NULL, 12, 'nativeName', false, true);
INSERT INTO public.property_type VALUES (127, NULL, 12, 'projectionPolicy', false, true);
INSERT INTO public.property_type VALUES (128, NULL, 12, 'SRS', false, true);
INSERT INTO public.property_type VALUES (129, NULL, 12, 'serviceConfiguration', false, false);
INSERT INTO public.property_type VALUES (130, NULL, 12, 'simpleConversionEnabled', false, false);
INSERT INTO public.property_type VALUES (131, NULL, 12, 'title', false, true);
INSERT INTO public.property_type VALUES (132, NULL, 12, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (133, NULL, 13, 'abstract', false, true);
INSERT INTO public.property_type VALUES (134, NULL, 13, 'advertised', false, false);
INSERT INTO public.property_type VALUES (135, NULL, 13, 'enabled', false, false);
INSERT INTO public.property_type VALUES (136, NULL, 13, 'id', false, false);
INSERT INTO public.property_type VALUES (137, NULL, 13, 'name', false, true);
INSERT INTO public.property_type VALUES (138, NULL, 13, 'title', false, true);
INSERT INTO public.property_type VALUES (139, NULL, 13, 'type', false, true);
INSERT INTO public.property_type VALUES (140, NULL, 13, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (141, NULL, 14, 'abstract', false, true);
INSERT INTO public.property_type VALUES (142, NULL, 14, 'advertised', false, false);
INSERT INTO public.property_type VALUES (143, NULL, 14, 'defaultWMSInterpolationMethod', false, true);
INSERT INTO public.property_type VALUES (144, NULL, 14, 'enabled', false, false);
INSERT INTO public.property_type VALUES (145, NULL, 14, 'id', false, false);
INSERT INTO public.property_type VALUES (146, NULL, 14, 'name', false, true);
INSERT INTO public.property_type VALUES (147, NULL, 14, 'opaque', false, false);
INSERT INTO public.property_type VALUES (148, NULL, 14, 'path', false, true);
INSERT INTO public.property_type VALUES (149, NULL, 14, 'queryable', false, false);
INSERT INTO public.property_type VALUES (150, NULL, 14, 'title', false, true);
INSERT INTO public.property_type VALUES (151, NULL, 14, 'type', false, true);
INSERT INTO public.property_type VALUES (152, NULL, 14, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (153, NULL, 15, 'abstract', false, true);
INSERT INTO public.property_type VALUES (154, NULL, 15, 'advertised', false, false);
INSERT INTO public.property_type VALUES (155, NULL, 15, 'enabled', false, false);
INSERT INTO public.property_type VALUES (156, NULL, 15, 'id', false, false);
INSERT INTO public.property_type VALUES (157, NULL, 15, 'mode', false, true);
INSERT INTO public.property_type VALUES (158, NULL, 15, 'name', false, true);
INSERT INTO public.property_type VALUES (159, NULL, 15, 'queryDisabled', false, false);
INSERT INTO public.property_type VALUES (160, NULL, 15, 'title', false, true);
INSERT INTO public.property_type VALUES (161, NULL, 15, 'type', false, true);
INSERT INTO public.property_type VALUES (162, NULL, 15, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (163, NULL, 16, 'enabled', false, false);
INSERT INTO public.property_type VALUES (164, NULL, 16, 'id', false, false);
INSERT INTO public.property_type VALUES (165, NULL, 16, 'name', false, true);
INSERT INTO public.property_type VALUES (166, NULL, 17, 'filename', false, true);
INSERT INTO public.property_type VALUES (167, NULL, 17, 'format', false, true);
INSERT INTO public.property_type VALUES (168, NULL, 17, 'id', false, false);
INSERT INTO public.property_type VALUES (169, NULL, 17, 'name', false, true);
INSERT INTO public.property_type VALUES (170, NULL, 17, 'prefixedName', false, true);
INSERT INTO public.property_type VALUES (171, NULL, 18, 'adminPassword', false, true);
INSERT INTO public.property_type VALUES (172, NULL, 18, 'adminUsername', false, true);
INSERT INTO public.property_type VALUES (173, NULL, 18, 'allowStoredQueriesPerWorkspace', false, false);
INSERT INTO public.property_type VALUES (174, NULL, 18, 'featureTypeCacheSize', false, false);
INSERT INTO public.property_type VALUES (175, NULL, 18, 'globalServices', false, false);
INSERT INTO public.property_type VALUES (176, NULL, 18, 'id', false, false);
INSERT INTO public.property_type VALUES (177, NULL, 18, 'lockProviderName', false, true);
INSERT INTO public.property_type VALUES (178, NULL, 18, 'resourceErrorHandling', false, true);
INSERT INTO public.property_type VALUES (179, NULL, 18, 'updateSequence', false, false);
INSERT INTO public.property_type VALUES (180, NULL, 18, 'useHeadersProxyURL', false, false);
INSERT INTO public.property_type VALUES (181, NULL, 18, 'webUIMode', false, true);
INSERT INTO public.property_type VALUES (182, NULL, 18, 'xmlExternalEntitiesEnabled', false, false);
INSERT INTO public.property_type VALUES (183, NULL, 18, 'xmlPostRequestLogBufferSize', false, false);
INSERT INTO public.property_type VALUES (184, NULL, 19, 'id', false, false);
INSERT INTO public.property_type VALUES (185, NULL, 19, 'level', false, true);
INSERT INTO public.property_type VALUES (186, NULL, 19, 'location', false, true);
INSERT INTO public.property_type VALUES (187, NULL, 19, 'stdOutLogging', false, false);
INSERT INTO public.property_type VALUES (188, NULL, 20, 'charset', false, true);
INSERT INTO public.property_type VALUES (189, NULL, 20, 'id', false, false);
INSERT INTO public.property_type VALUES (190, NULL, 20, 'localWorkspaceIncludesPrefix', false, false);
INSERT INTO public.property_type VALUES (191, NULL, 20, 'numDecimals', false, false);
INSERT INTO public.property_type VALUES (192, NULL, 20, 'onlineResource', false, true);
INSERT INTO public.property_type VALUES (193, NULL, 20, 'proxyBaseUrl', false, true);
INSERT INTO public.property_type VALUES (194, NULL, 20, 'schemaBaseUrl', false, true);
INSERT INTO public.property_type VALUES (195, NULL, 20, 'showCreatedTimeColumnsInAdminList', false, false);
INSERT INTO public.property_type VALUES (196, NULL, 20, 'showModifiedTimeColumnsInAdminList', false, false);
INSERT INTO public.property_type VALUES (197, NULL, 20, 'title', false, true);
INSERT INTO public.property_type VALUES (198, NULL, 20, 'verbose', false, false);
INSERT INTO public.property_type VALUES (199, NULL, 20, 'verboseExceptions', false, false);
INSERT INTO public.property_type VALUES (200, NULL, 21, 'abstract', false, true);
INSERT INTO public.property_type VALUES (201, NULL, 21, 'accessConstraints', false, true);
INSERT INTO public.property_type VALUES (202, NULL, 21, 'citeCompliant', false, false);
INSERT INTO public.property_type VALUES (203, NULL, 21, 'enabled', false, false);
INSERT INTO public.property_type VALUES (204, NULL, 21, 'fees', false, true);
INSERT INTO public.property_type VALUES (205, NULL, 21, 'id', false, false);
INSERT INTO public.property_type VALUES (206, NULL, 21, 'maintainer', false, true);
INSERT INTO public.property_type VALUES (207, NULL, 21, 'name', false, true);
INSERT INTO public.property_type VALUES (208, NULL, 21, 'onlineResource', false, true);
INSERT INTO public.property_type VALUES (209, NULL, 21, 'outputStrategy', false, true);
INSERT INTO public.property_type VALUES (210, NULL, 21, 'schemaBaseURL', false, true);
INSERT INTO public.property_type VALUES (211, NULL, 21, 'title', false, true);
INSERT INTO public.property_type VALUES (212, NULL, 21, 'verbose', false, false);
INSERT INTO public.property_type VALUES (213, 3, 3, 'workspace.name', false, true);
INSERT INTO public.property_type VALUES (214, 1, 3, 'workspace.id', false, false);
INSERT INTO public.property_type VALUES (215, 1, 4, 'workspace.id', false, false);
INSERT INTO public.property_type VALUES (216, 3, 4, 'workspace.name', false, false);
INSERT INTO public.property_type VALUES (217, 1, 5, 'workspace.id', false, false);
INSERT INTO public.property_type VALUES (218, 3, 5, 'workspace.name', false, true);
INSERT INTO public.property_type VALUES (219, 6, 8, 'namespace.name', false, true);
INSERT INTO public.property_type VALUES (220, 8, 8, 'namespace.URI', false, false);
INSERT INTO public.property_type VALUES (221, NULL, 8, 'keywords.value', true, true);
INSERT INTO public.property_type VALUES (222, 12, 8, 'store.name', false, true);
INSERT INTO public.property_type VALUES (223, 7, 8, 'namespace.prefix', false, true);
INSERT INTO public.property_type VALUES (224, 4, 8, 'namespace.id', false, false);
INSERT INTO public.property_type VALUES (225, 11, 8, 'store.id', false, false);
INSERT INTO public.property_type VALUES (226, 8, 9, 'namespace.URI', false, false);
INSERT INTO public.property_type VALUES (227, 6, 9, 'namespace.name', false, true);
INSERT INTO public.property_type VALUES (228, 4, 9, 'namespace.id', false, false);
INSERT INTO public.property_type VALUES (229, 16, 9, 'store.id', false, false);
INSERT INTO public.property_type VALUES (230, 7, 9, 'namespace.prefix', false, true);
INSERT INTO public.property_type VALUES (231, 17, 9, 'store.name', false, true);
INSERT INTO public.property_type VALUES (232, NULL, 9, 'keywords.value', true, true);
INSERT INTO public.property_type VALUES (233, 6, 10, 'namespace.name', false, true);
INSERT INTO public.property_type VALUES (234, NULL, 10, 'keywords.value', true, true);
INSERT INTO public.property_type VALUES (235, 8, 10, 'namespace.URI', false, false);
INSERT INTO public.property_type VALUES (236, 26, 10, 'store.name', false, true);
INSERT INTO public.property_type VALUES (237, 7, 10, 'namespace.prefix', false, true);
INSERT INTO public.property_type VALUES (238, 4, 10, 'namespace.id', false, false);
INSERT INTO public.property_type VALUES (239, 24, 10, 'store.id', false, false);
INSERT INTO public.property_type VALUES (240, NULL, 12, 'keywords.value', true, true);
INSERT INTO public.property_type VALUES (241, 124, 14, 'resource.id', false, false);
INSERT INTO public.property_type VALUES (242, 240, 14, 'resource.keywords.value', true, true);
INSERT INTO public.property_type VALUES (243, 8, 14, 'resource.namespace.URI', false, false);
INSERT INTO public.property_type VALUES (244, 121, 14, 'resource.advertised', false, false);
INSERT INTO public.property_type VALUES (245, 6, 14, 'resource.namespace.name', false, false);
INSERT INTO public.property_type VALUES (246, 128, 14, 'resource.SRS', false, true);
INSERT INTO public.property_type VALUES (247, 120, 14, 'resource.abstract', false, true);
INSERT INTO public.property_type VALUES (248, 123, 14, 'resource.enabled', false, false);
INSERT INTO public.property_type VALUES (249, 122, 14, 'resource.description', false, true);
INSERT INTO public.property_type VALUES (250, 48, 14, 'resource.store.id', false, false);
INSERT INTO public.property_type VALUES (251, 168, 14, 'defaultStyle.id', false, false);
INSERT INTO public.property_type VALUES (252, 169, 14, 'styles.name', true, true);
INSERT INTO public.property_type VALUES (253, 169, 14, 'defaultStyle.name', false, false);
INSERT INTO public.property_type VALUES (254, 168, 14, 'styles.id', true, false);
INSERT INTO public.property_type VALUES (255, 47, 14, 'resource.store.enabled', false, false);
INSERT INTO public.property_type VALUES (256, 1, 14, 'resource.store.workspace.id', false, false);
INSERT INTO public.property_type VALUES (257, 3, 14, 'resource.store.workspace.name', false, true);
INSERT INTO public.property_type VALUES (258, 166, 14, 'defaultStyle.filename', false, false);
INSERT INTO public.property_type VALUES (259, 49, 14, 'resource.store.name', false, true);
INSERT INTO public.property_type VALUES (260, 166, 14, 'styles.filename', true, false);
INSERT INTO public.property_type VALUES (261, 7, 14, 'resource.namespace.prefix', false, false);
INSERT INTO public.property_type VALUES (262, 125, 14, 'resource.name', false, false);
INSERT INTO public.property_type VALUES (263, 4, 14, 'resource.namespace.id', false, false);
INSERT INTO public.property_type VALUES (264, 145, 15, 'layers.id', true, false);
INSERT INTO public.property_type VALUES (265, 146, 15, 'layers.name', true, true);
INSERT INTO public.property_type VALUES (266, 3, 15, 'workspace.name', false, true);
INSERT INTO public.property_type VALUES (267, 169, 15, 'rootLayerStyle.name', false, true);
INSERT INTO public.property_type VALUES (268, 146, 15, 'rootLayer.name', false, true);
INSERT INTO public.property_type VALUES (269, 168, 15, 'rootLayerStyle.id', false, false);
INSERT INTO public.property_type VALUES (270, 145, 15, 'rootLayer.id', false, false);
INSERT INTO public.property_type VALUES (271, 168, 15, 'styles.id', true, false);
INSERT INTO public.property_type VALUES (272, 169, 15, 'styles.name', true, true);
INSERT INTO public.property_type VALUES (273, 1, 15, 'workspace.id', false, false);
INSERT INTO public.property_type VALUES (274, 3, 17, 'workspace.name', false, false);
INSERT INTO public.property_type VALUES (275, 1, 17, 'workspace.id', false, false);
INSERT INTO public.property_type VALUES (276, 3, 18, 'settings.workspace.name', false, true);
INSERT INTO public.property_type VALUES (277, 1, 18, 'settings.workspace.id', false, false);
INSERT INTO public.property_type VALUES (278, 189, 18, 'settings.id', false, false);
INSERT INTO public.property_type VALUES (279, 3, 20, 'workspace.name', false, true);
INSERT INTO public.property_type VALUES (280, NULL, 20, 'contact.onlineResource', false, false);
INSERT INTO public.property_type VALUES (281, NULL, 20, 'contact.address', false, false);
INSERT INTO public.property_type VALUES (282, 1, 20, 'workspace.id', false, false);
INSERT INTO public.property_type VALUES (283, NULL, 21, 'keywords.value', true, true);
INSERT INTO public.property_type VALUES (284, 3, 21, 'workspace.name', false, true);
INSERT INTO public.property_type VALUES (285, 1, 21, 'workspace.id', false, false);


ALTER TABLE public.property_type ENABLE TRIGGER ALL;

--
-- TOC entry 4292 (class 0 OID 16396)
-- Dependencies: 209
-- Data for Name: object_property; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.object_property DISABLE TRIGGER ALL;

INSERT INTO public.object_property VALUES (1, 275, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (1, 166, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, 'default_point.sld');
INSERT INTO public.object_property VALUES (1, 167, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, 'sld');
INSERT INTO public.object_property VALUES (1, 169, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, 'point');
INSERT INTO public.object_property VALUES (1, 170, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, 'point');
INSERT INTO public.object_property VALUES (1, 168, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c');
INSERT INTO public.object_property VALUES (1, 274, 'StyleInfoImpl-21d5ab24-81fc-4522-9410-bd822acb4a6c', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (2, 275, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (2, 166, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, 'default_line.sld');
INSERT INTO public.object_property VALUES (2, 167, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, 'sld');
INSERT INTO public.object_property VALUES (2, 169, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, 'line');
INSERT INTO public.object_property VALUES (2, 170, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, 'line');
INSERT INTO public.object_property VALUES (2, 168, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391');
INSERT INTO public.object_property VALUES (2, 274, 'StyleInfoImpl-19dc94a1-89dd-4609-9fa5-63b3fda20391', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (3, 275, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (3, 166, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, 'default_polygon.sld');
INSERT INTO public.object_property VALUES (3, 167, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, 'sld');
INSERT INTO public.object_property VALUES (3, 169, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, 'polygon');
INSERT INTO public.object_property VALUES (3, 170, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, 'polygon');
INSERT INTO public.object_property VALUES (3, 168, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95');
INSERT INTO public.object_property VALUES (3, 274, 'StyleInfoImpl-a3822c0f-7533-48a9-a067-5aa5afa68e95', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (4, 275, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (4, 166, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, 'default_raster.sld');
INSERT INTO public.object_property VALUES (4, 167, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, 'sld');
INSERT INTO public.object_property VALUES (4, 169, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, 'raster');
INSERT INTO public.object_property VALUES (4, 170, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, 'raster');
INSERT INTO public.object_property VALUES (4, 168, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761');
INSERT INTO public.object_property VALUES (4, 274, 'StyleInfoImpl-a8529a7d-167b-4407-8ece-f930678a5761', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (5, 275, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (5, 166, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, 'default_generic.sld');
INSERT INTO public.object_property VALUES (5, 167, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, 'sld');
INSERT INTO public.object_property VALUES (5, 169, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, 'generic');
INSERT INTO public.object_property VALUES (5, 170, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, 'generic');
INSERT INTO public.object_property VALUES (5, 168, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912');
INSERT INTO public.object_property VALUES (5, 274, 'StyleInfoImpl-c6e77751-fffd-4a62-9a6b-88597f12e912', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 191, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, '4');
INSERT INTO public.object_property VALUES (6, 188, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'UTF-8');
INSERT INTO public.object_property VALUES (6, 282, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 190, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (6, 196, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (6, 281, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 192, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 197, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 198, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'true');
INSERT INTO public.object_property VALUES (6, 199, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (6, 194, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 189, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'SettingsInfo.-1ec455f5:17a0e145460:-8000');
INSERT INTO public.object_property VALUES (6, 195, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (6, 279, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 193, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (6, 280, 'SettingsInfo.-1ec455f5:17a0e145460:-8000', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 182, 'GeoServerInfo.global', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (7, 278, 'GeoServerInfo.global', 6, 189, 0, 'SettingsInfo.-1ec455f5:17a0e145460:-8000');
INSERT INTO public.object_property VALUES (7, 177, 'GeoServerInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 276, 'GeoServerInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 173, 'GeoServerInfo.global', NULL, NULL, 0, 'true');
INSERT INTO public.object_property VALUES (7, 172, 'GeoServerInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 174, 'GeoServerInfo.global', NULL, NULL, 0, '0');
INSERT INTO public.object_property VALUES (7, 183, 'GeoServerInfo.global', NULL, NULL, 0, '1024');
INSERT INTO public.object_property VALUES (7, 180, 'GeoServerInfo.global', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (7, 175, 'GeoServerInfo.global', NULL, NULL, 0, 'true');
INSERT INTO public.object_property VALUES (7, 176, 'GeoServerInfo.global', NULL, NULL, 0, 'GeoServerInfo.global');
INSERT INTO public.object_property VALUES (7, 178, 'GeoServerInfo.global', NULL, NULL, 0, 'SKIP_MISCONFIGURED_LAYERS');
INSERT INTO public.object_property VALUES (7, 277, 'GeoServerInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 171, 'GeoServerInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (8, 185, 'LoggingInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (8, 186, 'LoggingInfo.global', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (8, 184, 'LoggingInfo.global', NULL, NULL, 0, 'LoggingInfo.global');
INSERT INTO public.object_property VALUES (8, 187, 'LoggingInfo.global', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (9, 285, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 204, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 209, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 200, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 208, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 211, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 203, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, 'true');
INSERT INTO public.object_property VALUES (9, 206, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 212, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (9, 202, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (9, 210, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, 'http://schemas.opengis.net');
INSERT INTO public.object_property VALUES (9, 207, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, 'WFS');
INSERT INTO public.object_property VALUES (9, 205, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff');
INSERT INTO public.object_property VALUES (9, 201, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (9, 284, 'ServiceInfo.-1ec455f5:17a0e145460:-7fff', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 285, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 179, 'GeoServerInfo.global', NULL, NULL, 0, '1');
INSERT INTO public.object_property VALUES (10, 204, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 209, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 200, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 208, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 211, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 203, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, 'true');
INSERT INTO public.object_property VALUES (10, 206, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 212, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (10, 202, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (10, 210, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, 'http://schemas.opengis.net');
INSERT INTO public.object_property VALUES (10, 207, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, 'WMS');
INSERT INTO public.object_property VALUES (10, 205, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe');
INSERT INTO public.object_property VALUES (10, 201, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (10, 284, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffe', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 285, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 204, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 209, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 200, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 208, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 211, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 203, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, 'true');
INSERT INTO public.object_property VALUES (11, 206, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 212, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (11, 202, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, 'false');
INSERT INTO public.object_property VALUES (11, 210, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, 'http://schemas.opengis.net');
INSERT INTO public.object_property VALUES (11, 207, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, 'WCS');
INSERT INTO public.object_property VALUES (11, 205, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd');
INSERT INTO public.object_property VALUES (11, 201, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (11, 284, 'ServiceInfo.-1ec455f5:17a0e145460:-7ffd', NULL, NULL, 0, NULL);
INSERT INTO public.object_property VALUES (7, 181, 'GeoServerInfo.global', NULL, NULL, 0, 'DO_NOT_REDIRECT');


ALTER TABLE public.object_property ENABLE TRIGGER ALL;

--
-- TOC entry 4299 (class 0 OID 19754)
-- Dependencies: 293
-- Data for Name: resources; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.resources DISABLE TRIGGER ALL;

INSERT INTO public.resources VALUES (52, 'restInterceptor', 36, '2021-06-15 05:10:53.194', NULL);
INSERT INTO public.resources VALUES (49, 'interceptor', 36, '2021-06-15 05:10:53.167', NULL);
INSERT INTO public.resources VALUES (2, 'default_point.sld', 1, '2021-06-15 05:10:50.857', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d2249534f2d383835392d31223f3e0a3c5374796c65644c6179657244657363726970746f722076657273696f6e3d22312e302e30220a09097873693a736368656d614c6f636174696f6e3d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64205374796c65644c6179657244657363726970746f722e787364220a0909786d6c6e733d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64220a0909786d6c6e733a6f67633d22687474703a2f2f7777772e6f70656e6769732e6e65742f6f6763220a0909786d6c6e733a786c696e6b3d22687474703a2f2f7777772e77332e6f72672f313939392f786c696e6b220a0909786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e6365223e0a09093c212d2d2061206e616d6564206c6179657220697320746865206261736963206275696c64696e6720626c6f636b206f6620616e20736c6420646f63756d656e74202d2d3e0a0a093c4e616d65644c617965723e0a09093c4e616d653e44656661756c7420506f696e743c2f4e616d653e0a09093c557365725374796c653e0a0909202020203c212d2d20746865792068617665206e616d65732c207469746c657320616e6420616273747261637473202d2d3e0a0a0909093c5469746c653e5265642053717561726520706f696e743c2f5469746c653e0a0909093c41627374726163743e412073616d706c65207374796c652074686174206a757374207072696e7473206f7574206120726564207371756172653c2f41627374726163743e0a0909093c212d2d2046656174757265547970655374796c657320646573637269626520686f7720746f2072656e64657220646966666572656e74206665617475726573202d2d3e0a0909093c212d2d20612066656174757265207479706520666f7220706f696e7473202d2d3e0a0a0909093c46656174757265547970655374796c653e0a090909093c212d2d46656174757265547970654e616d653e466561747572653c2f46656174757265547970654e616d652d2d3e0a090909093c52756c653e0a09090909093c4e616d653e52756c6520313c2f4e616d653e0a09090909093c5469746c653e5265642053717561726520706f696e743c2f5469746c653e0a09090909093c41627374726163743e41207265642066696c6c2077697468203620706978656c732073697a653c2f41627374726163743e0a0a09090909093c212d2d206c696b652061206c696e6573796d626f6c697a657220627574207769746820612066696c6c20746f6f202d2d3e0a09090909093c506f696e7453796d626f6c697a65723e0a0909090909093c477261706869633e0a090909090909093c4d61726b3e0a09090909090909093c57656c6c4b6e6f776e4e616d653e7371756172653c2f57656c6c4b6e6f776e4e616d653e0a09090909090909093c46696c6c3e0a0909090909090909093c437373506172616d65746572206e616d653d2266696c6c223e234646303030303c2f437373506172616d657465723e0a09090909090909093c2f46696c6c3e0a090909090909093c2f4d61726b3e0a090909090909093c53697a653e363c2f53697a653e0a0909090909093c2f477261706869633e0a09090909093c2f506f696e7453796d626f6c697a65723e0a090909093c2f52756c653e0a0a0909202020203c2f46656174757265547970655374796c653e0a09093c2f557365725374796c653e0a093c2f4e616d65644c617965723e0a3c2f5374796c65644c6179657244657363726970746f723e0a');
INSERT INTO public.resources VALUES (3, 'point.xml', 1, '2021-06-15 05:10:51.001', '\x3c7374796c653e0a20203c69643e5374796c65496e666f496d706c2d32316435616232342d383166632d343532322d393431302d6264383232616362346136633c2f69643e0a20203c6e616d653e706f696e743c2f6e616d653e0a20203c666f726d61743e736c643c2f666f726d61743e0a20203c6c616e677561676556657273696f6e3e0a202020203c76657273696f6e3e312e302e303c2f76657273696f6e3e0a20203c2f6c616e677561676556657273696f6e3e0a20203c66696c656e616d653e64656661756c745f706f696e742e736c643c2f66696c656e616d653e0a3c2f7374796c653e');
INSERT INTO public.resources VALUES (55, 'config.xml', 54, '2021-06-15 05:10:53.154', '\x3c6c6f676f757446696c7465723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376665663c2f69643e0a20203c6e616d653e666f726d4c6f676f75743c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f5365727665724c6f676f757446696c7465723c2f636c6173734e616d653e0a20203c726564697265637455524c3e2f7765622f3c2f726564697265637455524c3e0a20203c666f726d4c6f676f7574436861696e3e2f6a5f737072696e675f73656375726974795f6c6f676f75742c2f6a5f737072696e675f73656375726974795f6c6f676f75742f2c2f6c6f676f75743c2f666f726d4c6f676f7574436861696e3e0a3c2f6c6f676f757446696c7465723e');
INSERT INTO public.resources VALUES (54, 'formLogout', 36, '2021-06-15 05:10:53.154', NULL);
INSERT INTO public.resources VALUES (4, 'default_line.sld', 1, '2021-06-15 05:10:51.012', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d2249534f2d383835392d31223f3e0a3c5374796c65644c6179657244657363726970746f722076657273696f6e3d22312e302e30220a09097873693a736368656d614c6f636174696f6e3d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64205374796c65644c6179657244657363726970746f722e787364220a0909786d6c6e733d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64220a0909786d6c6e733a6f67633d22687474703a2f2f7777772e6f70656e6769732e6e65742f6f6763220a0909786d6c6e733a786c696e6b3d22687474703a2f2f7777772e77332e6f72672f313939392f786c696e6b220a0909786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e6365223e0a09093c212d2d2061206e616d6564206c6179657220697320746865206261736963206275696c64696e6720626c6f636b206f6620616e20736c6420646f63756d656e74202d2d3e0a0a093c4e616d65644c617965723e0a09093c4e616d653e44656661756c74204c696e653c2f4e616d653e0a09093c557365725374796c653e0a0909202020203c212d2d20746865792068617665206e616d65732c207469746c657320616e6420616273747261637473202d2d3e0a0a0909093c5469746c653e426c7565204c696e653c2f5469746c653e0a0909093c41627374726163743e412073616d706c65207374796c652074686174206a757374207072696e7473206f7574206120626c7565206c696e653c2f41627374726163743e0a0909093c212d2d2046656174757265547970655374796c657320646573637269626520686f7720746f2072656e64657220646966666572656e74206665617475726573202d2d3e0a0909093c212d2d20612066656174757265207479706520666f72206c696e6573202d2d3e0a0a0909093c46656174757265547970655374796c653e0a090909093c212d2d46656174757265547970654e616d653e466561747572653c2f46656174757265547970654e616d652d2d3e0a090909093c52756c653e0a09090909093c4e616d653e52756c6520313c2f4e616d653e0a09090909093c5469746c653e426c7565204c696e653c2f5469746c653e0a09090909093c41627374726163743e4120626c7565206c696e6520776974682061203120706978656c2077696474683c2f41627374726163743e0a0a09090909093c212d2d206c696b65206120706f6c79676f6e73796d626f6c697a6572202d2d3e0a09090909093c4c696e6553796d626f6c697a65723e0a0909090909093c5374726f6b653e0a090909090909093c437373506172616d65746572206e616d653d227374726f6b65223e233030303046463c2f437373506172616d657465723e0a0909090909093c2f5374726f6b653e0a09090909093c2f4c696e6553796d626f6c697a65723e0a090909093c2f52756c653e0a0a0909202020203c2f46656174757265547970655374796c653e0a09093c2f557365725374796c653e0a093c2f4e616d65644c617965723e0a3c2f5374796c65644c6179657244657363726970746f723e0a');
INSERT INTO public.resources VALUES (50, 'config.xml', 49, '2021-06-15 05:10:53.167', '\x3c7365637572697479496e746572636570746f723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666313c2f69643e0a20203c6e616d653e696e746572636570746f723c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f5365727665725365637572697479496e746572636570746f7246696c7465723c2f636c6173734e616d653e0a20203c616c6c6f774966416c6c4162737461696e4465636973696f6e733e66616c73653c2f616c6c6f774966416c6c4162737461696e4465636973696f6e733e0a20203c73656375726974794d65746164617461536f757263653e67656f7365727665724d65746164617461536f757263653c2f73656375726974794d65746164617461536f757263653e0a3c2f7365637572697479496e746572636570746f723e');
INSERT INTO public.resources VALUES (5, 'line.xml', 1, '2021-06-15 05:10:51.032', '\x3c7374796c653e0a20203c69643e5374796c65496e666f496d706c2d31396463393461312d383964642d343630392d396661352d3633623366646132303339313c2f69643e0a20203c6e616d653e6c696e653c2f6e616d653e0a20203c666f726d61743e736c643c2f666f726d61743e0a20203c6c616e677561676556657273696f6e3e0a202020203c76657273696f6e3e312e302e303c2f76657273696f6e3e0a20203c2f6c616e677561676556657273696f6e3e0a20203c66696c656e616d653e64656661756c745f6c696e652e736c643c2f66696c656e616d653e0a3c2f7374796c653e');
INSERT INTO public.resources VALUES (61, 'default', 60, '2021-06-15 05:10:52.534', NULL);
INSERT INTO public.resources VALUES (1, 'styles', 0, '2021-06-15 05:10:51.122', NULL);
INSERT INTO public.resources VALUES (62, 'config.xml', 61, '2021-06-15 05:10:52.534', '\x3c757365726e616d6550617373776f72643e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376665633c2f69643e0a20203c6e616d653e64656661756c743c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e617574682e557365726e616d6550617373776f726441757468656e7469636174696f6e50726f76696465723c2f636c6173734e616d653e0a20203c7573657247726f7570536572766963654e616d653e64656661756c743c2f7573657247726f7570536572766963654e616d653e0a3c2f757365726e616d6550617373776f72643e');
INSERT INTO public.resources VALUES (60, 'auth', 12, '2021-06-15 05:10:52.534', NULL);
INSERT INTO public.resources VALUES (48, 'config.xml', 47, '2021-06-15 05:10:53.181', '\x3c72656d656d6265724d6541757468656e7469636174696f6e3e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666323c2f69643e0a20203c6e616d653e72656d656d6265726d653c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f53657276657252656d656d6265724d6541757468656e7469636174696f6e46696c7465723c2f636c6173734e616d653e0a3c2f72656d656d6265724d6541757468656e7469636174696f6e3e');
INSERT INTO public.resources VALUES (51, 'rest.properties', 12, '2021-06-15 05:10:52.85', '\x232044656661756c74205245535420736563757269747920636f6e66696775726174696f6e2e0a23200a232042792064656661756c74207468697320636f6e66696775726174696f6e206c6f636b7320646f776e20657665727920726573742063616c6c2e2054686520666f6c6c6f77696e6720697320616e206578616d706c65206f662061206d6f72650a23206c617820636f6e66696775726174696f6e20696e2077686963682072656164206f6e6c792028474554292061636365737320697320616c6c6f77656420616e6f6e796d6f75736c793a0a23200a232f2a2a3b4745543d49535f41555448454e544943415445445f414e4f4e594d4f55534c590a232f2a2a3b504f53542c44454c4554452c5055543d41444d494e0a230a232054686520666f6c6c6f77696e6720697320616e206578616d706c65206f66206120636f6e66696775726174696f6e207468617420636f756c6420626520757365642077697468207468652072657374636f6e66696720706c7567696e20696e200a23207768696368206f6e6c7920636f6e66696775726174696f6e20696e206120737065636966696320776f726b737061636520697320726573747269637465643a0a230a232f726573742f776f726b7370616365732f746f70702a3b4745543d41444d494e0a232f726573742f776f726b7370616365732f746f70702f2a2a3b4745543d41444d494e0a232f2a2a3b504f53542c44454c4554452c5055543d41444d494e0a230a23204e657874206c696e6520656e61626c65732061636365737320746f20757365722073656c662d61646d696e206f7065726174696f6e73202873756368206173206368616e67696e67206f776e2070617373776f7264293a0a23200a232f726573742f73656375726974792f73656c662f2a2a3b5055543d524f4c455f41555448454e544943415445440a230a230a2f2a2a3b4745543d41444d494e0a2f2a2a3b504f53542c44454c4554452c5055543d41444d494e0a');
INSERT INTO public.resources VALUES (57, 'config.xml', 56, '2021-06-15 05:10:53.118', '\x3c657863657074696f6e5472616e736c6174696f6e3e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376665653c2f69643e0a20203c6e616d653e657863657074696f6e3c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f536572766572457863657074696f6e5472616e736c6174696f6e46696c7465723c2f636c6173734e616d653e0a3c2f657863657074696f6e5472616e736c6174696f6e3e');
INSERT INTO public.resources VALUES (56, 'exception', 36, '2021-06-15 05:10:53.118', NULL);
INSERT INTO public.resources VALUES (53, 'config.xml', 52, '2021-06-15 05:10:53.194', '\x3c7365637572697479496e746572636570746f723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666303c2f69643e0a20203c6e616d653e72657374496e746572636570746f723c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f5365727665725365637572697479496e746572636570746f7246696c7465723c2f636c6173734e616d653e0a20203c616c6c6f774966416c6c4162737461696e4465636973696f6e733e66616c73653c2f616c6c6f774966416c6c4162737461696e4465636973696f6e733e0a20203c73656375726974794d65746164617461536f757263653e7265737446696c746572446566696e6974696f6e4d61703c2f73656375726974794d65746164617461536f757263653e0a3c2f7365637572697479496e746572636570746f723e');
INSERT INTO public.resources VALUES (6, 'default_polygon.sld', 1, '2021-06-15 05:10:51.044', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d2249534f2d383835392d31223f3e0a3c5374796c65644c6179657244657363726970746f722076657273696f6e3d22312e302e30220a202020207873693a736368656d614c6f636174696f6e3d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64205374796c65644c6179657244657363726970746f722e787364220a20202020786d6c6e733d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64220a20202020786d6c6e733a6f67633d22687474703a2f2f7777772e6f70656e6769732e6e65742f6f6763220a20202020786d6c6e733a786c696e6b3d22687474703a2f2f7777772e77332e6f72672f313939392f786c696e6b220a20202020786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e6365223e0a202020203c212d2d2061206e616d6564206c6179657220697320746865206261736963206275696c64696e6720626c6f636b206f6620616e20736c6420646f63756d656e74202d2d3e0a0a20203c4e616d65644c617965723e0a202020203c4e616d653e44656661756c7420506f6c79676f6e3c2f4e616d653e0a202020203c557365725374796c653e0a20202020202020203c212d2d20746865792068617665206e616d65732c207469746c657320616e6420616273747261637473202d2d3e0a0a2020202020203c5469746c653e4772657920506f6c79676f6e3c2f5469746c653e0a2020202020203c41627374726163743e412073616d706c65207374796c652074686174206a757374207072696e7473206f75742061206772657920696e746572696f722077697468206120626c61636b206f75746c696e653c2f41627374726163743e0a2020202020203c212d2d2046656174757265547970655374796c657320646573637269626520686f7720746f2072656e64657220646966666572656e74206665617475726573202d2d3e0a2020202020203c212d2d20612066656174757265207479706520666f7220706f6c79676f6e73202d2d3e0a0a2020202020203c46656174757265547970655374796c653e0a20202020202020203c212d2d46656174757265547970654e616d653e466561747572653c2f46656174757265547970654e616d652d2d3e0a20202020202020203c52756c653e0a202020202020202020203c4e616d653e52756c6520313c2f4e616d653e0a202020202020202020203c5469746c653e477265792046696c6c20616e6420426c61636b204f75746c696e653c2f5469746c653e0a202020202020202020203c41627374726163743e477265792066696c6c2077697468206120626c61636b206f75746c696e65203120706978656c20696e2077696474683c2f41627374726163743e0a0a202020202020202020203c212d2d206c696b652061206c696e6573796d626f6c697a657220627574207769746820612066696c6c20746f6f202d2d3e0a202020202020202020203c506f6c79676f6e53796d626f6c697a65723e0a2020202020202020202020203c46696c6c3e0a20202020202020202020202020203c437373506172616d65746572206e616d653d2266696c6c223e234141414141413c2f437373506172616d657465723e0a2020202020202020202020203c2f46696c6c3e0a2020202020202020202020203c5374726f6b653e0a20202020202020202020202020203c437373506172616d65746572206e616d653d227374726f6b65223e233030303030303c2f437373506172616d657465723e0a20202020202020202020202020203c437373506172616d65746572206e616d653d227374726f6b652d7769647468223e313c2f437373506172616d657465723e0a2020202020202020202020203c2f5374726f6b653e0a202020202020202020203c2f506f6c79676f6e53796d626f6c697a65723e0a20202020202020203c2f52756c653e0a0a20202020202020203c2f46656174757265547970655374796c653e0a202020203c2f557365725374796c653e0a20203c2f4e616d65644c617965723e0a3c2f5374796c65644c6179657244657363726970746f723e0a');
INSERT INTO public.resources VALUES (7, 'polygon.xml', 1, '2021-06-15 05:10:51.063', '\x3c7374796c653e0a20203c69643e5374796c65496e666f496d706c2d61333832326330662d373533332d343861392d613036372d3561613561666136386539353c2f69643e0a20203c6e616d653e706f6c79676f6e3c2f6e616d653e0a20203c666f726d61743e736c643c2f666f726d61743e0a20203c6c616e677561676556657273696f6e3e0a202020203c76657273696f6e3e312e302e303c2f76657273696f6e3e0a20203c2f6c616e677561676556657273696f6e3e0a20203c66696c656e616d653e64656661756c745f706f6c79676f6e2e736c643c2f66696c656e616d653e0a3c2f7374796c653e');
INSERT INTO public.resources VALUES (11, 'generic.xml', 1, '2021-06-15 05:10:51.122', '\x3c7374796c653e0a20203c69643e5374796c65496e666f496d706c2d63366537373735312d666666642d346136322d396136622d3838353937663132653931323c2f69643e0a20203c6e616d653e67656e657269633c2f6e616d653e0a20203c666f726d61743e736c643c2f666f726d61743e0a20203c6c616e677561676556657273696f6e3e0a202020203c76657273696f6e3e312e302e303c2f76657273696f6e3e0a20203c2f6c616e677561676556657273696f6e3e0a20203c66696c656e616d653e64656661756c745f67656e657269632e736c643c2f66696c656e616d653e0a3c2f7374796c653e');
INSERT INTO public.resources VALUES (8, 'default_raster.sld', 1, '2021-06-15 05:10:51.074', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e0a3c5374796c65644c6179657244657363726970746f7220786d6c6e733d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c642220786d6c6e733a6f67633d22687474703a2f2f7777772e6f70656e6769732e6e65742f6f67632220786d6c6e733a786c696e6b3d22687474703a2f2f7777772e77332e6f72672f313939392f786c696e6b2220786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e636522207873693a736368656d614c6f636174696f6e3d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c640a687474703a2f2f736368656d61732e6f70656e6769732e6e65742f736c642f312e302e302f5374796c65644c6179657244657363726970746f722e787364222076657273696f6e3d22312e302e30223e0a3c557365724c617965723e0a093c4e616d653e7261737465725f6c617965723c2f4e616d653e0a093c557365725374796c653e0a09093c4e616d653e7261737465723c2f4e616d653e0a09093c5469746c653e4f7061717565205261737465723c2f5469746c653e0a09093c41627374726163743e412073616d706c65207374796c6520666f7220726173746572733c2f41627374726163743e0a09093c46656174757265547970655374796c653e0a0920202020202020203c46656174757265547970654e616d653e466561747572653c2f46656174757265547970654e616d653e0a0909093c52756c653e0a090909093c52617374657253796d626f6c697a65723e0a09090909202020203c4f7061636974793e312e303c2f4f7061636974793e0a090909093c2f52617374657253796d626f6c697a65723e0a0909093c2f52756c653e0a09093c2f46656174757265547970655374796c653e0a093c2f557365725374796c653e0a3c2f557365724c617965723e0a3c2f5374796c65644c6179657244657363726970746f723e');
INSERT INTO public.resources VALUES (9, 'raster.xml', 1, '2021-06-15 05:10:51.093', '\x3c7374796c653e0a20203c69643e5374796c65496e666f496d706c2d61383532396137642d313637622d343430372d386563652d6639333036373861353736313c2f69643e0a20203c6e616d653e7261737465723c2f6e616d653e0a20203c666f726d61743e736c643c2f666f726d61743e0a20203c6c616e677561676556657273696f6e3e0a202020203c76657273696f6e3e312e302e303c2f76657273696f6e3e0a20203c2f6c616e677561676556657273696f6e3e0a20203c66696c656e616d653e64656661756c745f7261737465722e736c643c2f66696c656e616d653e0a3c2f7374796c653e');
INSERT INTO public.resources VALUES (10, 'default_generic.sld', 1, '2021-06-15 05:10:51.103', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d2249534f2d383835392d31223f3e0a3c5374796c65644c6179657244657363726970746f722076657273696f6e3d22312e302e3022200a20202020202020202020202020202020202020202020207873693a736368656d614c6f636174696f6e3d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c64205374796c65644c6179657244657363726970746f722e78736422200a2020202020202020202020202020202020202020202020786d6c6e733d22687474703a2f2f7777772e6f70656e6769732e6e65742f736c6422200a2020202020202020202020202020202020202020202020786d6c6e733a6f67633d22687474703a2f2f7777772e6f70656e6769732e6e65742f6f676322200a2020202020202020202020202020202020202020202020786d6c6e733a786c696e6b3d22687474703a2f2f7777772e77332e6f72672f313939392f786c696e6b22200a2020202020202020202020202020202020202020202020786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e6365223e0a20203c4e616d65644c617965723e0a202020203c4e616d653e67656e657269633c2f4e616d653e0a202020203c557365725374796c653e0a2020202020203c5469746c653e47656e657269633c2f5469746c653e0a2020202020203c41627374726163743e47656e65726963207374796c653c2f41627374726163743e0a2020202020203c46656174757265547970655374796c653e0a20202020202020203c52756c653e0a202020202020202020203c4e616d653e7261737465723c2f4e616d653e0a202020202020202020203c5469746c653e4f7061717565205261737465723c2f5469746c653e0a202020202020202020203c6f67633a46696c7465723e0a2020202020202020202020203c6f67633a50726f70657274794973457175616c546f3e0a20202020202020202020202020203c6f67633a46756e6374696f6e206e616d653d226973436f766572616765222f3e0a20202020202020202020202020203c6f67633a4c69746572616c3e747275653c2f6f67633a4c69746572616c3e0a2020202020202020202020203c2f6f67633a50726f70657274794973457175616c546f3e0a202020202020202020203c2f6f67633a46696c7465723e0a202020202020202020203c52617374657253796d626f6c697a65723e0a2020202020202020202020203c4f7061636974793e312e303c2f4f7061636974793e0a202020202020202020203c2f52617374657253796d626f6c697a65723e0a20202020202020203c2f52756c653e0a20202020202020203c52756c653e0a202020202020202020203c4e616d653e506f6c79676f6e3c2f4e616d653e0a202020202020202020203c5469746c653e4772657920506f6c79676f6e3c2f5469746c653e0a202020202020202020203c6f67633a46696c7465723e0a2020202020202020202020203c6f67633a50726f70657274794973457175616c546f3e0a20202020202020202020202020203c6f67633a46756e6374696f6e206e616d653d2264696d656e73696f6e223e0a202020202020202020202020202020203c6f67633a46756e6374696f6e206e616d653d2267656f6d65747279222f3e0a20202020202020202020202020203c2f6f67633a46756e6374696f6e3e0a20202020202020202020202020203c6f67633a4c69746572616c3e323c2f6f67633a4c69746572616c3e0a2020202020202020202020203c2f6f67633a50726f70657274794973457175616c546f3e0a202020202020202020203c2f6f67633a46696c7465723e0a202020202020202020203c506f6c79676f6e53796d626f6c697a65723e0a2020202020202020202020203c46696c6c3e0a20202020202020202020202020203c437373506172616d65746572206e616d653d2266696c6c223e234141414141413c2f437373506172616d657465723e0a2020202020202020202020203c2f46696c6c3e0a2020202020202020202020203c5374726f6b653e0a20202020202020202020202020203c437373506172616d65746572206e616d653d227374726f6b65223e233030303030303c2f437373506172616d657465723e0a20202020202020202020202020203c437373506172616d65746572206e616d653d227374726f6b652d7769647468223e313c2f437373506172616d657465723e0a2020202020202020202020203c2f5374726f6b653e0a202020202020202020203c2f506f6c79676f6e53796d626f6c697a65723e0a20202020202020203c2f52756c653e0a20202020202020203c52756c653e0a202020202020202020203c4e616d653e4c696e653c2f4e616d653e0a202020202020202020203c5469746c653e426c7565204c696e653c2f5469746c653e0a202020202020202020203c6f67633a46696c7465723e0a2020202020202020202020203c6f67633a50726f70657274794973457175616c546f3e0a20202020202020202020202020203c6f67633a46756e6374696f6e206e616d653d2264696d656e73696f6e223e0a202020202020202020202020202020203c6f67633a46756e6374696f6e206e616d653d2267656f6d65747279222f3e0a20202020202020202020202020203c2f6f67633a46756e6374696f6e3e0a20202020202020202020202020203c6f67633a4c69746572616c3e313c2f6f67633a4c69746572616c3e0a2020202020202020202020203c2f6f67633a50726f70657274794973457175616c546f3e0a202020202020202020203c2f6f67633a46696c7465723e0a202020202020202020203c4c696e6553796d626f6c697a65723e0a2020202020202020202020203c5374726f6b653e0a20202020202020202020202020203c437373506172616d65746572206e616d653d227374726f6b65223e233030303046463c2f437373506172616d657465723e0a20202020202020202020202020203c437373506172616d65746572206e616d653d227374726f6b652d6f706163697479223e313c2f437373506172616d657465723e0a2020202020202020202020203c2f5374726f6b653e0a202020202020202020203c2f4c696e6553796d626f6c697a65723e0a20202020202020203c2f52756c653e0a20202020202020203c52756c653e0a202020202020202020203c4e616d653e706f696e743c2f4e616d653e0a202020202020202020203c5469746c653e5265642053717561726520506f696e743c2f5469746c653e0a202020202020202020203c456c736546696c7465722f3e0a202020202020202020203c506f696e7453796d626f6c697a65723e0a2020202020202020202020203c477261706869633e0a20202020202020202020202020203c4d61726b3e0a202020202020202020202020202020203c57656c6c4b6e6f776e4e616d653e7371756172653c2f57656c6c4b6e6f776e4e616d653e0a202020202020202020202020202020203c46696c6c3e0a2020202020202020202020202020202020203c437373506172616d65746572206e616d653d2266696c6c223e234646303030303c2f437373506172616d657465723e0a202020202020202020202020202020203c2f46696c6c3e0a20202020202020202020202020203c2f4d61726b3e0a20202020202020202020202020203c53697a653e363c2f53697a653e0a2020202020202020202020203c2f477261706869633e0a202020202020202020203c2f506f696e7453796d626f6c697a65723e0a20202020202020203c2f52756c653e0a20202020202020203c56656e646f724f7074696f6e206e616d653d2272756c654576616c756174696f6e223e66697273743c2f56656e646f724f7074696f6e3e0a2020202020203c2f46656174757265547970655374796c653e0a202020203c2f557365725374796c653e0a20203c2f4e616d65644c617965723e0a3c2f5374796c65644c6179657244657363726970746f723e0a');
INSERT INTO public.resources VALUES (15, 'config.xml', 14, '2021-06-15 05:10:51.644', '\x3c75726c50726f76696465723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666633c2f69643e0a20203c6e616d653e64656661756c743c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e70617373776f72642e55524c4d617374657250617373776f726450726f76696465723c2f636c6173734e616d653e0a20203c726561644f6e6c793e66616c73653c2f726561644f6e6c793e0a20203c6c6f67696e456e61626c65643e66616c73653c2f6c6f67696e456e61626c65643e0a20203c75726c3e66696c653a7061737377643c2f75726c3e0a20203c656e6372797074696e673e747275653c2f656e6372797074696e673e0a3c2f75726c50726f76696465723e');
INSERT INTO public.resources VALUES (16, 'masterpw.info', 12, '2021-06-15 05:10:51.673', '\x546869732066696c6520776173206372656174656420617420323032312f30362f31352030353a31303a35310a0a5468652067656e657261746564206d61737465722070617373776f72642069733a20777e2247385269210a0a5465737420746865206d61737465722070617373776f7264206279206c6f6767696e6720696e20617320757365722022726f6f74220a0a546869732066696c652073686f756c642062652072656d6f7665642061667465722072656164696e67202121212e0a');
INSERT INTO public.resources VALUES (17, 'passwd', 14, '2021-06-15 05:10:51.717', '\x593455755674596c46677775355547617a357a5930716345574b536967497266');
INSERT INTO public.resources VALUES (14, 'default', 13, '2021-06-15 05:10:51.717', NULL);
INSERT INTO public.resources VALUES (13, 'masterpw', 12, '2021-06-15 05:10:51.717', NULL);
INSERT INTO public.resources VALUES (19, 'services.properties', 12, '2021-06-15 05:10:52.846', '\x232054686520666f726d6174206865726520697320736572766963655b2e6d6574686f645d3d524f4c45312c2e2e2e2c524f4c454e0a2320285b6d6574686f645d206265696e67206f7074696f6e616c20696620796f752077616e7420746f206170706c79207468652072756c6520746f20616c6c2063616c6c7320746f206120737065636966696320736572766963650a23204120757365722063616e2061636365737320612073657276696365206f6e6c7920696620686520686173206f6e65206f66207468652073706563696669656420726f6c65730a23204966206e6f742073706563696669656420696e20746869732066696c652c20612073657276696365206f72206d6574686f642077696c6c20626520636f6e7369646572656420756e736563757265640a0a2320556e636f6d6d656e742074686520666f6c6c6f77696e6720636f6e66696720696620796f752077616e7420746f2074657374207365637572696e672057465320736572766963650a237766732e476574466561747572653d524f4c455f5746535f524541440a237766732e5472616e73616374696f6e3d524f4c455f5746535f57524954450a7766732e44726f7053746f72656451756572793d41444d494e2c47524f55505f41444d494e0a7766732e43726561746553746f72656451756572793d41444d494e2c47524f55505f41444d494e0a');
INSERT INTO public.resources VALUES (18, 'masterpw.xml', 12, '2021-06-15 05:10:51.726', '\x3c6d617374657250617373776f72643e0a20203c70726f76696465724e616d653e64656661756c743c2f70726f76696465724e616d653e0a3c2f6d617374657250617373776f72643e');
INSERT INTO public.resources VALUES (35, 'roles.xml', 32, '2021-06-15 05:10:52.885', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e3c726f6c65526567697374727920786d6c6e733d22687474703a2f2f7777772e67656f7365727665722e6f72672f73656375726974792f726f6c6573222076657273696f6e3d22312e30223e0a3c726f6c654c6973743e0a3c726f6c652069643d2241444d494e222f3e0a3c726f6c652069643d2247524f55505f41444d494e222f3e0a3c2f726f6c654c6973743e0a3c757365724c6973743e0a3c75736572526f6c657320757365726e616d653d2261646d696e223e0a3c726f6c6552656620726f6c6549443d2241444d494e222f3e0a3c2f75736572526f6c65733e0a3c2f757365724c6973743e0a3c67726f75704c6973742f3e0a3c2f726f6c6552656769737472793e0a');
INSERT INTO public.resources VALUES (66, 'roleFilter', 36, '2021-06-15 05:10:53.208', NULL);
INSERT INTO public.resources VALUES (68, 'sslFilter', 36, '2021-06-15 05:10:53.217', NULL);
INSERT INTO public.resources VALUES (67, 'config.xml', 66, '2021-06-15 05:10:53.208', '\x3c726f6c6546696c7465723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376665623c2f69643e0a20203c6e616d653e726f6c6546696c7465723c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f536572766572526f6c6546696c7465723c2f636c6173734e616d653e0a20203c68747470526573706f6e736548656164657241747472466f72496e636c75646564526f6c65733e726f6c65733c2f68747470526573706f6e736548656164657241747472466f72496e636c75646564526f6c65733e0a20203c726f6c65436f6e7665727465724e616d653e726f6c65436f6e7665727465723c2f726f6c65436f6e7665727465724e616d653e0a3c2f726f6c6546696c7465723e');
INSERT INTO public.resources VALUES (12, 'security', 0, '2021-06-15 05:12:14.37', NULL);
INSERT INTO public.resources VALUES (69, 'config.xml', 68, '2021-06-15 05:10:53.217', '\x3c73736c46696c7465723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376665613c2f69643e0a20203c6e616d653e73736c46696c7465723c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f53657276657253534c46696c7465723c2f636c6173734e616d653e0a20203c73736c506f72743e3434333c2f73736c506f72743e0a3c2f73736c46696c7465723e');
INSERT INTO public.resources VALUES (20, 'geoserver.jceks', 12, '2021-06-15 05:10:51.964', '\xcececece000000020000000200000003000e75673a64656661756c743a6b65790000017a0e1456dcaced000573720033636f6d2e73756e2e63727970746f2e70726f76696465722e5365616c65644f626a656374466f724b657950726f746563746f72cd57ca59e730bb53020000787200196a617661782e63727970746f2e5365616c65644f626a6563743e363da6c3b754700200045b000d656e636f646564506172616d737400025b425b0010656e63727970746564436f6e74656e7471007e00024c0009706172616d73416c677400124c6a6176612f6c616e672f537472696e673b4c00077365616c416c6771007e00037870757200025b42acf317f8060854e0020000787000000011300f04088e31d020a3e68db40203030d407571007e0005000000a0dbd9dfe481e8dadc4993d7682bd133fb650c9f94869c159426f4db2ad24edf3ff1a59dc356f987ce5f30f14add1b31c8a3c7da6642425babb1f8efe5046b3c8c3508436c6648f3d3394a6dde27ebfa02c1d4e003560af7740e5cd2e8c7b1be0065004eb11626abea0c8e6cce0d143480297c327806ea634234b6dd26f0e53536ae494d0178703a7cff8e041e13c2df466e8b580fe7d7f918262e08e502452940740016504245576974684d4435416e64547269706c65444553740016504245576974684d4435416e64547269706c65444553000000030013636f6e6669673a70617373776f72643a6b65790000017a0e145667aced000573720033636f6d2e73756e2e63727970746f2e70726f76696465722e5365616c65644f626a656374466f724b657950726f746563746f72cd57ca59e730bb53020000787200196a617661782e63727970746f2e5365616c65644f626a6563743e363da6c3b754700200045b000d656e636f646564506172616d737400025b425b0010656e63727970746564436f6e74656e7471007e00024c0009706172616d73416c677400124c6a6176612f6c616e672f537472696e673b4c00077365616c416c6771007e00037870757200025b42acf317f8060854e0020000787000000011300f04084fae6cc4148a99920203030d407571007e0005000000a8d71c2a7436f1b8678cb8664259296bc1875c7d05d95395aab4a50cb38c16401439c99c454f670f4a33d59447da4ef897f034ddb1c1c79cd12c747ca7f843a78dc8b7fe612a657dcc6e6a994298339f265085e5301db2d4dbabb19ba293ee7d32930b486438e6f66dcc09e84268ea545e947e6d890195833dca154630b160afe19ea2060ee762a7dad08bbc164a1e3c3adb0e45aaf6a720b29f758a5dafd920a147c2933fb6e678e4740016504245576974684d4435416e64547269706c65444553740016504245576974684d4435416e64547269706c65444553139f9a689adc9acd75c736ca87142d9f9f02bf44');
INSERT INTO public.resources VALUES (23, 'config.xml', 22, '2021-06-15 05:10:51.974', '\x3c70617373776f7264506f6c6963793e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666623c2f69643e0a20203c6e616d653e64656661756c743c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e76616c69646174696f6e2e50617373776f726456616c696461746f72496d706c3c2f636c6173734e616d653e0a20203c75707065726361736552657175697265643e66616c73653c2f75707065726361736552657175697265643e0a20203c6c6f7765726361736552657175697265643e66616c73653c2f6c6f7765726361736552657175697265643e0a20203c646967697452657175697265643e66616c73653c2f646967697452657175697265643e0a20203c6d696e4c656e6774683e303c2f6d696e4c656e6774683e0a20203c6d61784c656e6774683e2d313c2f6d61784c656e6774683e0a3c2f70617373776f7264506f6c6963793e');
INSERT INTO public.resources VALUES (22, 'default', 21, '2021-06-15 05:10:51.974', NULL);
INSERT INTO public.resources VALUES (25, 'config.xml', 24, '2021-06-15 05:10:51.987', '\x3c70617373776f7264506f6c6963793e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666613c2f69643e0a20203c6e616d653e6d61737465723c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e76616c69646174696f6e2e50617373776f726456616c696461746f72496d706c3c2f636c6173734e616d653e0a20203c75707065726361736552657175697265643e66616c73653c2f75707065726361736552657175697265643e0a20203c6c6f7765726361736552657175697265643e66616c73653c2f6c6f7765726361736552657175697265643e0a20203c646967697452657175697265643e66616c73653c2f646967697452657175697265643e0a20203c6d696e4c656e6774683e383c2f6d696e4c656e6774683e0a20203c6d61784c656e6774683e2d313c2f6d61784c656e6774683e0a3c2f70617373776f7264506f6c6963793e');
INSERT INTO public.resources VALUES (24, 'master', 21, '2021-06-15 05:10:51.987', NULL);
INSERT INTO public.resources VALUES (21, 'pwpolicy', 12, '2021-06-15 05:10:51.987', NULL);
INSERT INTO public.resources VALUES (28, 'config.xml', 27, '2021-06-15 05:10:52.197', '\x3c7573657247726f7570536572766963653e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666393c2f69643e0a20203c6e616d653e64656661756c743c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e786d6c2e584d4c5573657247726f7570536572766963653c2f636c6173734e616d653e0a20203c66696c654e616d653e75736572732e786d6c3c2f66696c654e616d653e0a20203c636865636b496e74657276616c3e31303030303c2f636865636b496e74657276616c3e0a20203c76616c69646174696e673e747275653c2f76616c69646174696e673e0a20203c70617373776f7264456e636f6465724e616d653e70626550617373776f7264456e636f6465723c2f70617373776f7264456e636f6465724e616d653e0a20203c70617373776f7264506f6c6963794e616d653e64656661756c743c2f70617373776f7264506f6c6963794e616d653e0a3c2f7573657247726f7570536572766963653e');
INSERT INTO public.resources VALUES (29, 'users.xsd', 27, '2021-06-15 05:10:52.212', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e0a3c736368656d6120786d6c6e733d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d6122207461726765744e616d6573706163653d22687474703a2f2f7777772e67656f7365727665722e6f72672f73656375726974792f75736572732220786d6c6e733a6773753d22687474703a2f2f7777772e67656f7365727665722e6f72672f73656375726974792f75736572732220656c656d656e74466f726d44656661756c743d227175616c6966696564223e0a0a0a202020203c656c656d656e74206e616d653d227573657252656769737472792220747970653d226773753a55736572526567697374727954797065223e0a20202020202020203c6b6579206e616d653d22557365724b6579223e0a2020202009093c73656c6563746f722078706174683d226773753a75736572732f6773753a75736572222f3e0a2020202009093c6669656c642078706174683d22406e616d65222f3e0a20202020093c2f6b65793e0a20202020093c6b6579206e616d653d2247726f75704b6579223e0a2020202020202020093c73656c6563746f722078706174683d226773753a67726f7570732f6773753a67726f7570222f3e0a2020202020202020093c6669656c642078706174683d22406e616d65222f3e0a09093c2f6b65793e0a09093c6b6579726566206e616d653d22466f7265696e557365724b6579222072656665723d226773753a557365724b6579223e20200a20200909093c73656c6563746f722078706174683d226773753a67726f7570732f6773753a67726f75702f6773753a6d656d626572222f3e0a20200909093c6669656c642078706174683d2240757365726e616d65222f3e0a09093c2f6b65797265663e20202020090909092020202009092020202009092020202009202020200909202020200a202020203c2f656c656d656e743e0a202020200a202020203c636f6d706c657854797065206e616d653d225573657254797065223e0a20202020202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d2270726f70657274792220747970653d226773753a5573657250726f70657274795479706522206d696e4f63637572733d223022206d61784f63637572733d22756e626f756e646564222f3e200a20202020093c2f73657175656e63653e20202020090a202020200a20202020093c617474726962757465206e616d653d226e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a20202020093c617474726962757465206e616d653d2270617373776f72642220747970653d22737472696e6722207573653d226f7074696f6e616c223e3c2f6174747269627574653e0a20202020093c617474726962757465206e616d653d22656e61626c65642220747970653d22626f6f6c65616e22207573653d226f7074696f6e616c222064656661756c743d2274727565223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d2247726f757054797065223e0a20202020093c73657175656e63653e2020202009090a2020202009093c212d2d3c656c656d656e74206e616d653d226d656d6265722220747970653d226773753a557365725265665479706522206d696e4f63637572733d223022206d61784f63637572733d22756e626f756e646564223e3c2f656c656d656e743e202d2d3e0a2020202009093c656c656d656e74206e616d653d226d656d6265722220747970653d226773753a557365725265665479706522206d696e4f63637572733d223022206d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e202020200a20202020093c617474726962757465206e616d653d226e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a20202020093c617474726962757465206e616d653d22656e61626c65642220747970653d22626f6f6c65616e22207573653d226f7074696f6e616c222064656661756c743d2274727565223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020200a202020203c636f6d706c657854797065206e616d653d225573657252656654797065223e0a20202020093c617474726962757465206e616d653d22757365726e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020200a0a202020203c636f6d706c657854797065206e616d653d2255736572526567697374727954797065223e0a20202020093c73657175656e63653e2020202009090a2020202009093c656c656d656e74206e616d653d2275736572732220747970653d226773753a55736572735479706522206d696e4f63637572733d223122206d61784f63637572733d223122202f3e0a2020202009093c656c656d656e74206e616d653d2267726f7570732220747970653d226773753a47726f7570735479706522206d696e4f63637572733d223122206d61784f63637572733d2231222f3e0a20202020093c2f73657175656e63653e0a20202020093c61747472696275746520206e616d653d2276657273696f6e2220747970653d226773753a56657273696f6e5479706522207573653d22726571756972656422203e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d22557365727354797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d22757365722220747970653d226773753a557365725479706522206d696e4f63637572733d2230220a202020200909096d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d2247726f75707354797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d2267726f75702220747970653d226773753a47726f75705479706522206d696e4f63637572733d2230220a202020200909096d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e20202020090a202020203c2f636f6d706c6578547970653e0a202020200a20202020202020202020202020202020202020200a202020203c636f6d706c657854797065206e616d653d225573657250726f706572747954797065223e0a20202020093c73696d706c65436f6e74656e743e0a2020202009093c657874656e73696f6e20626173653d22737472696e67223e0a202020200909093c617474726962757465206e616d653d226e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a2020202009093c2f657874656e73696f6e3e0a20202020093c2f73696d706c65436f6e74656e743e0a202020203c2f636f6d706c6578547970653e0a0a202020203c73696d706c6554797065206e616d653d2256657273696f6e5479706522203e0a20202020093c7265737472696374696f6e20626173653d22737472696e67223e0a2020202009093c656e756d65726174696f6e2076616c75653d22312e30223e3c2f656e756d65726174696f6e3e0a20202020093c2f7265737472696374696f6e3e0a202020203c2f73696d706c65547970653e0a3c2f736368656d613e');
INSERT INTO public.resources VALUES (37, 'basic', 36, '2021-06-15 05:10:53.07', NULL);
INSERT INTO public.resources VALUES (44, 'config.xml', 43, '2021-06-15 05:10:53.104', '\x3c636f6e7465787450657273697374656e63653e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666343c2f69643e0a20203c6e616d653e636f6e746578744e6f4173633c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f5365727665725365637572697479436f6e7465787450657273697374656e636546696c7465723c2f636c6173734e616d653e0a20203c616c6c6f7753657373696f6e4372656174696f6e3e66616c73653c2f616c6c6f7753657373696f6e4372656174696f6e3e0a3c2f636f6e7465787450657273697374656e63653e');
INSERT INTO public.resources VALUES (27, 'default', 26, '2021-06-15 05:10:52.916', NULL);
INSERT INTO public.resources VALUES (38, 'config.xml', 37, '2021-06-15 05:10:53.07', '\x3c626173696341757468656e7469636174696f6e3e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666373c2f69643e0a20203c6e616d653e62617369633c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f536572766572426173696341757468656e7469636174696f6e46696c7465723c2f636c6173734e616d653e0a20203c75736552656d656d6265724d653e747275653c2f75736552656d656d6265724d653e0a3c2f626173696341757468656e7469636174696f6e3e');
INSERT INTO public.resources VALUES (31, 'role', 12, '2021-06-15 05:10:52.885', NULL);
INSERT INTO public.resources VALUES (33, 'config.xml', 32, '2021-06-15 05:10:52.348', '\x3c726f6c65536572766963653e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666383c2f69643e0a20203c6e616d653e64656661756c743c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e786d6c2e584d4c526f6c65536572766963653c2f636c6173734e616d653e0a20203c66696c654e616d653e726f6c65732e786d6c3c2f66696c654e616d653e0a20203c636865636b496e74657276616c3e31303030303c2f636865636b496e74657276616c3e0a20203c76616c69646174696e673e747275653c2f76616c69646174696e673e0a20203c61646d696e526f6c654e616d653e41444d494e3c2f61646d696e526f6c654e616d653e0a20203c67726f757041646d696e526f6c654e616d653e47524f55505f41444d494e3c2f67726f757041646d696e526f6c654e616d653e0a3c2f726f6c65536572766963653e');
INSERT INTO public.resources VALUES (47, 'rememberme', 36, '2021-06-15 05:10:53.181', NULL);
INSERT INTO public.resources VALUES (34, 'roles.xsd', 32, '2021-06-15 05:10:52.36', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e0a3c736368656d6120786d6c6e733d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d6122207461726765744e616d6573706163653d22687474703a2f2f7777772e67656f7365727665722e6f72672f73656375726974792f726f6c65732220786d6c6e733a6773723d22687474703a2f2f7777772e67656f7365727665722e6f72672f73656375726974792f726f6c65732220656c656d656e74466f726d44656661756c743d227175616c6966696564223e0a0a0a202020203c656c656d656e74206e616d653d22726f6c6552656769737472792220747970653d226773723a526f6c65526567697374727954797065223e0a20202020202020203c6b6579206e616d653d22526f6c654b6579223e0a2020202009093c73656c6563746f722078706174683d226773723a726f6c654c6973742f6773723a726f6c65222f3e0a2020202009093c6669656c642078706174683d22406964222f3e0a20202020093c2f6b65793e0a20202020093c6b6579726566206e616d653d22506172656e744b6579222072656665723d226773723a526f6c654b6579223e0a2020202009093c73656c6563746f722078706174683d226773723a726f6c654c6973742f6773723a726f6c65222f3e0a2020202009093c6669656c642078706174683d2240706172656e744944222f3e0a20202020093c2f6b65797265663e0a20202020093c6b6579726566206e616d653d2255736572526f6c654b6579222072656665723d226773723a526f6c654b6579223e0a2020202009093c73656c6563746f722078706174683d226773723a757365724c6973742f6773723a75736572526f6c65732f6773723a726f6c65526566222f3e0a2020202009093c6669656c642078706174683d2240726f6c654944222f3e0a20202020093c2f6b65797265663e202020202020202009202020200a20202020093c6b6579726566206e616d653d2247726f7570526f6c654b6579222072656665723d226773723a526f6c654b6579223e0a2020202009093c73656c6563746f722078706174683d226773723a67726f75704c6973742f6773723a67726f7570526f6c65732f6773723a726f6c65526566222f3e0a2020202009093c6669656c642078706174683d2240726f6c654944222f3e0a20202020093c2f6b65797265663e202020202020202009202020202020202009202020202020202009202020200a202020203c2f656c656d656e743e0a202020200a202020203c636f6d706c657854797065206e616d653d22526f6c6554797065223e0a20202020202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d2270726f70657274792220747970653d226773723a526f6c6550726f70657274795479706522206d696e4f63637572733d223022206d61784f63637572733d22756e626f756e646564222f3e200a20202020093c2f73657175656e63653e2020202009202020200a20202020093c617474726962757465206e616d653d2269642220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a20202020093c617474726962757465206e616d653d22706172656e7449442220747970653d22737472696e6722207573653d226f7074696f6e616c223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d22526f6c6552656654797065223e0a20202020093c617474726962757465206e616d653d22726f6c6549442220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020200a0a202020203c636f6d706c657854797065206e616d653d22526f6c65526567697374727954797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d22726f6c654c6973742220747970653d226773723a526f6c654c6973745479706522206d696e4f63637572733d223122206d61784f63637572733d2231223e3c2f656c656d656e743e0a2020202009093c656c656d656e74206e616d653d22757365724c6973742220747970653d226773723a55736572526f6c65735479706522206d696e4f63637572733d223122206d61784f63637572733d2231223e3c2f656c656d656e743e0a2020202009093c656c656d656e74206e616d653d2267726f75704c6973742220747970653d226773723a47726f7570526f6c65735479706522206d696e4f63637572733d223122206d61784f63637572733d2231223e3c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a20202020093c617474726962757465206e616d653d2276657273696f6e2220747970653d226773723a56657273696f6e5479706522207573653d22726571756972656422203e3c2f6174747269627574653e20202020090a202020203c2f636f6d706c6578547970653e0a0a202020203c636f6d706c657854797065206e616d653d22526f6c654c69737454797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d22726f6c652220747970653d226773723a526f6c655479706522206d696e4f63637572733d223022206d61784f63637572733d22756e626f756e646564223e3c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d2255736572526f6c655265664c69737454797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d22726f6c655265662220747970653d226773723a526f6c655265665479706522206d696e4f63637572733d2230220a202020200909096d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a20202020093c617474726962757465206e616d653d22757365726e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d2247726f7570526f6c655265664c69737454797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d22726f6c655265662220747970653d226773723a526f6c655265665479706522206d696e4f63637572733d2230220a202020200909096d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a20202020093c617474726962757465206e616d653d2267726f75706e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d2247726f7570526f6c657354797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d2267726f7570526f6c65732220747970653d226773723a47726f7570526f6c655265664c6973745479706522206d696e4f63637572733d2230220a202020200909096d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d2255736572526f6c657354797065223e0a20202020093c73657175656e63653e0a2020202009093c656c656d656e74206e616d653d2275736572526f6c65732220747970653d226773723a55736572526f6c655265664c6973745479706522206d696e4f63637572733d2230220a202020200909096d61784f63637572733d22756e626f756e646564223e0a2020202009093c2f656c656d656e743e0a20202020093c2f73657175656e63653e0a202020203c2f636f6d706c6578547970653e0a202020200a202020203c636f6d706c657854797065206e616d653d22526f6c6550726f706572747954797065223e0a20202020093c73696d706c65436f6e74656e743e0a2020202009093c657874656e73696f6e20626173653d22737472696e67223e0a202020200909093c617474726962757465206e616d653d226e616d652220747970653d22737472696e6722207573653d227265717569726564223e3c2f6174747269627574653e0a2020202009093c2f657874656e73696f6e3e0a20202020093c2f73696d706c65436f6e74656e743e0a202020203c2f636f6d706c6578547970653e0a0a202020203c73696d706c6554797065206e616d653d2256657273696f6e5479706522203e0a20202020093c7265737472696374696f6e20626173653d22737472696e67223e0a2020202009093c656e756d65726174696f6e2076616c75653d22312e30223e3c2f656e756d65726174696f6e3e0a20202020093c2f7265737472696374696f6e3e0a202020203c2f73696d706c65547970653e0a202020200a2020202020202020202020200a3c2f736368656d613e');
INSERT INTO public.resources VALUES (39, 'form', 36, '2021-06-15 05:10:53.14', NULL);
INSERT INTO public.resources VALUES (32, 'default', 31, '2021-06-15 05:10:52.885', NULL);
INSERT INTO public.resources VALUES (30, 'users.xml', 27, '2021-06-15 05:10:52.916', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e3c75736572526567697374727920786d6c6e733d22687474703a2f2f7777772e67656f7365727665722e6f72672f73656375726974792f7573657273222076657273696f6e3d22312e30223e0a3c75736572733e0a3c7573657220656e61626c65643d227472756522206e616d653d2261646d696e222070617373776f72643d226372797074313a4f6e2b594c35347861517739734c7450683158713833637a5965574162532b76222f3e0a3c2f75736572733e0a3c67726f7570732f3e0a3c2f7573657252656769737472793e0a');
INSERT INTO public.resources VALUES (26, 'usergroup', 12, '2021-06-15 05:10:52.916', NULL);
INSERT INTO public.resources VALUES (36, 'filter', 12, '2021-06-15 05:10:53.217', NULL);
INSERT INTO public.resources VALUES (41, 'contextAsc', 36, '2021-06-15 05:10:53.085', NULL);
INSERT INTO public.resources VALUES (43, 'contextNoAsc', 36, '2021-06-15 05:10:53.104', NULL);
INSERT INTO public.resources VALUES (40, 'config.xml', 39, '2021-06-15 05:10:53.14', '\x3c757365726e616d6550617373776f726446696c7465723e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666363c2f69643e0a20203c6e616d653e666f726d3c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f536572766572557365724e616d6550617373776f726441757468656e7469636174696f6e46696c7465723c2f636c6173734e616d653e0a20203c70617373776f7264506172616d657465724e616d653e70617373776f72643c2f70617373776f7264506172616d657465724e616d653e0a20203c757365726e616d65506172616d657465724e616d653e757365726e616d653c2f757365726e616d65506172616d657465724e616d653e0a3c2f757365726e616d6550617373776f726446696c7465723e');
INSERT INTO public.resources VALUES (45, 'anonymous', 36, '2021-06-15 05:10:53.055', NULL);
INSERT INTO public.resources VALUES (42, 'config.xml', 41, '2021-06-15 05:10:53.085', '\x3c636f6e7465787450657273697374656e63653e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666353c2f69643e0a20203c6e616d653e636f6e746578744173633c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f5365727665725365637572697479436f6e7465787450657273697374656e636546696c7465723c2f636c6173734e616d653e0a20203c616c6c6f7753657373696f6e4372656174696f6e3e747275653c2f616c6c6f7753657373696f6e4372656174696f6e3e0a3c2f636f6e7465787450657273697374656e63653e');
INSERT INTO public.resources VALUES (46, 'config.xml', 45, '2021-06-15 05:10:53.055', '\x3c616e6f6e796d6f757341757468656e7469636174696f6e3e0a20203c69643e2d31656334353566353a31376130653134353436303a2d376666333c2f69643e0a20203c6e616d653e616e6f6e796d6f75733c2f6e616d653e0a20203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e66696c7465722e47656f536572766572416e6f6e796d6f757341757468656e7469636174696f6e46696c7465723c2f636c6173734e616d653e0a3c2f616e6f6e796d6f757341757468656e7469636174696f6e3e');
INSERT INTO public.resources VALUES (63, 'config.xml', 12, '2021-06-15 05:10:53.041', '\x3c73656375726974793e0a20203c726f6c65536572766963654e616d653e64656661756c743c2f726f6c65536572766963654e616d653e0a20203c6175746850726f76696465724e616d65733e0a202020203c737472696e673e64656661756c743c2f737472696e673e0a20203c2f6175746850726f76696465724e616d65733e0a20203c636f6e66696750617373776f7264456e637279707465724e616d653e70626550617373776f7264456e636f6465723c2f636f6e66696750617373776f7264456e637279707465724e616d653e0a20203c656e6372797074696e6755726c506172616d733e66616c73653c2f656e6372797074696e6755726c506172616d733e0a20203c66696c746572436861696e3e0a202020203c66696c74657273206e616d653d227765622220636c6173733d226f72672e67656f7365727665722e73656375726974792e48746d6c4c6f67696e46696c746572436861696e2220696e746572636570746f724e616d653d22696e746572636570746f722220657863657074696f6e5472616e736c6174696f6e4e616d653d22657863657074696f6e2220706174683d222f7765622f2a2a2c2f6777632f726573742f7765622f2a2a2c2f222064697361626c65643d2266616c73652220616c6c6f7753657373696f6e4372656174696f6e3d2274727565222073736c3d2266616c736522206d61746368485454504d6574686f643d2266616c7365223e0a2020202020203c66696c7465723e72656d656d6265726d653c2f66696c7465723e0a2020202020203c66696c7465723e666f726d3c2f66696c7465723e0a2020202020203c66696c7465723e616e6f6e796d6f75733c2f66696c7465723e0a202020203c2f66696c746572733e0a202020203c66696c74657273206e616d653d227765624c6f67696e2220636c6173733d226f72672e67656f7365727665722e73656375726974792e436f6e7374616e7446696c746572436861696e2220706174683d222f6a5f737072696e675f73656375726974795f636865636b2c2f6a5f737072696e675f73656375726974795f636865636b2f2c2f6c6f67696e222064697361626c65643d2266616c73652220616c6c6f7753657373696f6e4372656174696f6e3d2274727565222073736c3d2266616c736522206d61746368485454504d6574686f643d2266616c7365223e0a2020202020203c66696c7465723e666f726d3c2f66696c7465723e0a202020203c2f66696c746572733e0a202020203c66696c74657273206e616d653d227765624c6f676f75742220636c6173733d226f72672e67656f7365727665722e73656375726974792e4c6f676f757446696c746572436861696e2220706174683d222f6a5f737072696e675f73656375726974795f6c6f676f75742c2f6a5f737072696e675f73656375726974795f6c6f676f75742f2c2f6c6f676f7574222064697361626c65643d2266616c73652220616c6c6f7753657373696f6e4372656174696f6e3d2266616c7365222073736c3d2266616c736522206d61746368485454504d6574686f643d2266616c7365223e0a2020202020203c66696c7465723e666f726d4c6f676f75743c2f66696c7465723e0a202020203c2f66696c746572733e0a202020203c66696c74657273206e616d653d22726573742220636c6173733d226f72672e67656f7365727665722e73656375726974792e536572766963654c6f67696e46696c746572436861696e2220696e746572636570746f724e616d653d2272657374496e746572636570746f722220657863657074696f6e5472616e736c6174696f6e4e616d653d22657863657074696f6e2220706174683d222f726573742f2a2a222064697361626c65643d2266616c73652220616c6c6f7753657373696f6e4372656174696f6e3d2266616c7365222073736c3d2266616c736522206d61746368485454504d6574686f643d2266616c7365223e0a2020202020203c66696c7465723e62617369633c2f66696c7465723e0a2020202020203c66696c7465723e616e6f6e796d6f75733c2f66696c7465723e0a202020203c2f66696c746572733e0a202020203c66696c74657273206e616d653d226777632220636c6173733d226f72672e67656f7365727665722e73656375726974792e536572766963654c6f67696e46696c746572436861696e2220696e746572636570746f724e616d653d2272657374496e746572636570746f722220657863657074696f6e5472616e736c6174696f6e4e616d653d22657863657074696f6e2220706174683d222f6777632f726573742f2a2a222064697361626c65643d2266616c73652220616c6c6f7753657373696f6e4372656174696f6e3d2266616c7365222073736c3d2266616c736522206d61746368485454504d6574686f643d2266616c7365223e0a2020202020203c66696c7465723e62617369633c2f66696c7465723e0a202020203c2f66696c746572733e0a202020203c66696c74657273206e616d653d2264656661756c742220636c6173733d226f72672e67656f7365727665722e73656375726974792e536572766963654c6f67696e46696c746572436861696e2220696e746572636570746f724e616d653d22696e746572636570746f722220657863657074696f6e5472616e736c6174696f6e4e616d653d22657863657074696f6e2220706174683d222f2a2a222064697361626c65643d2266616c73652220616c6c6f7753657373696f6e4372656174696f6e3d2266616c7365222073736c3d2266616c736522206d61746368485454504d6574686f643d2266616c7365223e0a2020202020203c66696c7465723e62617369633c2f66696c7465723e0a2020202020203c66696c7465723e616e6f6e796d6f75733c2f66696c7465723e0a202020203c2f66696c746572733e0a20203c2f66696c746572436861696e3e0a20203c72656d656d6265724d65536572766963653e0a202020203c636c6173734e616d653e6f72672e67656f7365727665722e73656375726974792e72656d656d6265726d652e47656f536572766572546f6b656e426173656452656d656d6265724d6553657276696365733c2f636c6173734e616d653e0a202020203c6b65793e67656f7365727665723c2f6b65793e0a20203c2f72656d656d6265724d65536572766963653e0a20203c6272757465466f72636550726576656e74696f6e3e0a202020203c656e61626c65643e747275653c2f656e61626c65643e0a202020203c6d696e44656c61795365636f6e64733e313c2f6d696e44656c61795365636f6e64733e0a202020203c6d617844656c61795365636f6e64733e353c2f6d617844656c61795365636f6e64733e0a202020203c6d6178426c6f636b6564546872656164733e3130303c2f6d6178426c6f636b6564546872656164733e0a202020203c77686974656c69737465644d61736b733e0a2020202020203c737472696e673e3132372e302e302e313c2f737472696e673e0a202020203c2f77686974656c69737465644d61736b733e0a20203c2f6272757465466f72636550726576656e74696f6e3e0a3c2f73656375726974793e');
INSERT INTO public.resources VALUES (70, 'version.properties', 12, '2021-06-15 05:10:53.235', '\x2343757272656e742076657273696f6e206f6620746865207365637572697479206469726563746f72792e20446f206e6f742072656d6f7665206f7220616c74657220746869732066696c650a23547565204a756e2031352030353a31303a353320474d5420323032310a76657273696f6e3d322e350a');
INSERT INTO public.resources VALUES (71, 'masterpw.digest', 12, '2021-06-15 05:11:41.45', '\x646967657374313a32553467685031713867434f645348764a6b56314d726e684463354b4943734d69636f545279474a68695a774b6549374e30534b46514a4b655a325048514442');
INSERT INTO public.resources VALUES (72, 'layers.properties', 12, '2021-06-15 05:12:14.37', '\x232072756c6520737472756374757265206973206e616d6573706163652e6c617965722e6f7065726174696f6e3d726f6c65312c726f6c65322c2e2e2e0a23206e616d6573706163653a2061206e616d657370616365206f72202a20746f206361746368207468656d20616c6c2028696e207468617420636173652c206c617965722073686f756c64206265202a290a23206c617965723a2061206c617965722f66656174757265547970652f636f766572616765206e616d65206f72202a20746f206361746368207468656d20616c6c0a23206f7065726174696f6e3a2072206f722077202872656164206f72207772697465290a2320726f6c65206c6973743a202a20746f20696d706c7920616c6c20726f6c65732c206f722061206c697374206f66206578706c6963697420726f6c65730a2320546865206f7065726174696f6e2077696c6c20626520616c6c6f776564206966207468652063757272656e74207573657220686173206174206c65617374206f6e65206f66207468650a2320726f6c657320696e207468652072756c650a2a2e2a2e723d2a0a2a2e2a2e773d41444d494e2c47524f55505f41444d494e');


ALTER TABLE public.resources ENABLE TRIGGER ALL;

--
-- TOC entry 4093 (class 0 OID 18459)
-- Dependencies: 232
-- Data for Name: spatial_ref_sys; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.spatial_ref_sys DISABLE TRIGGER ALL;



ALTER TABLE public.spatial_ref_sys ENABLE TRIGGER ALL;

--
-- TOC entry 4305 (class 0 OID 0)
-- Dependencies: 207
-- Name: object_oid_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.object_oid_seq', 11, true);


--
-- TOC entry 4306 (class 0 OID 0)
-- Dependencies: 212
-- Name: property_type_oid_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.property_type_oid_seq', 285, true);


--
-- TOC entry 4307 (class 0 OID 0)
-- Dependencies: 292
-- Name: resources_oid_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.resources_oid_seq', 72, true);


--
-- TOC entry 4308 (class 0 OID 0)
-- Dependencies: 210
-- Name: type_oid_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.type_oid_seq', 21, true);


-- Completed on 2021-06-15 05:14:28 UTC

--
-- PostgreSQL database dump complete
--

