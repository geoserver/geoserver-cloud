"""
OGC API Features acceptance tests for GeoServer Cloud.

Tests verify compliance with OGC API Features Core specification:
- Collections endpoint (list all feature types)
- Single collection metadata
- Items endpoint (GeoJSON features)
- Pagination (limit parameter)
- Spatial filtering (bbox parameter)
- Queryables endpoint
- Landing page
- Conformance classes

All tests use the shared geoserver fixture with WORKSPACE and DATASTORE from conftest.py.
"""
import json

from sqlalchemy.sql import text

# These match the WORKSPACE and DATASTORE constants used by the geoserver fixture in conftest.py
WORKSPACE = "test_workspace"
DATASTORE = "test_datastore"


# Endpoint: GET /{workspace}/ogc/features/v1/collections
def test_ogcapi_collections_list(db_session, geoserver):
    """Test OGC API Features collections endpoint lists all feature types"""
    # Use the existing workspace and datastore from the geoserver fixture
    workspace = WORKSPACE
    feature_type1 = "cities"
    feature_type2 = "roads"

    # Create two feature types using the existing datastore
    attributes1 = {
        "geom": {"type": "Point", "required": True},
        "name": {"type": "string", "required": True},
        "population": {"type": "integer", "required": False},
    }
    content, status = geoserver.create_feature_type(
        feature_type1, attributes=attributes1, epsg=2056
    )
    assert status == 201

    attributes2 = {
        "geom": {"type": "Line", "required": True},
        "name": {"type": "string", "required": True},
    }
    content, status = geoserver.create_feature_type(
        feature_type2, attributes=attributes2, epsg=2056
    )
    assert status == 201

    # Test OGC API Features collections endpoint
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/collections?f=application/json"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert "collections" in data
    collections = data["collections"]

    # Find our collections
    collection_ids = [c["id"] for c in collections]
    assert feature_type1 in collection_ids
    assert feature_type2 in collection_ids

    # Verify collection metadata
    cities_collection = next(c for c in collections if c["id"] == feature_type1)
    assert "title" in cities_collection  # GeoServer may return "Default title"
    assert "extent" in cities_collection
    assert "spatial" in cities_collection["extent"]


# Endpoint: GET /{workspace}/ogc/features/v1/collections/{collectionId}
def test_ogcapi_single_collection(db_session, geoserver):
    """Test OGC API Features single collection metadata endpoint"""
    workspace = WORKSPACE
    feature_type = "single"
    attributes = {
        "geom": {"type": "Point", "required": True},
        "name": {"type": "string", "required": True},
    }

    # Create feature type using the existing datastore
    content, status = geoserver.create_feature_type(feature_type, attributes=attributes, epsg=2056)
    assert status == 201

    # Test single collection endpoint
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/collections/{workspace}:{feature_type}?f=application/json"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert data["id"] == feature_type
    assert "title" in data  # GeoServer may return "Default title"
    assert "extent" in data
    assert "links" in data

    # Verify links include items endpoint
    links = data["links"]
    items_links = [link for link in links if link["rel"] == "items"]
    assert len(items_links) > 0


# Endpoint: GET /{workspace}/ogc/features/v1/collections/{collectionId}/items
def test_ogcapi_items_geojson(db_session, geoserver):
    """Test OGC API Features items endpoint returns features as GeoJSON"""
    workspace = WORKSPACE
    feature_type = "items"
    attributes = {
        "geom": {"type": "Point", "required": True},
        "name": {"type": "string", "required": True},
        "category": {"type": "string", "required": False},
    }

    # Create feature type using the existing datastore
    content, status = geoserver.create_feature_type(feature_type, attributes=attributes, epsg=2056)
    assert status == 201

    # Insert test features (using EPSG:2056 coordinates)
    db_session.execute(
        text(
            f"INSERT INTO {feature_type} (geom, name, category) VALUES "
            "(public.ST_SetSRID(public.ST_MakePoint(2600000, 1200000), 2056), 'City A', 'city'), "
            "(public.ST_SetSRID(public.ST_MakePoint(2601000, 1201000), 2056), 'City B', 'city'), "
            "(public.ST_SetSRID(public.ST_MakePoint(2602000, 1202000), 2056), 'City C', 'city')"
        )
    )
    db_session.commit()

    # Test items endpoint
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/collections/{workspace}:{feature_type}/items?f=application/geo%2Bjson"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert data["type"] == "FeatureCollection"
    assert "features" in data
    assert len(data["features"]) == 3

    # Verify feature structure
    feature = data["features"][0]
    assert feature["type"] == "Feature"
    assert "geometry" in feature
    assert feature["geometry"]["type"] == "Point"
    assert "properties" in feature
    assert "name" in feature["properties"]
    assert feature["properties"]["category"] == "city"

    # Note: CRS is implicit in GeoJSON (defaults to WGS84/EPSG:4326)
    # Modern GeoJSON spec doesn't require explicit CRS field


# Endpoint: GET /{workspace}/ogc/features/v1/collections/{collectionId}/items?limit={n}
def test_ogcapi_items_with_limit(db_session, geoserver):
    """Test OGC API Features items endpoint with limit parameter"""
    workspace = WORKSPACE
    feature_type = "pagination_test"
    attributes = {
        "geom": {"type": "Point", "required": True},
        "name": {"type": "string", "required": True},
    }

    # Create feature type using the existing datastore
    content, status = geoserver.create_feature_type(feature_type, attributes=attributes, epsg=2056)
    assert status == 201

    # Insert 5 features
    for i in range(5):
        db_session.execute(
            text(
                f"INSERT INTO {feature_type} (geom, name) VALUES "
                f"(public.ST_SetSRID(public.ST_MakePoint({2600000 + i * 1000}, {1200000 + i * 1000}), 2056), 'Feature {i}')"
            )
        )
    db_session.commit()

    # Test with limit=2
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/collections/{workspace}:{feature_type}/items?f=application/geo%2Bjson&limit=2"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert data["type"] == "FeatureCollection"
    assert len(data["features"]) == 2

    # Verify pagination links exist
    assert "links" in data
    links = data["links"]
    next_links = [link for link in links if link["rel"] == "next"]
    assert len(next_links) > 0, "Should have a 'next' link for pagination"


# Endpoint: GET /{workspace}/ogc/features/v1/collections/{collectionId}/items?bbox={bbox}
def test_ogcapi_items_with_bbox(db_session, geoserver):
    """Test OGC API Features items endpoint with bbox filter"""
    workspace = WORKSPACE
    feature_type = "bbox"
    attributes = {
        "geom": {"type": "Point", "required": True},
        "name": {"type": "string", "required": True},
    }

    # Create feature type using the existing datastore
    content, status = geoserver.create_feature_type(feature_type, attributes=attributes, epsg=2056)
    assert status == 201

    # Insert features in different locations
    db_session.execute(
        text(
            f"INSERT INTO {feature_type} (geom, name) VALUES "
            "(public.ST_SetSRID(public.ST_MakePoint(2600000, 1200000), 2056), 'Inside'), "
            "(public.ST_SetSRID(public.ST_MakePoint(2700000, 1300000), 2056), 'Outside')"
        )
    )
    db_session.commit()

    # Test with bbox that includes only the first point
    # bbox format: minx,miny,maxx,maxy (in WGS84 coordinates)
    # First point is at ~(7.4386, 46.9511), second at ~(8.7745, 47.8428)
    bbox = "7.3,46.8,7.6,47.0"  # Only includes first point in WGS84
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/collections/{workspace}:{feature_type}/items"
        f"?f=application/geo%2Bjson&bbox={bbox}"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert data["type"] == "FeatureCollection"
    assert len(data["features"]) == 1
    assert data["features"][0]["properties"]["name"] == "Inside"


# Endpoint: GET /{workspace}/ogc/features/v1/collections/{collectionId}/queryables
def test_ogcapi_queryables(db_session, geoserver):
    """Test OGC API Features queryables endpoint"""
    workspace = WORKSPACE
    feature_type = "queryables"
    attributes = {
        "geom": {"type": "Point", "required": True},
        "name": {"type": "string", "required": True},
        "age": {"type": "integer", "required": False},
    }

    # Create feature type using the existing datastore
    content, status = geoserver.create_feature_type(feature_type, attributes=attributes, epsg=2056)
    assert status == 201

    # Test queryables endpoint
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/collections/{workspace}:{feature_type}/queryables"
        f"?f=application/json"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    # Queryables should list the attributes that can be used in filtering
    assert "properties" in data or "queryables" in data


# Endpoint: GET /{workspace}/ogc/features/v1
def test_ogcapi_landing_page(geoserver):
    """Test OGC API Features landing page endpoint"""
    workspace = WORKSPACE

    # Test landing page
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1?f=application/json"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert "links" in data

    # Verify conformance and collections links exist
    links = data["links"]
    conformance_links = [link for link in links if link["rel"] == "conformance"]
    collections_links = [link for link in links if link["rel"] == "data"]

    assert len(conformance_links) > 0, "Should have a conformance link"
    assert len(collections_links) > 0, "Should have a collections (data) link"


# Endpoint: GET /{workspace}/ogc/features/v1/conformance
def test_ogcapi_conformance(geoserver):
    """Test OGC API Features conformance endpoint"""
    workspace = WORKSPACE

    # Test conformance endpoint
    response = geoserver.rest_service.rest_client.get(
        f"/{workspace}/ogc/features/v1/conformance?f=application/json"
    )
    assert response.status_code == 200

    data = json.loads(response.content.decode("utf-8"))
    assert "conformsTo" in data

    # Verify it conforms to OGC API Features Core
    conformance_classes = data["conformsTo"]
    assert any(
        "ogcapi-features" in cc or "features/core" in cc for cc in conformance_classes
    )
