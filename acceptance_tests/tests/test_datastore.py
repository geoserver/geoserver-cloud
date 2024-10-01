from conftest import PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD, PGSCHEMA


def test_create_get_and_delete_datastore(geoserver):
    workspace = datastore = "test_create_pg_datastore"
    geoserver.create_workspace(workspace)
    response = geoserver.create_pg_datastore(
        workspace=workspace,
        datastore=datastore,
        pg_host=PGHOST,
        pg_port=PGPORT,
        pg_db=PGDATABASE,
        pg_user=PGUSER,
        pg_password=PGPASSWORD,
        pg_schema=PGSCHEMA,
        set_default_datastore=True,
    )
    assert response.status_code == 201
    response = geoserver.get_request(
        f"/rest/workspaces/{workspace}/datastores/{datastore}.json"
    )
    assert response.status_code == 200
    response = geoserver.delete_workspace(workspace)
    assert response.status_code == 200
