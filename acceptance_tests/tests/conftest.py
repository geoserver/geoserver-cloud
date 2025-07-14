import os
from pathlib import Path

import pytest
import sqlalchemy
from geoservercloud import GeoServerCloud

GEOSERVER_URL = os.getenv("GEOSERVER_URL", "http://gateway:8080/geoserver/cloud")
RESOURCE_DIR = Path(__file__).parent / "resources"
# Database connection - defaults for container, can be overridden for local testing
PGHOST = os.getenv("PGHOST", "geodatabase")
PGPORT = int(os.getenv("PGPORT", "5432"))
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
def geoserver():
    geoserver = GeoServerCloud(GEOSERVER_URL)
    geoserver.recreate_workspace(WORKSPACE, set_default_workspace=True)
    geoserver.create_pg_datastore(
        workspace=WORKSPACE,
        datastore=DATASTORE,
        pg_host=PGHOST,
        pg_port=PGPORT,
        pg_db=PGDATABASE,
        pg_user=PGUSER,
        pg_password=PGPASSWORD,
        pg_schema=PGSCHEMA,
        set_default_datastore=True,
    )
    geoserver.publish_workspace(WORKSPACE)
    yield geoserver
    geoserver.delete_workspace(WORKSPACE)
