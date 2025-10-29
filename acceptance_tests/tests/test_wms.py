import json

from tests.conftest import (
    PGDATABASE,
    PGPASSWORD,
    PGSCHEMA,
    PGUSER,
    RESOURCE_DIR,
)
from tests.lib.utils import compare_images, write_actual_image
from sqlalchemy.sql import text


def test_create_and_feature_type_and_get_map(db_session, geoserver_factory):
    workspace = datastore = feature_type = "test_create_feature_type"
    geoserver = geoserver_factory(workspace)
    geoserver.create_pg_datastore(
        workspace_name=workspace,
        datastore_name=datastore,
        pg_host="geodatabase",
        pg_port=5432,
        pg_db=PGDATABASE,
        pg_user=PGUSER,
        pg_password=PGPASSWORD,
        pg_schema=PGSCHEMA,
        set_default_datastore=True,
    )
    _, status = geoserver.create_feature_type(
        feature_type,
        epsg=2056,
    )
    assert status == 201

    # Create feature
    db_session.execute(
        text(
            f"INSERT INTO {feature_type} (geom) VALUES (public.ST_SetSRID(public.ST_MakePoint(2600000, 1200000), 2056))"
        )
    )
    db_session.commit()

    # GetMap request
    response = geoserver.get_map(
        layers=[feature_type],
        bbox=(2599999.5, 1199999.5, 2600000.5, 1200000.5),
        size=(40, 40),
        format="image/png",
        transparent=False,
    )

    file_root = f"getmap"
    write_actual_image(response, file_root)
    compare_images(RESOURCE_DIR, file_root)


def test_get_feature_info(db_session, geoserver_factory):
    workspace = datastore = feature_type = "test_get_feature_info"
    geoserver = geoserver_factory(workspace)
    attributes = {
        "geom": {
            "type": "Point",
            "required": True,
        },
        "label": {
            "type": "string",
            "required": False,
        },
    }
    _, status = geoserver.create_pg_datastore(
        workspace_name=workspace,
        datastore_name=datastore,
        pg_host="geodatabase",
        pg_port=5432,
        pg_db=PGDATABASE,
        pg_user=PGUSER,
        pg_password=PGPASSWORD,
        pg_schema=PGSCHEMA,
        set_default_datastore=True,
    )
    assert status == 201
    _, status = geoserver.create_feature_type(
        feature_type, attributes=attributes, epsg=2056
    )
    assert status == 201

    # Create feature
    db_session.execute(
        text(
            f"INSERT INTO {feature_type} (geom, label) VALUES "
            "(public.ST_SetSRID(public.ST_MakePoint(2600000, 1200000), 2056), 'Label')"
        )
    )
    db_session.commit()

    # Test that layer is published
    _, status = geoserver.get_feature_type(workspace, datastore, feature_type)
    assert status == 200

    # GetFeatureInfo request
    response = geoserver.get_feature_info(
        layers=[feature_type],
        bbox=(2599999.5, 1199999.5, 2600000.5, 1200000.5),
        size=(40, 40),
        info_format="application/json",
        xy=(20, 20),
    )

    data = json.loads(response.read().decode("utf-8"))

    feature = data.get("features", [])[0]
    assert feature
    assert feature.get("properties").get("label") == "Label"
    assert feature.get("geometry") == {
        "type": "Point",
        "coordinates": [2600000, 1200000],
    }
    assert data.get("crs") == {
        "type": "name",
        "properties": {"name": "urn:ogc:def:crs:EPSG::2056"},
    }
