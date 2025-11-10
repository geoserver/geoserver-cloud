import tempfile
import zipfile
from pathlib import Path
import pytest


def _create_imagemosaic(
    geoserver,
    workspace,
    coverage,
    granules,
    indexer_content,
    title="ImageMosaic Coverage",
):
    """Helper function to create an ImageMosaic with COG granules"""

    # Create temporary directory for mosaic files
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)

        # Create indexer.properties
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

        # Create zip file
        zip_file = tmp_path / f"{coverage}.zip"
        with zipfile.ZipFile(zip_file, "w") as zf:
            zf.write(indexer_file, "indexer.properties")
            zf.write(datastore_file, "datastore.properties")

            # Create timeregex.properties if needed for time-based PropertyCollector
            if "timeregex" in indexer_content:
                # Regex pattern to extract date from MODIS filename format: 2018.01.01
                timeregex_content = "regex=(?<=\\.)([0-9]{4}\\.[0-9]{2}\\.[0-9]{2})(?=\\.),format=yyyy.MM.dd"
                timeregex_file = tmp_path / "timeregex.properties"
                timeregex_file.write_text(timeregex_content)
                zf.write(timeregex_file, "timeregex.properties")

        # Create empty imagemosaic
        with open(zip_file, "rb") as f:
            zip_data = f.read()

        content, status = geoserver.create_imagemosaic_store_from_properties_zip(
            workspace_name=workspace,
            coveragestore_name=coverage,
            properties_zip=zip_data,
        )
        assert status == 201, f"Failed to create ImageMosaic store: {content}"
        assert content == ""

        # Add granules
        for uri in granules:
            content, status = geoserver.publish_granule_to_coverage_store(
                workspace_name=workspace,
                coveragestore_name=coverage,
                method="remote",
                granule_path=uri,
            )
            assert status in [
                201,
                202,
            ], f"Failed to publish granule {uri}: {content}"

        # Initialize the store (list available coverages)
        content, status = geoserver.get_coverages(
            workspace_name=workspace, coveragestore_name=coverage
        )
        assert status == 200
        # Verify coverage name in response
        assert content[0].get("name") == coverage

        # Configure the coverage
        content, status = geoserver.create_coverage(
            workspace_name=workspace,
            coveragestore_name=coverage,
            coverage_name=coverage,
            title=title,
        )
        assert status == 201, f"Failed to create coverage: {content}"
        assert content == coverage

        # Verify the coverage was created
        coverage_data, code = geoserver.get_coverage(workspace, coverage, coverage)
        assert code == 200

        # Verify coverage properties
        assert coverage_data["name"] == coverage
        assert coverage_data["nativeName"] == coverage
        assert coverage_data["enabled"] == True
        assert coverage_data["title"] == title

        # Test WMS GetMap request
        wms_response = geoserver.rest_service.rest_client.get(
            f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage}&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326"
        )
        assert wms_response.status_code == 200
        assert wms_response.headers.get("content-type").startswith("image/png")

        return coverage_data


def test_create_imagemosaic_landshallow_topo(geoserver_factory):
    """Test creating an ImageMosaic coverage store with multiple COG granules"""
    workspace = "s3cog_public"
    coverage = "land_shallow_topo_http"
    geoserver = geoserver_factory(workspace)

    # HTTP granules
    granules = [
        "https://test-data-cog-public.s3.amazonaws.com/public/land_shallow_topo_21600_NE_cog.tif",
        "https://test-data-cog-public.s3.amazonaws.com/public/land_shallow_topo_21600_NW_cog.tif",
        "https://test-data-cog-public.s3.amazonaws.com/public/land_shallow_topo_21600_SE_cog.tif",
        "https://test-data-cog-public.s3.amazonaws.com/public/land_shallow_topo_21600_SW_cog.tif",
    ]

    # Create indexer.properties
    indexer_content = f"""Cog=true
CogRangeReader=it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
Name={coverage}"""

    _create_imagemosaic(
        geoserver,
        workspace,
        coverage,
        granules,
        indexer_content,
        "Land Shallow Topo HTTP",
    )


@pytest.mark.skip(reason="Takes too long - enable for full testing")
def test_create_imagemosaic_modis(geoserver_factory):
    """Test creating a MODIS ImageMosaic coverage with time dimension (reproduces official tutorial)"""
    workspace = "modis_cog"
    coverage = "modisvi"
    geoserver = geoserver_factory(workspace)

    # MODIS COG datasets from NASA EarthData
    modis_granules = [
        "https://modis-vi-nasa.s3-us-west-2.amazonaws.com/MOD13A1.006/2018.01.01.tif",
        "https://modis-vi-nasa.s3-us-west-2.amazonaws.com/MOD13A1.006/2018.01.17.tif",
    ]

    # Create indexer.properties (based on MODIS tutorial)
    indexer_content = f"""Cog=true
PropertyCollectors=TimestampFileNameExtractorSPI[timeregex](time)
TimeAttribute=time
Schema=*the_geom:Polygon,location:String,time:java.util.Date
CanBeEmpty=true
Name={coverage}"""

    coverage_data = _create_imagemosaic(
        geoserver,
        workspace,
        coverage,
        modis_granules,
        indexer_content,
        "MODIS Vegetation Index",
    )

    # Additional test for time-based query (since MODIS has time dimension)
    time_wms_response = geoserver.rest_service.rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage}&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/png&SRS=EPSG:4326&TIME=2018-01-01"
    )
    assert time_wms_response.status_code == 200
    assert time_wms_response.headers.get("content-type").startswith("image/png")
