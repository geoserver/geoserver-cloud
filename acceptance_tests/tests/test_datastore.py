from tests.conftest import (
    PGHOST,
    PGPORT,
    PGDATABASE,
    PGUSER,
    PGPASSWORD,
    PGSCHEMA,
)

WORKSPACE = "test_pg_datastore"


def test_create_get_and_delete_datastore(geoserver_factory):
    workspace = "test_pg_datastore"
    datastore = "test_pg_datastore"
    geoserver = geoserver_factory(workspace)
    content, code = geoserver.create_pg_datastore(
        workspace_name=workspace,
        datastore_name=datastore,
        pg_host=PGHOST,
        pg_port=PGPORT,
        pg_db=PGDATABASE,
        pg_user=PGUSER,
        pg_password=PGPASSWORD,
        pg_schema=PGSCHEMA,
        set_default_datastore=True,
    )
    assert content == datastore
    assert code == 201
    content, code = geoserver.get_pg_datastore(workspace, datastore)
    assert content.get("name") == datastore
    assert code == 200
