def test_create_get_and_delete_workspace(geoserver):
    workspace = "test_create_workspace"
    response = geoserver.create_workspace(workspace)
    assert response.status_code == 201
    response = geoserver.get_request(f"/rest/workspaces/{workspace}.json")
    assert response.status_code == 200
    response = geoserver.publish_workspace(workspace)
    assert response.status_code == 200
    response = geoserver.delete_workspace(workspace)
    assert response.status_code == 200


def test_update_workspace(geoserver):
    workspace = "update_workspace"
    response = geoserver.create_workspace(workspace, isolated=True)
    assert response.status_code == 201
    response = geoserver.get_request(f"/rest/workspaces/{workspace}.json")
    assert response.json().get("workspace").get("isolated") == True
    response = geoserver.create_workspace(workspace, isolated=False)
    assert response.status_code == 200
    response = geoserver.get_request(f"/rest/workspaces/{workspace}.json")
    assert response.json().get("workspace").get("isolated") == False
    geoserver.delete_workspace(workspace)
