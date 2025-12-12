import os
from pathlib import Path

import pytest
import sqlalchemy
from geoservercloud import GeoServerCloud

GEOSERVER_URL = os.getenv("GEOSERVER_URL", "http://gateway:8080/geoserver/cloud")
RESOURCE_DIR = Path(__file__).parent / "resources"

# Database connection for test client (can be overridden for local testing)
PGHOST = os.getenv("PGHOST", "geodatabase")
PGPORT = int(os.getenv("PGPORT", "5432"))

# Database connection for GeoServer datastores (must use container names)
# These are separate because GeoServer runs inside containers and needs
# to use the internal Docker network, while tests may run on the host
PGHOST_GEOSERVER = os.getenv("PGHOST_GEOSERVER", "geodatabase")
PGPORT_GEOSERVER = int(os.getenv("PGPORT_GEOSERVER", "5432"))

PGDATABASE = os.getenv("PGDATABASE", "acceptance")
PGUSER = os.getenv("PGUSER", "geoserver")
PGPASSWORD = os.getenv("PGPASSWORD", "geoserver")
PGSCHEMA = os.getenv("PGSCHEMA", "test1")
WORKSPACE = "test_workspace"
DATASTORE = "test_datastore"


@pytest.fixture(scope="session", autouse=True)
def engine():
    yield sqlalchemy.create_engine(
        f"postgresql://{PGUSER}:{PGPASSWORD}@{PGHOST}:{PGPORT}/{PGDATABASE}",
    )


@pytest.fixture(scope="session", autouse=True)
def db_session(engine):
    with engine.connect() as connection:
        connection.execute(
            sqlalchemy.sql.text(f"CREATE SCHEMA IF NOT EXISTS {PGSCHEMA}")
        )
        connection.execute(sqlalchemy.sql.text(f"SET SEARCH_PATH = {PGSCHEMA}"))
        connection.commit()
        yield connection
        connection.execute(
            sqlalchemy.sql.text(f"DROP SCHEMA IF EXISTS {PGSCHEMA} CASCADE")
        )
        connection.commit()


@pytest.fixture(scope="module")
def geoserver_factory(request):
    """
    Factory fixture to create a GeoServerCloud instance with a dedicated workspace.
    Cleanup (workspace deletion) is handled automatically.
    """

    def _create(workspace_name):
        geoserver = GeoServerCloud(url=GEOSERVER_URL)
        geoserver.create_workspace(workspace_name, set_default_workspace=True)
        geoserver.publish_workspace(workspace_name)

        # Register cleanup for this workspace
        request.addfinalizer(lambda: geoserver.delete_workspace(workspace_name))
        return geoserver

    return _create


@pytest.fixture(scope="module")
def geoserver(request):
    """
    Shared GeoServerCloud instance with workspace and PostGIS datastore.
    Used by tests that need a consistent workspace/datastore setup (like OGC API Features tests).
    """
    geoserver = GeoServerCloud(url=GEOSERVER_URL)
    geoserver.recreate_workspace(WORKSPACE, set_default_workspace=True)
    geoserver.create_pg_datastore(
        workspace_name=WORKSPACE,
        datastore_name=DATASTORE,
        pg_host=PGHOST_GEOSERVER,  # Use container network parameters
        pg_port=PGPORT_GEOSERVER,
        pg_db=PGDATABASE,
        pg_user=PGUSER,
        pg_password=PGPASSWORD,
        pg_schema=PGSCHEMA,
        set_default_datastore=True,
    )
    geoserver.publish_workspace(WORKSPACE)

    # Register cleanup
    request.addfinalizer(lambda: geoserver.delete_workspace(WORKSPACE))

    return geoserver
