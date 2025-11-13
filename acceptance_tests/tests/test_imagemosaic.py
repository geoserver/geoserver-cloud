"""
ImageMosaic acceptance tests for GeoServer Cloud

Tests various workflows for creating ImageMosaic stores and layers:
- Direct directory creation (like web UI)
- Manual granule addition
- Empty store creation with directory/file harvesting
- XML-based store creation

All tests use sample data from a shared mount volume at /opt/geoserver_data
that is accessible to both the test environment and GeoServer containers.
"""

import tempfile
import zipfile
from pathlib import Path


def test_create_imagemosaic_local_files(geoserver_factory):
    """Test creating an ImageMosaic using local sample data files via direct directory approach"""
    workspace = "local_sampledata"
    store_name = "ne_pyramid_store"
    coverage_name = "pyramid"
    geoserver = geoserver_factory(workspace)

    # Use direct directory approach (like web UI) instead of individual file URLs
    directory_path = "/opt/geoserver_data/sampledata/ne/pyramid/"

    # Create ImageMosaic store directly from directory
    content, status = geoserver.create_imagemosaic_store_from_directory(
        workspace_name=workspace,
        coveragestore_name=store_name,
        directory_path=directory_path,
    )
    assert status == 201, f"Failed to create ImageMosaic from directory: {content}"
    assert content == store_name

    # List available coverages (should be auto-discovered)
    content, status = geoserver.get_coverages(
        workspace_name=workspace, coveragestore_name=store_name
    )
    assert status == 200, f"Failed to list coverages: {content}"
    assert content[0].get("name") == coverage_name

    # Check if coverage was auto-created (likely scenario)
    coverage_data, status = geoserver.get_coverage(workspace, store_name, coverage_name)
    assert status == 200
    assert coverage_data.get("name") == coverage_name
    assert coverage_data.get("nativeName") == coverage_name
    assert coverage_data.get("enabled") == True

    # Test WMS GetMap request (verify local file mosaic works)
    wms_response = geoserver.get_map(
        layers=[f"{workspace}:{coverage_name}"],
        bbox=(-180, -90, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/png",
    )._response
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")

    # Delete coverage store
    content, status = geoserver.delete_coverage_store(workspace, store_name)
    assert status == 200, f"Failed to delete coverage store: {content}"
    assert content == ""


def test_create_imagemosaic_manual_granules(geoserver_factory):
    workspace = "manual_granules"
    store_name = "manual_granules_store"
    coverage_name = "manual_granules_coverage"
    geoserver = geoserver_factory(workspace)

    # Create temporary directory for mosaic configuration
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)

        # Create indexer.properties for manual granule addition
        indexer_content = f"""MosaicCRS=EPSG\\:4326
Name={coverage_name}
PropertyCollectors=CRSExtractorSPI(crs),ResolutionExtractorSPI(resolution)
Schema=*the_geom:Polygon,location:String,crs:String,resolution:String
CanBeEmpty=true
AbsolutePath=true"""

        indexer_file = tmp_path / "indexer.properties"
        indexer_file.write_text(indexer_content)

        # Create datastore.properties (using JNDI like in COG tests)
        datastore_content = """SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
# JNDI data source
jndiReferenceName=java:comp/env/jdbc/postgis

#Boolean
# perform only primary filter on bbox
# Default Boolean.TRUE
Loose\\ bbox=true

#Boolean
# use prepared statements
#Default Boolean.FALSE
preparedStatements=false
"""
        datastore_file = tmp_path / "datastore.properties"
        datastore_file.write_text(datastore_content)

        # Create ZIP file with both configuration files
        zip_file = tmp_path / "manual-granules-config.zip"
        with zipfile.ZipFile(zip_file, "w") as zf:
            zf.write(indexer_file, "indexer.properties")
            zf.write(datastore_file, "datastore.properties")

        # Create empty ImageMosaic store
        with open(zip_file, "rb") as f:
            zip_data = f.read()

        content, status = geoserver.create_imagemosaic_store_from_properties_zip(
            workspace_name=workspace,
            coveragestore_name=store_name,
            properties_zip=zip_data,
        )
        assert status == 201, f"Failed to create ImageMosaic store: {content}"
        assert content == ""

    # Manually add individual granules from the sample data
    granule_paths = [
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_1_1.tif",
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_1_2.tif",
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_2_1.tif",
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_2_2.tif",
    ]

    for granule_path in granule_paths:
        # Use direct file paths (without file:// protocol) for external.imagemosaic
        content, status = geoserver.publish_granule_to_coverage_store(
            workspace_name=workspace,
            coveragestore_name=store_name,
            method="external",
            granule_path=granule_path,
        )
        assert status in [
            201,
            202,
        ], f"Failed to publish granule {granule_path}: {content}"

    # Initialize the store (list available coverages)
    content, status = geoserver.get_coverages(
        workspace_name=workspace, coveragestore_name=store_name
    )
    assert status == 200, f"Failed to list coverages: {content}"
    assert content[0].get("name") == coverage_name

    # Create layer/coverage
    content, status = geoserver.create_coverage(
        workspace_name=workspace,
        coveragestore_name=store_name,
        coverage_name=coverage_name,
        title="Manual Granules Test Coverage",
    )
    assert status == 201, f"Failed to create coverage: {content}"
    assert content == coverage_name

    # Verify the coverage was created successfully
    coverage_data, status = geoserver.get_coverage(workspace, store_name, coverage_name)
    assert status == 200
    assert coverage_data.get("name") == coverage_name
    assert coverage_data.get("nativeName") == coverage_name
    assert coverage_data.get("enabled") == True
    assert coverage_data.get("title") == "Manual Granules Test Coverage"

    # Test WMS GetMap request (verify manual granule addition works)
    wms_response = geoserver.get_map(
        layers=[f"{workspace}:{coverage_name}"],
        bbox=(-180, -90, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/png",
    )._response
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")


def test_create_imagemosaic_empty_store_with_directory_harvest(geoserver_factory):
    """
    Test creating an empty ImageMosaic store first, then harvesting granules from a directory.
    This tests the workflow: create store -> harvest directory -> create layer.
    """
    workspace = "directory_harvest"
    store_name = "directory_harvest_store"
    coverage_name = "directory_harvest_coverage"
    geoserver = geoserver_factory(workspace)

    # Step 2: Create ImageMosaic store with configuration
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)

        # Create indexer.properties
        indexer_content = f"""MosaicCRS=EPSG\\:4326
Name={coverage_name}
PropertyCollectors=CRSExtractorSPI(crs),ResolutionExtractorSPI(resolution)
Schema=*the_geom:Polygon,location:String,crs:String,resolution:String
CanBeEmpty=true
AbsolutePath=true"""

        indexer_file = tmp_path / "indexer.properties"
        indexer_file.write_text(indexer_content)

        # Create datastore.properties (using JNDI)
        datastore_content = """SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
# JNDI data source
jndiReferenceName=java:comp/env/jdbc/postgis

#Boolean
# perform only primary filter on bbox
# Default Boolean.TRUE
Loose\\ bbox=true

#Boolean
# use prepared statements
#Default Boolean.FALSE
preparedStatements=false
"""
        datastore_file = tmp_path / "datastore.properties"
        datastore_file.write_text(datastore_content)

        # Create ZIP file with both configuration files
        zip_file = tmp_path / "mosaic-config.zip"
        with zipfile.ZipFile(zip_file, "w") as zf:
            zf.write(indexer_file, "indexer.properties")
            zf.write(datastore_file, "datastore.properties")

        # Upload ZIP to create empty ImageMosaic store
        with open(zip_file, "rb") as f:
            zip_data = f.read()

        content, status = geoserver.create_imagemosaic_store_from_properties_zip(
            workspace_name=workspace,
            coveragestore_name=store_name,
            properties_zip=zip_data,
        )
        assert status == 201, f"Failed to create ImageMosaic store: {content}"
        assert content == ""

    # Step 3: Harvest granules from directory
    harvest_path = "/opt/geoserver_data/sampledata/ne/pyramid/"

    content, status = geoserver.harvest_granules_to_coverage_store(
        workspace_name=workspace,
        coveragestore_name=store_name,
        directory_path=harvest_path,
    )
    assert status in [
        201,
        202,
    ], f"Failed to harvest directory {harvest_path}: {content}"
    assert content == ""

    # Step 4: List available coverages
    content, code = geoserver.get_coverages(workspace, store_name)
    assert code == 200, f"Failed to list coverages: {content}"

    # Verify coverage name is available
    assert content[0].get("name") == coverage_name

    # Step 5: Create layer/coverage
    content, status = geoserver.create_coverage(
        workspace_name=workspace,
        coveragestore_name=store_name,
        coverage_name=coverage_name,
        title="Directory Harvest Test Coverage",
    )
    assert status == 201, f"Failed to create coverage: {content}"
    assert content == coverage_name

    # Step 6: Verify the coverage was created successfully
    coverage_data, status = geoserver.get_coverage(workspace, store_name, coverage_name)
    assert status == 200
    assert coverage_data.get("name") == coverage_name
    assert coverage_data.get("nativeName") == coverage_name
    assert coverage_data.get("enabled") == True
    assert coverage_data.get("title") == "Directory Harvest Test Coverage"

    # Step 7: Test WMS GetMap request
    wms_response = geoserver.get_map(
        layers=[f"{workspace}:{coverage_name}"],
        bbox=(-180, -90, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/png",
    )._response
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")


def test_create_imagemosaic_empty_store_with_single_file_harvest(geoserver_factory):
    """
    Test creating an empty ImageMosaic store first, then harvesting a single file.
    This tests the workflow: create store -> harvest single file -> create layer.
    """
    workspace = "single_file_harvest"
    store_name = "single_file_harvest_store"
    coverage_name = "single_file_harvest_coverage"
    geoserver = geoserver_factory(workspace)

    # Step 2: Create ImageMosaic store
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)

        # Create indexer.properties for single file
        indexer_content = f"""MosaicCRS=EPSG\\:4326
Name={coverage_name}
PropertyCollectors=CRSExtractorSPI(crs),ResolutionExtractorSPI(resolution)
Schema=*the_geom:Polygon,location:String,crs:String,resolution:String
CanBeEmpty=true
AbsolutePath=true"""

        indexer_file = tmp_path / "indexer.properties"
        indexer_file.write_text(indexer_content)

        # Create datastore.properties (using JNDI)
        datastore_content = """SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
# JNDI data source
jndiReferenceName=java:comp/env/jdbc/postgis

#Boolean
# perform only primary filter on bbox
# Default Boolean.TRUE
Loose\\ bbox=true

#Boolean
# use prepared statements
#Default Boolean.FALSE
preparedStatements=false
"""
        datastore_file = tmp_path / "datastore.properties"
        datastore_file.write_text(datastore_content)

        # Create ZIP file with both files
        zip_file = tmp_path / "mosaic-single-config.zip"
        with zipfile.ZipFile(zip_file, "w") as zf:
            zf.write(indexer_file, "indexer.properties")
            zf.write(datastore_file, "datastore.properties")

        # Upload ZIP to create ImageMosaic store
        with open(zip_file, "rb") as f:
            zip_data = f.read()

        content, status = geoserver.create_imagemosaic_store_from_properties_zip(
            workspace_name=workspace,
            coveragestore_name=store_name,
            properties_zip=zip_data,
        )
        assert status == 201, f"Failed to create ImageMosaic store: {content}"
        assert content == ""

    # Step 3: Harvest single file
    single_file_path = "/opt/geoserver_data/sampledata/ne/NE1_LR_LC_SR_W_DR.tif"

    content, status = geoserver.harvest_granules_to_coverage_store(
        workspace_name=workspace,
        coveragestore_name=store_name,
        directory_path=single_file_path,
    )
    assert status in [201, 202], f"Failed to harvest file {single_file_path}: {content}"
    assert content == ""

    # Step 4: List and create layer
    content, code = geoserver.get_coverages(workspace, store_name)
    assert code == 200
    assert content[0].get("name") == coverage_name

    # Create layer/coverage
    content, status = geoserver.create_coverage(
        workspace_name=workspace,
        coveragestore_name=store_name,
        coverage_name=coverage_name,
        title="Single File Harvest Test Coverage",
    )
    assert status == 201, f"Failed to create coverage: {content}"
    assert content == coverage_name

    content, status = geoserver.get_coverage(workspace, store_name, coverage_name)
    assert status == 200
    assert content.get("name") == coverage_name
    assert content.get("nativeName") == coverage_name
    assert content.get("enabled") == True
    assert content.get("title") == "Single File Harvest Test Coverage"

    # Verify WMS works
    wms_response = geoserver.get_map(
        layers=[f"{workspace}:{coverage_name}"],
        bbox=(-180, -90, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/png",
    )._response
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")


def test_create_imagemosaic_via_store_definition(geoserver_factory):
    """
    Test creating an ImageMosaic store via JSON store definition (not file upload).
    This tests direct store creation pointing to a directory.
    """
    workspace = "json_store_creation"
    store_name = "json_store_creation_store"
    geoserver = geoserver_factory(workspace)

    # Step 2: Create ImageMosaic store via JSON store definition
    content, status = geoserver.create_coverage_store(
        workspace_name=workspace,
        coveragestore_name=store_name,
        url="/opt/geoserver_data/sampledata/ne/pyramid/",
    )
    assert status == 201
    assert content == store_name

    # Step 3: List available coverages
    content, status = geoserver.get_coverages(workspace, store_name)
    assert status == 200
    assert len(content) == 1

    coverage_name = content[0].get("name")

    # Create layer
    content, status = geoserver.create_coverage(
        workspace_name=workspace,
        coveragestore_name=store_name,
        coverage_name=coverage_name,
        title="JSON Store Creation Test Coverage",
    )
    assert status == 201
    assert content == coverage_name

    content, status = geoserver.get_coverage(workspace, store_name, coverage_name)
    assert status == 200
    assert content.get("name") == coverage_name
    assert content.get("nativeName") == coverage_name
    assert content.get("enabled") == True
    assert content.get("title") == "JSON Store Creation Test Coverage"

    # Verify WMS works
    wms_response = geoserver.get_map(
        layers=[f"{workspace}:{coverage_name}"],
        bbox=(-180, -90, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/png",
    )._response
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")
