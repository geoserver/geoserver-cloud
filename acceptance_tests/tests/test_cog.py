def test_create_cog_coverage(geoserver_factory):
    """Test creating a COG coverage store and coverage"""
    workspace = "cog"
    store_name = "land_shallow_topo_21600_NW_cog"
    coverage_name = "land_shallow_topo_NW"
    geoserver = geoserver_factory(workspace)

    # Create COG coverage store
    content, status = geoserver.create_coverage_store(
        workspace_name=workspace,
        coveragestore_name=store_name,
        type="GeoTIFF",
        url=f"cog://https://test-data-cog-public.s3.amazonaws.com/public/land_shallow_topo_21600_NW_cog.tif",
        metadata={"cogSettings": {"rangeReaderSettings": "HTTP"}},
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
        bbox=(-180, -90, 180, 90),
        size=(256, 256),
        srs="EPSG:4326",
        format="image/jpeg",
    )._response
    assert wms_response.status_code == 200
    assert wms_response.headers.get("content-type").startswith("image/jpeg")
