import json

from geoservercloud import GeoServerCloud

from tests.conftest import GEOSERVER_URL

EXPECTED_FONTS = [
    "Noto Sans",
    "Noto Sans Bold",
    "FontAwesome",
    "DejaVu Sans",
    "Material Design Icons",
    "Material Icons",
    "Roboto",
]


def test_installed_fonts():
    """Verify that custom fonts are installed in GeoServer."""
    geoserver = GeoServerCloud(url=GEOSERVER_URL)

    response = geoserver.rest_service.rest_client.get(
        "/rest/fonts",
        headers={"Accept": "application/json"},
    )

    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    fonts = data.get("fonts", [])

    for font in EXPECTED_FONTS:
        assert font in fonts, f"Expected font '{font}' not found in installed fonts"
