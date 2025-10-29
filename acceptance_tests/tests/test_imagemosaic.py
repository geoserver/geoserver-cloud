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
    geoserver = geoserver_factory(workspace)

    # Use direct directory approach (like web UI) instead of individual file URLs
    directory_path = "/opt/geoserver_data/sampledata/ne/pyramid/"

    # Create ImageMosaic store directly from directory
    response = geoserver.rest_service.rest_client.put(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/external.imagemosaic",
        data=directory_path,
        headers={"Content-Type": "text/plain"},
    )
    assert response.status_code in [
        201,
        202,
    ], f"Failed to create ImageMosaic from directory: {response.text}"

    # List available coverages (should be auto-discovered)
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages.xml?list=all"
    )
    assert response.status_code == 200, f"Failed to list coverages: {response.text}"

    # Extract the auto-discovered coverage name
    response_text = response.text
    import re

    coverage_match = re.search(r"<coverageName>([^<]+)</coverageName>", response_text)
    assert coverage_match, f"No coverage found in response: {response_text}"

    coverage_name = coverage_match.group(1)

    # Check if coverage was auto-created (likely scenario)
    coverage_response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages/{coverage_name}.json"
    )

    if coverage_response.status_code == 200:
        # Coverage was auto-created - this is the normal case
        coverage_data = coverage_response.json()["coverage"]
        assert coverage_data["name"] == coverage_name
        assert coverage_data["nativeName"] == coverage_name
        assert coverage_data["enabled"] == True
    else:
        # Coverage not auto-created, create it manually
        coverage_xml = f"""<coverage>
    <name>{coverage_name}</name>
    <title>Natural Earth Pyramid Mosaic</title>
    <nativeName>{coverage_name}</nativeName>
    <enabled>true</enabled>
</coverage>"""

        response = geoserver.rest_service.rest_client.post(
            f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages",
            data=coverage_xml,
            headers={"Content-Type": "text/xml"},
        )
        assert (
            response.status_code == 201
        ), f"Failed to create coverage: {response.text}"

        # Verify the coverage was created
        response = geoserver.rest_service.rest_client.get(
            f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages/{coverage_name}.json"
        )
        assert response.status_code == 200

        coverage_data = response.json()["coverage"]
        assert coverage_data["name"] == coverage_name
        assert coverage_data["nativeName"] == coverage_name
        assert coverage_data["enabled"] == True

    # Test WMS GetMap request (verify local file mosaic works)
    wms_response = geoserver.rest_service.rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326"
    )
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")

    # Delete coverage store
    response = geoserver.rest_service.rest_client.delete(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}?recurse=true"
    )
    assert (
        response.status_code == 200
    ), f"Failed to delete coverage store: {response.text}"


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

        response = geoserver.rest_service.rest_client.put(
            f"/rest/workspaces/{workspace}/coveragestores/{store_name}/file.imagemosaic?configure=none",
            data=zip_data,
            headers={"Content-Type": "application/zip"},
        )
        assert (
            response.status_code == 201
        ), f"Failed to create ImageMosaic store: {response.text}"

    # Manually add individual granules from the sample data
    granule_paths = [
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_1_1.tif",
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_1_2.tif",
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_2_1.tif",
        "/opt/geoserver_data/sampledata/ne/pyramid/NE1_LR_LC_SR_W_DR_2_2.tif",
    ]

    for granule_path in granule_paths:
        # Use direct file paths (without file:// protocol) for external.imagemosaic
        response = geoserver.rest_service.rest_client.post(
            f"/rest/workspaces/{workspace}/coveragestores/{store_name}/external.imagemosaic",
            data=granule_path,
            headers={"Content-Type": "text/plain"},
        )
        assert response.status_code in [
            201,
            202,
        ], f"Failed to add granule {granule_path}: {response.text}"

    # Initialize the store (list available coverages)
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages.xml?list=all"
    )
    assert response.status_code == 200, f"Failed to list coverages: {response.text}"

    # Verify coverage name is available
    response_text = response.text
    assert (
        f"<coverageName>{coverage_name}</coverageName>" in response_text
    ), f"Coverage name '{coverage_name}' not found in response: {response_text}"

    # Create layer/coverage
    coverage_xml = f"""<coverage>
    <name>{coverage_name}</name>
    <title>Manual Granules Test Coverage</title>
    <nativeName>{coverage_name}</nativeName>
    <enabled>true</enabled>
</coverage>"""

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages",
        data=coverage_xml,
        headers={"Content-Type": "text/xml"},
    )
    assert response.status_code == 201, f"Failed to create coverage: {response.text}"

    # Verify the coverage was created successfully
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages/{coverage_name}.json"
    )
    assert (
        response.status_code == 200
    ), f"Failed to get coverage details: {response.text}"

    coverage_data = response.json()["coverage"]
    assert coverage_data["name"] == coverage_name
    assert coverage_data["nativeName"] == coverage_name
    assert coverage_data["enabled"] == True

    # Test WMS GetMap request (verify manual granule addition works)
    wms_response = geoserver.rest_service.rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326"
    )
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

        response = geoserver.rest_service.rest_client.put(
            f"/rest/workspaces/{workspace}/coveragestores/{store_name}/file.imagemosaic?configure=none",
            data=zip_data,
            headers={"Content-Type": "application/zip"},
        )
        assert (
            response.status_code == 201
        ), f"Failed to create ImageMosaic store: {response.text}"

    # Step 3: Harvest granules from directory
    harvest_path = "/opt/geoserver_data/sampledata/ne/pyramid/"

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/external.imagemosaic",
        data=harvest_path,
        headers={"Content-Type": "text/plain"},
    )
    assert response.status_code in [
        201,
        202,
    ], f"Failed to harvest directory {harvest_path}: {response.text}"

    # Step 4: List available coverages
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages.xml?list=all"
    )
    assert response.status_code == 200, f"Failed to list coverages: {response.text}"

    # Verify coverage name is available
    response_text = response.text
    assert (
        f"<coverageName>{coverage_name}</coverageName>" in response_text
    ), f"Coverage name '{coverage_name}' not found in response: {response_text}"

    # Step 5: Create layer/coverage
    coverage_xml = f"""<coverage>
    <name>{coverage_name}</name>
    <title>Directory Harvest Test Coverage</title>
    <nativeName>{coverage_name}</nativeName>
    <enabled>true</enabled>
</coverage>"""

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages",
        data=coverage_xml,
        headers={"Content-Type": "text/xml"},
    )
    assert response.status_code == 201, f"Layer creation failed: {response.text}"

    # Step 6: Verify the coverage was created successfully
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages/{coverage_name}.json"
    )
    assert (
        response.status_code == 200
    ), f"Failed to get coverage details: {response.text}"

    coverage_data = response.json()["coverage"]
    assert coverage_data["name"] == coverage_name
    assert coverage_data["nativeName"] == coverage_name
    assert coverage_data["enabled"] == True

    # Step 7: Test WMS GetMap request
    wms_response = geoserver.rest_service.rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}"
        f"&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326"
    )
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

        response = geoserver.rest_service.rest_client.put(
            f"/rest/workspaces/{workspace}/coveragestores/{store_name}/file.imagemosaic?configure=none",
            data=zip_data,
            headers={"Content-Type": "application/zip"},
        )
        assert (
            response.status_code == 201
        ), f"Failed to create ImageMosaic store: {response.text}"

    # Step 3: Harvest single file
    single_file_path = "/opt/geoserver_data/sampledata/ne/NE1_LR_LC_SR_W_DR.tif"

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/external.imagemosaic",
        data=single_file_path,
        headers={"Content-Type": "text/plain"},
    )
    assert response.status_code in [
        201,
        202,
    ], f"Failed to harvest file {single_file_path}: {response.text}"

    # Step 4: List and create layer
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages.xml?list=all"
    )
    assert response.status_code == 200, f"Failed to list coverages: {response.text}"

    # Create layer/coverage
    coverage_xml = f"""<coverage>
    <name>{coverage_name}</name>
    <title>Single File Harvest Test Coverage</title>
    <nativeName>{coverage_name}</nativeName>
    <enabled>true</enabled>
</coverage>"""

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages",
        data=coverage_xml,
        headers={"Content-Type": "text/xml"},
    )
    assert response.status_code == 201, f"Layer creation failed: {response.text}"

    # Verify WMS works
    wms_response = geoserver.rest_service.rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}"
        f"&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326"
    )
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")


def test_create_imagemosaic_via_xml_store_creation(geoserver_factory):
    """
    Test creating an ImageMosaic store via XML store creation (not file upload).
    This tests direct store creation pointing to a directory.
    """
    workspace = "xml_store_creation"
    store_name = "xml_store_creation_store"
    geoserver = geoserver_factory(workspace)

    # Step 2: Create ImageMosaic store via XML store creation
    store_xml = f"""<coverageStore>
    <name>{store_name}</name>
    <workspace>
        <name>{workspace}</name>
    </workspace>
    <type>ImageMosaic</type>
    <enabled>true</enabled>
    <url>/opt/geoserver_data/sampledata/ne/pyramid/</url>
</coverageStore>"""

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores",
        data=store_xml,
        headers={"Content-Type": "text/xml"},
    )
    assert (
        response.status_code == 201
    ), f"Store creation via XML failed: {response.text}"

    # Step 3: List available coverages
    response = geoserver.rest_service.rest_client.get(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages.xml?list=all"
    )
    assert response.status_code == 200, f"Failed to list coverages: {response.text}"
    assert (
        "coverageName" in response.text
    ), f"No coverage found in response: {response.text}"

    # Extract coverage name
    import re

    coverage_match = re.search(r"<coverageName>([^<]+)</coverageName>", response.text)
    assert coverage_match, f"Could not extract coverage name from: {response.text}"
    coverage_name = coverage_match.group(1)

    # Create layer
    coverage_xml = f"""<coverage>
    <name>{coverage_name}</name>
    <title>XML Store Creation Test Coverage</title>
    <nativeName>{coverage_name}</nativeName>
    <enabled>true</enabled>
</coverage>"""

    response = geoserver.rest_service.rest_client.post(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages",
        data=coverage_xml,
        headers={"Content-Type": "text/xml"},
    )
    assert response.status_code == 201, f"Layer creation failed: {response.text}"

    # Verify WMS works
    wms_response = geoserver.rest_service.rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}"
        f"&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326"
    )
    assert wms_response.status_code == 200, f"WMS GetMap failed: {wms_response.text}"
    assert wms_response.headers.get("content-type").startswith("image/png")
