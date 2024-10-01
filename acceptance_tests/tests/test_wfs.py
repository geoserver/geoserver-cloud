from conftest import (
    PGDATABASE,
    PGHOST,
    PGPASSWORD,
    PGPORT,
    PGSCHEMA,
    PGUSER,
    RESOURCE_DIR,
)


def test_wfs(geoserver):
    workspace = datastore = feature_type = "test_wfs"
    attributes = {
        "geom": {
            "type": "Point",
            "required": True,
        },
        "id": {
            "type": "integer",
            "required": True,
        },
        "title": {
            "type": "string",
            "required": False,
        },
        "timestamp": {
            "type": "datetime",
            "required": False,
        },
    }
    response = geoserver.create_workspace(workspace, set_default_workspace=True)
    assert response.status_code == 201
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
    response = geoserver.create_feature_type(
        feature_type, attributes=attributes, epsg=2056
    )
    assert response.status_code == 201

    # Post a feature through a WFS request
    with open(f"{RESOURCE_DIR}/wfs_payload.xml") as file:
        data = file.read()
        response = geoserver.post_request(f"/{workspace}/wfs/", data=data)
        assert response.status_code == 200

    # GetFeature request
    feature_collection = geoserver.get_feature(workspace, feature_type)
    assert type(feature_collection) is dict
    assert type(feature_collection.get("features")) is list
    feature = feature_collection["features"][0]
    properties = feature.get("properties")
    assert properties.get("id") == 10
    assert properties.get("title") == "Title"
    assert properties.get("timestamp") == "2024-05-13T08:14:48.763Z"
    assert feature.get("geometry", {}) == {
        "type": "Point",
        "coordinates": [2600000, 1200000],
    }
    assert feature_collection.get("crs") == {
        "type": "name",
        "properties": {"name": "urn:ogc:def:crs:EPSG::2056"},
    }

    response = geoserver.delete_workspace(workspace)
    assert response.status_code == 200
