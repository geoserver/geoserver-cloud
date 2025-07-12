import pytest
from geoservercloud import GeoServerCloud
from conftest import GEOSERVER_URL


def test_create_cog_coverage():
    """Test creating a COG coverage store and coverage"""
    geoserver = GeoServerCloud(GEOSERVER_URL)
    workspace = "cog"
    store_name = "land_shallow_topo_21600_NW_cog"
    coverage_name = "land_shallow_topo_NW"

    # Delete and recreate workspace
    geoserver.delete_workspace(workspace)
    response = geoserver.create_workspace(workspace)
    assert response.status_code == 201

    # Create COG coverage store
    store_xml = f"""<coverageStore>
    <name>{store_name}</name>
    <type>GeoTIFF</type>
    <enabled>true</enabled>
    <workspace><name>{workspace}</name></workspace>
    <url>cog://https://test-data-cog-public.s3.amazonaws.com/public/land_shallow_topo_21600_NW_cog.tif</url>
    <metadata>
        <entry key="CogSettings.Key">
            <cogSettings>
                <rangeReaderSettings>HTTP</rangeReaderSettings>
            </cogSettings>
        </entry>
    </metadata>
</coverageStore>"""

    response = geoserver.post_request(
        f"/rest/workspaces/{workspace}/coveragestores",
        data=store_xml,
        headers={"Content-Type": "application/xml"}
    )
    assert response.status_code == 201

    # Create coverage
    coverage_xml = f"""<coverage>
        <name>{coverage_name}</name>
        <nativeName>{store_name}</nativeName>
    </coverage>"""

    response = geoserver.post_request(
        f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages",
        data=coverage_xml,
        headers={"Content-Type": "application/xml"}
    )
    assert response.status_code == 201

    # Verify the coverage was created - try listing coverages first
    list_response = geoserver.get_request(f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages.json")
    if list_response.status_code != 200:
        print(f"Coverage listing failed: {list_response.status_code} - {list_response.text}")
    assert list_response.status_code == 200

    # Check specific coverage
    response = geoserver.get_request(f"/rest/workspaces/{workspace}/coveragestores/{store_name}/coverages/{coverage_name}.json")
    assert response.status_code == 200

    # Verify coverage properties
    coverage_data = response.json()["coverage"]
    assert coverage_data["name"] == coverage_name
    assert coverage_data["nativeName"] == coverage_name
    assert coverage_data["enabled"] == True

    # Test WMS GetMap request
    wms_response = geoserver.get_request(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/jpeg&SRS=EPSG:4326"
    )
    assert wms_response.status_code == 200
    assert wms_response.headers.get("content-type").startswith("image/jpeg")

    # Cleanup
    geoserver.delete_workspace(workspace)
