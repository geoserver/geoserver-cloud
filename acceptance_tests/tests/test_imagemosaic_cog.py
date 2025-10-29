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

        response = geoserver.rest_service.rest_client.put(
            f"/rest/workspaces/{workspace}/coveragestores/{coverage}/file.imagemosaic?configure=none",
            data=zip_data,
            headers={"Content-Type": "application/zip"},
        )
        assert response.status_code == 201

        # Add granules
        for uri in granules:
            response = geoserver.rest_service.rest_client.post(
                f"/rest/workspaces/{workspace}/coveragestores/{coverage}/remote.imagemosaic",
                data=uri,
                headers={"Content-Type": "text/plain"},
            )
            # Accept both 202 (Accepted) and 201 (Created) as valid responses
            assert response.status_code in [201, 202]

        # Initialize the store (list available coverages)
        response = geoserver.rest_service.rest_client.get(
            f"/rest/workspaces/{workspace}/coveragestores/{coverage}/coverages.xml?list=all"
        )
        assert response.status_code == 200

        # Verify coverage name in response
        response_text = response.text
        assert f"<coverageName>{coverage}</coverageName>" in response_text

        # Configure the coverage
        coverage_xml = f"""<coverage>
    <name>{coverage}</name>
    <title>{title}</title>
    <nativeName>{coverage}</nativeName>
    <enabled>true</enabled>
</coverage>"""

        response = geoserver.rest_service.rest_client.post(
            f"/rest/workspaces/{workspace}/coveragestores/{coverage}/coverages",
            data=coverage_xml,
            headers={"Content-Type": "text/xml"},
        )
        assert response.status_code == 201

        # Verify the coverage was created
        response = geoserver.rest_service.rest_client.get(
            f"/rest/workspaces/{workspace}/coveragestores/{coverage}/coverages/{coverage}.json"
        )
        assert response.status_code == 200

        # Verify coverage properties
        coverage_data = response.json()["coverage"]
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
