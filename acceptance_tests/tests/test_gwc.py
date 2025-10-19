import pytest

WORKSPACE = "test_gwc"
WMTS_STORE = "test_gwc_store"
WMTS_LAYER = "ch.swisstopo.swissimage"


@pytest.fixture(scope="module")
def geoserver_with_gwc_layers(geoserver):
    geoserver.create_workspace(WORKSPACE)
    geoserver.create_wmts_store(
        WORKSPACE,
        WMTS_STORE,
        capabilities="https://wmts.geo.admin.ch/EPSG/4326/1.0.0/WMTSCapabilities.xml",
    )
    geoserver.create_wmts_layer(WORKSPACE, WMTS_STORE, WMTS_LAYER)
    geoserver.publish_gwc_layer(WORKSPACE, WMTS_LAYER)
    yield geoserver
    geoserver.delete_gwc_layer(WORKSPACE, WMTS_LAYER)
    geoserver.delete_workspace(WORKSPACE)


def test_tile_cache(geoserver_with_gwc_layers):

    response = geoserver_with_gwc_layers.get_tile(
        format="image/png",
        layer=f"{WORKSPACE}:{WMTS_LAYER}",
        tile_matrix_set="EPSG:4326",
        tile_matrix="EPSG:4326:9",
        row=122,
        column=534,
    )
    assert response.info().get("Content-Type") == "image/png"
    assert response.info().get("Geowebcache-Cache-Result") == "MISS"

    response = geoserver_with_gwc_layers.get_tile(
        format="image/png",
        layer=f"{WORKSPACE}:{WMTS_LAYER}",
        tile_matrix_set="EPSG:4326",
        tile_matrix="EPSG:4326:9",
        row=122,
        column=534,
    )
    assert response.info().get("Geowebcache-Cache-Result") == "HIT"
