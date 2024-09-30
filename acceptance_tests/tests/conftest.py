import os

import pytest
from geoservercloud import GeoServerCloud


GEOSERVER_URL = os.getenv("GEOSERVER_URL", "http://gateway:8080/geoserver/cloud")


@pytest.fixture(scope="module")
def geoserver():
    yield GeoServerCloud(GEOSERVER_URL)
