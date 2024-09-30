import json

import pytest
from conftest import GEOSERVER_URL
from geoservercloud import GeoServerCloud

WORKSPACE = "test_cascade"
WMS_STORE = "test_cascaded_wms_store"
WMS_URL = "https://wms.geo.admin.ch/?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities"
WMS_LAYER = "ch.swisstopo.swissboundaries3d-gemeinde-flaeche.fill"
WMTS_STORE = "test_cascaded_wmts_store"
WMTS_URL = "https://wmts.geo.admin.ch/EPSG/4326/1.0.0/WMTSCapabilities.xml"
WMTS_LAYER = "ch.swisstopo.pixelkarte-grau"


def create_cascaded_wms_store_payload():
    return {
        "wmsStore": {
            "name": WMS_STORE,
            "type": "WMS",
            "enabled": "true",
            "workspace": {"name": WORKSPACE},
            "metadata": {"entry": {"@key": "useConnectionPooling", "$": "true"}},
            "_default": "false",
            "disableOnConnFailure": "false",
            "capabilitiesURL": WMS_URL,
            "maxConnections": 6,
            "readTimeout": 60,
            "connectTimeout": 30,
        }
    }


def delete_wms_store(geoserver):
    geoserver.delete_request(
        f"/rest/workspaces/{WORKSPACE}/wmsstores/{WMS_STORE}?recurse=true"
    )


def delete_wmts_store(geoserver):
    geoserver.delete_request(
        f"/rest/workspaces/{WORKSPACE}/wmtsstores/{WMTS_STORE}?recurse=true"
    )


@pytest.fixture(scope="module")
def geoserver():
    geoserver = GeoServerCloud(url=GEOSERVER_URL)
    geoserver.create_workspace(WORKSPACE, set_default_workspace=True)
    geoserver.publish_workspace(WORKSPACE)
    yield geoserver
    # geoserver.delete_workspace(WORKSPACE)


def test_cascaded_wms(geoserver):
    format = "image/jpeg"

    # Create WMS store
    payload = create_cascaded_wms_store_payload()
    response = geoserver.post_request(
        f"/rest/workspaces/{WORKSPACE}/wmsstores", json=payload
    )
    assert response.status_code == 201

    # Publish layer
    payload = {
        "wmsLayer": {
            "name": WMS_LAYER,
        }
    }
    response = geoserver.post_request(
        f"/rest/workspaces/{WORKSPACE}/wmsstores/{WMS_STORE}/wmslayers",
        json=payload,
    )
    assert response.status_code == 201

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

    delete_wms_store(geoserver)


def test_cascaded_wmts(geoserver):
    format = "image/jpeg"

    # Create WMTS store
    response = geoserver.create_wmts_store(
        WORKSPACE,
        WMTS_STORE,
        capabilities="https://wmts.geo.admin.ch/EPSG/4326/1.0.0/WMTSCapabilities.xml",
    )
    assert response.status_code == 201

    # Publish layer (GeoServer)
    response = geoserver.create_wmts_layer(WORKSPACE, WMTS_STORE, WMTS_LAYER)
    assert response.status_code == 201
    response = geoserver.get_request(
        f"/rest/workspaces/{WORKSPACE}/wmtsstores/{WMTS_STORE}/layers/{WMTS_LAYER}.json"
    )
    assert response.status_code == 200

    # Publish the layer in GWC
    response = geoserver.publish_gwc_layer(WORKSPACE, WMTS_LAYER)
    assert response.status_code == 200

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

    response = geoserver.delete_request(f"/gwc/rest/layers/{WORKSPACE}:{WMTS_LAYER}")
    assert response.status_code == 200
    delete_wmts_store(geoserver)
