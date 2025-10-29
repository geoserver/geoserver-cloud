import json

WORKSPACE = "test_cascade"
WMS_STORE = "test_cascaded_wms_store"
WMS_URL = "https://wms.geo.admin.ch/?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities"
WMS_LAYER = "ch.swisstopo.swissboundaries3d-gemeinde-flaeche.fill"
WMTS_STORE = "test_cascaded_wmts_store"
WMTS_URL = "https://wmts.geo.admin.ch/EPSG/4326/1.0.0/WMTSCapabilities.xml"
WMTS_LAYER = "ch.swisstopo.pixelkarte-grau"


def test_cascaded_wms(geoserver_factory):
    geoserver = geoserver_factory(WORKSPACE)
    format = "image/jpeg"

    # Create WMS store
    content, status = geoserver.create_wms_store(
        workspace_name=WORKSPACE,
        wms_store_name=WMS_STORE,
        capabilities_url=WMS_URL,
    )
    assert content == WMS_STORE
    assert status == 201

    # Publish layer
    content, status = geoserver.create_wms_layer(
        workspace_name=WORKSPACE,
        wms_store_name=WMS_STORE,
        native_layer_name=WMS_LAYER,
    )
    assert content == WMS_LAYER
    assert status == 201

    # Perform GetMap request
    response = geoserver.get_map(
        layers=[WMS_LAYER],
        bbox=(2590000, 1196000, 2605000, 1203000),
        size=(10, 10),
        format=format,
    )
    assert response.info().get("Content-Type") == format

    # Perform GetFeatureInfo request
    response = geoserver.get_feature_info(
        layers=[WMS_LAYER],
        bbox=(2599999.5, 1199999.5, 2600000.5, 1200000.5),
        size=(40, 40),
        info_format="application/json",
        xy=(20, 20),
    )

    # Due to conflicting formats, the forwarding of GetFeatureInfo requests from map.geo.admin (MapServer)
    # through GeoServer is not possible as of 2.25.0.
    # See https://sourceforge.net/p/geoserver/mailman/message/30757977/
    data = json.loads(response.read().decode("utf-8"))
    assert data.get("features") == []

    # Delete store
    content, status = geoserver.delete_wms_store(
        workspace_name=WORKSPACE, wms_store_name=WMS_STORE
    )
    assert content == ""
    assert status == 200


def test_cascaded_wmts(geoserver_factory):
    geoserver = geoserver_factory(WORKSPACE)
    format = "image/jpeg"

    # Create WMTS store
    content, status = geoserver.create_wmts_store(
        workspace_name=WORKSPACE,
        name=WMTS_STORE,
        capabilities=WMTS_URL,
    )
    assert content == WMTS_STORE
    assert status == 201

    # Publish layer (GeoServer)
    content, status = geoserver.create_wmts_layer(
        workspace_name=WORKSPACE,
        wmts_store=WMTS_STORE,
        native_layer=WMTS_LAYER,
    )
    assert content == WMTS_LAYER
    assert status == 201

    # Publish the layer in GWC
    content, status = geoserver.publish_gwc_layer(WORKSPACE, WMTS_LAYER)
    assert content == "layer saved"
    assert status == 200
    content, status = geoserver.get_gwc_layer(WORKSPACE, WMTS_LAYER)
    assert status == 200
    assert content.get("GeoServerLayer", {}).get("name") == f"{WORKSPACE}:{WMTS_LAYER}"

    # Perform GetTile request (GWC)
    response = geoserver.get_tile(
        layer=f"{WORKSPACE}:{WMTS_LAYER}",
        tile_matrix_set="EPSG:4326",
        tile_matrix="EPSG:4326:9",
        row=122,
        column=534,
        format=format,
    )
    assert response.info().get("Content-Type") == format

    # Delete layer and store
    content, code = geoserver.delete_gwc_layer(
        workspace_name=WORKSPACE, layer=WMTS_LAYER
    )
    assert content == f"{WORKSPACE}:{WMTS_LAYER} deleted"
    assert code == 200
    response = geoserver.rest_service.rest_client.delete(
        f"{geoserver.rest_service.rest_endpoints.wmtsstore(WORKSPACE,WMTS_STORE)}?recurse=true",
    )
    assert response.status_code == 200
