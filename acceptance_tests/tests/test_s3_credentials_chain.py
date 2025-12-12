"""
Integration tests for cloud native data formats on private S3 buckets using AWS credentials chain.

These tests verify that GeoServer Cloud can access and serve cloud-optimized formats (COG, GeoParquet, PMTiles)
stored on private S3 buckets by using the AWS default credentials chain for authentication.

The credentials can be provided through:
- Environment variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
- AWS credentials file: ~/.aws/credentials (mounted at /opt/app/home/.aws/credentials in containers)
- IAM roles (when running on AWS infrastructure)

In local development, credentials are typically mounted from compose/secrets/aws directory.
In CI/CD (GitHub Actions), credentials are configured from repository secrets.
"""
from pathlib import Path
from typing import Any
import pytest

from geoservercloud import GeoServerCloud
from geoservercloud.models.common import KeyDollarListDict

# Skip all tests in this module if AWS credentials file doesn't exist
# This allows running tests locally without credentials and in CI with GitHub secrets
CREDENTIALS_FILE = Path(__file__).parent.parent.parent / "compose/secrets/aws/credentials"
pytestmark = pytest.mark.skipif(
    not CREDENTIALS_FILE.exists(),
    reason="AWS credentials not available. Set up compose/secrets/aws/credentials to run these tests.",
)

# Monkey-patch EPSG:3857 support into geoservercloud library
# TODO: Propose this fix to upstream geoservercloud library
# See: https://github.com/camptocamp/python-geoservercloud
#
# The library claims to support EPSG:3857 in its documentation and has gridset files for it,
# but the EPSG_BBOX lookup dictionary in utils.py only contains 2056 and 4326.
# This causes a KeyError when trying to create feature types with epsg=3857.
#
# EPSG:3857 (Web Mercator) bounding box values derived from gridsets/3857.xml:
# - Native: ±20,037,508.34 meters (Web Mercator projection space)
# - Lat/Lon: ±180° longitude, ±85.05° latitude (Web Mercator's WGS84 limits)
import geoservercloud.utils as gs_utils

gs_utils.EPSG_BBOX[3857] = {
    "nativeBoundingBox": {
        "crs": {"$": "EPSG:3857", "@class": "projected"},
        "maxx": 20037508.34,
        "maxy": 20037508.34,
        "minx": -20037508.34,
        "miny": -20037508.34,
    },
    "latLonBoundingBox": {
        "crs": "EPSG:4326",
        "maxx": 180.0,
        "maxy": 85.0511287798,
        "minx": -180.0,
        "miny": -85.0511287798,
    },
}


def create_datastore(
    geoserver: GeoServerCloud,
    workspace_name: str,
    datastore_name: str,
    datastore_type: str,
    connection_parameters: dict[str, Any],
    description: str | None = None,
    enabled: bool = True,
    set_default_datastore: bool = False,
) -> tuple[str, int]:
    """
    Create a generic datastore of any type in GeoServer.

    This is a standalone helper function that can create any type of datastore
    by accepting flexible connection parameters.

    Args:
        geoserver: GeoServerCloud instance to use for API calls
        workspace_name: Name of the workspace
        datastore_name: Name for the datastore
        datastore_type: Type of datastore (e.g., "PostGIS", "Shapefile", "Directory of spatial files (shapefiles)")
        connection_parameters: Dict of connection parameters specific to the datastore type
        description: Optional description
        enabled: Whether the datastore should be enabled (default: True)
        set_default_datastore: Whether to set as default datastore (default: False)

    Returns:
        Tuple of (datastore_name, status_code)

    Example:
        create_datastore(
            geoserver=geoserver,
            workspace_name="myworkspace",
            datastore_name="my_store",
            datastore_type="PostGIS",
            connection_parameters={
                "dbtype": "postgis",
                "host": "localhost",
                "port": 5432,
                "database": "mydb",
                "user": "user",
                "passwd": "password",
                "schema": "public",
                "namespace": "http://myworkspace",
                "Expose primary keys": "true",
            }
        )
    """
    # Convert connection parameters to GeoServer's KeyDollar format
    connection_params = KeyDollarListDict(input_dict=connection_parameters)

    # Build the datastore payload
    datastore_payload = {
        "name": datastore_name,
        "type": datastore_type,
        "connectionParameters": {"entry": connection_params.serialize()},
        "workspace": {"name": workspace_name},
        "enabled": enabled,
    }

    # Add optional description if provided
    if description is not None:
        datastore_payload["description"] = description

    # Wrap in dataStore envelope
    payload = {"dataStore": datastore_payload}

    # Check if datastore already exists (upsert pattern)
    datastore_url = geoserver.rest_service.rest_endpoints.datastore(
        workspace_name, datastore_name
    )

    if not geoserver.rest_service.resource_exists(datastore_url):
        # Create new datastore (POST)
        datastores_url = geoserver.rest_service.rest_endpoints.datastores(workspace_name)
        response = geoserver.rest_service.rest_client.post(datastores_url, json=payload)
    else:
        # Update existing datastore (PUT)
        response = geoserver.rest_service.rest_client.put(datastore_url, json=payload)

    # Set as default datastore if requested
    if set_default_datastore:
        geoserver.default_datastore = datastore_name

    return response.content.decode(), response.status_code


def test_cog(geoserver_factory):
    """Test creating a COG coverage store and coverage on private S3 bucket"""
    workspace = "s3_private_cog"
    store_name = "land_shallow_topo_21600_NE_cog"
    coverage_name = "land_shallow_topo_21600_NE_cog"
    geoserver = geoserver_factory(workspace)

    # Create COG coverage store
    content, status = geoserver.create_coverage_store(
        workspace_name=workspace,
        coveragestore_name=store_name,
        type="GeoTIFF",
        url=f"cog://https://s3-us-east-1.amazonaws.com/geoserver-test-data-private/cog/land_shallow_topo/land_shallow_topo_21600_NE_cog.tif",
        metadata={"cogSettings": {"rangeReaderSettings": "S3"}},
    )
    assert status == 201
    assert content == store_name

    # Create coverage
    content, status = geoserver.create_coverage(
        workspace_name=workspace,
        coveragestore_name=store_name,
        coverage_name=coverage_name,
        native_name=store_name,
    )
    assert status == 201
    assert content == coverage_name

    # Verify the coverage was created - try listing coverages first
    content, status = geoserver.get_coverages(workspace, store_name)
    assert status == 200, f"Failed to get coverages: {status} - {content}"
    assert content[0].get("name") == store_name

    # Check specific coverage
    content, status = geoserver.get_coverage(workspace, store_name, coverage_name)
    assert status == 200, f"Failed to get coverage: {status} - {content}"

    # Verify coverage properties
    assert content.get("name") == coverage_name
    assert content.get("nativeName") == store_name
    assert content.get("enabled") is True

    # Test WMS GetMap request
    wms_response = geoserver.get_map(
        layers=[f"{workspace}:{coverage_name}"],
        bbox=(0, 0, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/jpeg",
    )._response
    assert wms_response.status_code == 200
    assert wms_response.headers.get("content-type").startswith("image/jpeg")


def test_geoparquet(geoserver_factory):
    """Test creating a GeoParquet datastore on private S3 bucket"""
    workspace = "s3_private_geoparquet"
    datastore = "germany"
    geoserver = geoserver_factory(workspace)

    # Create GeoParquet datastore with S3 credentials chain
    content, status = create_datastore(
        geoserver=geoserver,
        workspace_name=workspace,
        datastore_name=datastore,
        datastore_type="GeoParquet",
        connection_parameters={
            "dbtype": "geoparquet",
            "uri": "s3://geoserver-test-data-private/geoparquet/overture/singlefiles/germany/*",
            "namespace": workspace,
            "use_aws_credential_chain": True,
            "fetch size": 1000,
            "screenmap": True,
            "simplification": True,
        },
        description="GeoParquet datastore on private S3 bucket using AWS credentials chain",
    )
    assert status == 201, f"Failed to create datastore: {status} - {content}"
    assert content == datastore

    # Verify the datastore was created
    datastores, status = geoserver.get_datastores(workspace)
    assert status == 200
    assert datastore in [ds.get("name") for ds in datastores]

    # Get the datastore details
    datastore_info, status = geoserver.get_pg_datastore(workspace, datastore)
    assert status == 200
    assert datastore_info.get("name") == datastore
    assert datastore_info.get("type") == "GeoParquet"
    assert datastore_info.get("enabled") is True

    # Create feature type with explicit attributes since GeoParquet schema is known
    feature_type = "addresses"
    attributes = {
        "geometry": {"type": "Point", "required": False},
        "bbox": {"type": "string", "required": False},  # Struct type
        "country": {"type": "string", "required": False},
        "postcode": {"type": "string", "required": False},
        "street": {"type": "string", "required": False},
        "number": {"type": "string", "required": False},
        "unit": {"type": "string", "required": False},
        "postal_city": {"type": "string", "required": False},
        "version": {"type": "integer", "required": False},
        "theme": {"type": "string", "required": False},
        "type": {"type": "string", "required": False},
    }
    content, status = geoserver.create_feature_type(
        layer_name=feature_type,
        workspace_name=workspace,
        datastore_name=datastore,
        title="Germany Addresses from Overture Maps",
        abstract="Address points from Overture Maps for Germany, stored as GeoParquet on S3",
        epsg=4326,
        attributes=attributes,
    )
    assert status == 201, f"Failed to create feature type: {status} - {content}"

    # Verify the feature type was created
    feature_types, status = geoserver.get_feature_types(workspace, datastore)
    assert status == 200
    assert feature_type in [ft.get("name") for ft in feature_types]

    # Get the feature type details
    ft_info, status = geoserver.get_feature_type(workspace, datastore, feature_type)
    assert status == 200
    assert ft_info.get("name") == feature_type
    assert ft_info.get("enabled") is True

    # Test WFS GetFeature request (limit to 10 features for faster test)
    feature_collection = geoserver.get_feature(workspace, feature_type, max_feature=10)
    assert isinstance(feature_collection, dict)
    assert isinstance(feature_collection.get("features"), list)
    assert len(feature_collection.get("features")) > 0

    # Verify feature has expected properties
    feature = feature_collection["features"][0]
    properties = feature.get("properties")
    assert "geometry" in feature or "geometry" in properties
    # Check for some expected GeoParquet attributes
    assert any(
        key in properties for key in ["country", "street", "postcode", "postal_city"]
    )


def test_pmtiles(geoserver_factory):
    """Test creating a PMTiles datastore on private S3 bucket"""
    workspace = "s3_private_pmtiles"
    datastore = "europe"
    geoserver = geoserver_factory(workspace)

    # Create PMTiles datastore with S3 credentials chain
    content, status = create_datastore(
        geoserver=geoserver,
        workspace_name=workspace,
        datastore_name=datastore,
        datastore_type="PMTiles",
        connection_parameters={
            "pmtiles": "s3://geoserver-test-data-private/pmtiles/shortbread/europe.pmtiles",
            "namespace": workspace,
            "io.tileverse.rangereader.s3.use-default-credentials-provider": True,
            "io.tileverse.rangereader.caching.enabled": True,
            "io.tileverse.rangereader.caching.blockaligned": True,
        },
        description="PMTiles datastore on private S3 bucket using AWS credentials chain",
    )
    assert status == 201, f"Failed to create datastore: {status} - {content}"
    assert content == datastore

    # Verify the datastore was created
    datastores, status = geoserver.get_datastores(workspace)
    assert status == 200
    assert datastore in [ds.get("name") for ds in datastores]

    # Get the datastore details
    datastore_info, status = geoserver.get_pg_datastore(workspace, datastore)
    assert status == 200
    assert datastore_info.get("name") == datastore
    assert datastore_info.get("type") == "PMTiles"
    assert datastore_info.get("enabled") is True

    # Create feature type with explicit attributes for PMTiles boundaries layer
    # Using EPSG:3857 (Web Mercator) - supported via monkey-patch at top of file
    feature_type = "boundaries"
    attributes = {
        "the_geom": {"type": "MultiPolygon", "required": False},  # Boundaries are typically MultiPolygons
        "admin_level": {"type": "double", "required": False},
        "disputed": {"type": "boolean", "required": False},
        "maritime": {"type": "boolean", "required": False},
    }
    content, status = geoserver.create_feature_type(
        layer_name=feature_type,
        workspace_name=workspace,
        datastore_name=datastore,
        title="Boundaries from Shortbread Europe PMTiles",
        abstract="Administrative boundaries from Shortbread schema, stored as PMTiles on S3",
        epsg=3857,  # PMTiles uses Web Mercator (EPSG:3857)
        attributes=attributes,
    )
    assert status == 201, f"Failed to create feature type: {status} - {content}"

    # Verify the feature type was created
    feature_types, status = geoserver.get_feature_types(workspace, datastore)
    assert status == 200
    assert feature_type in [ft.get("name") for ft in feature_types]

    # Get the feature type details
    ft_info, status = geoserver.get_feature_type(workspace, datastore, feature_type)
    assert status == 200
    assert ft_info.get("name") == feature_type
    assert ft_info.get("enabled") is True
    assert ft_info.get("srs") == "EPSG:3857"

    # Test WFS GetFeature request (limit to 10 features for faster test)
    feature_collection = geoserver.get_feature(workspace, feature_type, max_feature=10)
    assert isinstance(feature_collection, dict)
    assert isinstance(feature_collection.get("features"), list)
    assert len(feature_collection.get("features")) > 0

    # Verify feature has expected properties
    feature = feature_collection["features"][0]
    properties = feature.get("properties")
    assert "geometry" in feature or "the_geom" in properties
    # Check for some expected PMTiles attributes
    assert any(key in properties for key in ["admin_level", "disputed", "maritime"])
