"""System tests for catalog changes that cross the REST-config/GWC boundary."""

import os
import time
import uuid
from urllib.parse import quote

import requests


RESTCONFIG_URL = os.getenv("RESTCONFIG_URL", "http://restconfig:8080").rstrip("/")
GWC_URL = os.getenv("GWC_URL", "http://gwc:8080").rstrip("/")
GEOSERVER_USERNAME = os.getenv("GEOSERVER_USERNAME", "admin")
GEOSERVER_PASSWORD = os.getenv("GEOSERVER_PASSWORD", "geoserver")
REQUEST_TIMEOUT = float(os.getenv("SYSTEM_TEST_REQUEST_TIMEOUT", "10"))
EVENT_TIMEOUT = float(os.getenv("SYSTEM_TEST_EVENT_TIMEOUT", "30"))


def _session():
    session = requests.Session()
    session.auth = (GEOSERVER_USERNAME, GEOSERVER_PASSWORD)
    return session


def _response_details(response):
    body = response.text.strip()
    if len(body) > 1000:
        body = f"{body[:1000]}..."
    return f"{response.request.method} {response.url} returned {response.status_code}: {body}"


def _wait_for_tile_layer(session, qualified_name):
    encoded_name = quote(qualified_name, safe=":")
    endpoint = f"{GWC_URL}/gwc/rest/layers/{encoded_name}.json"
    deadline = time.monotonic() + EVENT_TIMEOUT
    last_response = None

    while time.monotonic() < deadline:
        last_response = session.get(endpoint, timeout=REQUEST_TIMEOUT)
        if last_response.status_code == 200:
            return last_response
        time.sleep(0.5)

    assert last_response is not None
    raise AssertionError(
        f"GWC did not expose tile layer {qualified_name!r} within {EVENT_TIMEOUT} seconds; "
        f"last response: {_response_details(last_response)}"
    )


def test_rest_created_layer_is_visible_in_the_gwc_service():
    """REST catalog publication must create tile-layer state consumed by GWC."""

    session = _session()
    layer_name = f"rest_gwc_system_{uuid.uuid4().hex}"
    qualified_name = f"sf:{layer_name}"
    feature_type_endpoint = f"{RESTCONFIG_URL}/rest/workspaces/sf/datastores/sf/featuretypes"
    layer_endpoint = f"{RESTCONFIG_URL}/rest/layers/{quote(qualified_name, safe=':')}.json"
    delete_endpoint = f"{feature_type_endpoint}/{quote(layer_name, safe='')}?recurse=true"

    payload = {
        "featureType": {
            "name": layer_name,
            "nativeName": "roads",
            "title": "REST/GWC system-test layer",
        }
    }

    create_response = session.post(feature_type_endpoint, json=payload, timeout=REQUEST_TIMEOUT)
    assert create_response.status_code == 201, _response_details(create_response)

    try:
        catalog_response = session.get(layer_endpoint, timeout=REQUEST_TIMEOUT)
        assert catalog_response.status_code == 200, _response_details(catalog_response)

        gwc_response = _wait_for_tile_layer(session, qualified_name)
        assert qualified_name in gwc_response.text, _response_details(gwc_response)
    finally:
        delete_response = session.delete(delete_endpoint, timeout=REQUEST_TIMEOUT)
        assert delete_response.status_code in (200, 202), _response_details(delete_response)
