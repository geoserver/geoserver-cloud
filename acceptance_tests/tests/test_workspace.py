from tests.conftest import GEOSERVER_URL
from geoservercloud import GeoServerCloud


def test_create_get_and_delete_workspace():
    geoserver = GeoServerCloud(GEOSERVER_URL)
    workspace = "test_create_workspace"
    content, status = geoserver.create_workspace(workspace)
    assert content == workspace
    assert status == 201
    content, status = geoserver.get_workspace(workspace)
    assert content == {"name": workspace, "isolated": False}
    assert status == 200
    content, status = geoserver.publish_workspace(workspace)
    assert status == 200
    content, status = geoserver.delete_workspace(workspace)
    assert status == 200


def test_update_workspace():
    geoserver = GeoServerCloud(GEOSERVER_URL)
    workspace = "update_workspace"
    content, status = geoserver.create_workspace(workspace, isolated=True)
    content, status = geoserver.get_workspace(workspace)
    assert content == {"name": workspace, "isolated": True}
    assert status == 200
    content, status = geoserver.create_workspace(workspace, isolated=False)
    assert content == ""
    assert status == 200
    content, status = geoserver.get_workspace(workspace)
    assert content == {"name": workspace, "isolated": False}
    geoserver.delete_workspace(workspace)
