import pytest
from geoservercloud import GeoServerCloud


@pytest.fixture(scope="module")
def geoserver():
    yield GeoServerCloud("http://gateway:8080/geoserver/cloud")
