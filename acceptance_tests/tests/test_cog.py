WORKSPACE = "cog"


def test_create_cog_coverage(geoserver_factory):
    """Test creating a COG coverage store and coverage"""
    workspace = "cog"
    store_name = "land_shallow_topo_21600_NW_cog"
    coverage_name = "land_shallow_topo_NW"
    geoserver = geoserver_factory(workspace)

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

    rest_client = geoserver.rest_service.rest_client
    endpoints = geoserver.rest_service.rest_endpoints

    response = rest_client.post(
        endpoints.coveragestores(workspace),
        data=store_xml,
        headers={"Content-Type": "application/xml"},
    )
    assert response.status_code == 201

    # Create coverage
    coverage_xml = f"""<coverage>
        <name>{coverage_name}</name>
        <nativeName>{store_name}</nativeName>
    </coverage>"""

    response = rest_client.post(
        endpoints.coverages(workspace, store_name),
        data=coverage_xml,
        headers={"Content-Type": "application/xml"},
    )
    assert response.status_code == 201

    # Verify the coverage was created - try listing coverages first
    list_response = rest_client.get(endpoints.coverages(workspace, store_name))
    assert (
        list_response.status_code == 200
    ), f"Failed to get coverages: {list_response.status_code} - {list_response.text}"

    # Check specific coverage
    response = rest_client.get(endpoints.coverage(workspace, store_name, coverage_name))
    assert response.status_code == 200

    # Verify coverage properties
    coverage_data = response.json()["coverage"]
    assert coverage_data["name"] == coverage_name
    assert coverage_data["nativeName"] == coverage_name
    assert coverage_data["enabled"] == True

    # Test WMS GetMap request
    wms_response = rest_client.get(
        f"/wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&LAYERS={workspace}:{coverage_name}&STYLES=&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&FORMAT=image/jpeg&SRS=EPSG:4326"
    )
    assert wms_response.status_code == 200
    assert wms_response.headers.get("content-type").startswith("image/jpeg")
