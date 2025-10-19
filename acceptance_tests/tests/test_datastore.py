from geoservercloud import GeoServerCloud
import pytest
from conftest import (
    GEOSERVER_URL,
    PGHOST,
    PGPORT,
    PGDATABASE,
    PGUSER,
    PGPASSWORD,
    PGSCHEMA,
)

WORKSPACE = "test_pg_datastore"


@pytest.fixture(scope="function")
def geoserver():
    geoserver = GeoServerCloud(url=GEOSERVER_URL)
    geoserver.create_workspace(WORKSPACE, set_default_workspace=True)
    yield geoserver
    geoserver.delete_workspace(WORKSPACE)


def test_create_get_and_delete_datastore(geoserver):
    datastore = "test_pg_datastore"
    content, code = geoserver.create_pg_datastore(
        workspace_name=WORKSPACE,
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
    content, code = geoserver.get_pg_datastore(WORKSPACE, datastore)
    assert content.get("name") == datastore
    assert code == 200
