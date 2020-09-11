/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.test;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.Converters;
import org.junit.rules.ExternalResource;

/**
 * Junit {@code @Rule} to provide or populate a catalog; use {@link CatalogTestData#empty
 * CatalogTestData.empty(Supplier<Catalog>)} to start up with an empty catalog but having the test
 * data {@link #createObjects() ready to be used}, or {@link CatalogTestData#initialized
 * CatalogTestData.initialized(Supplier<Catalog>)} to pre-populate the catalog with the {@link
 * #createObjects() test objects} before running the tests.
 */
public class CatalogTestData extends ExternalResource {

    private Supplier<Catalog> catalog;
    private boolean initialize;

    private CatalogTestData(Supplier<Catalog> catalog, boolean initialize) {
        this.catalog = catalog;
        this.initialize = initialize;
    }

    public static CatalogTestData empty(Supplier<Catalog> catalog) {
        return new CatalogTestData(catalog, false);
    }

    public static CatalogTestData initialized(Supplier<Catalog> catalog) {
        return new CatalogTestData(catalog, true);
    }

    protected @Override void before() throws Throwable {
        createObjects();
        if (initialize) {
            deleteAll();
            addObjects();
        }
    }

    protected @Override void after() {
        if (initialize) {
            deleteAll();
        }
    }

    private void deleteAll() {
        CascadeDeleteVisitor deleteVisitor = new CascadeDeleteVisitor(catalog.get());
        workspaceA.accept(deleteVisitor);
        workspaceB.accept(deleteVisitor);
        workspaceC.accept(deleteVisitor);
        style1.accept(deleteVisitor);
        style2.accept(deleteVisitor);
        layerGroup1.accept(deleteVisitor);
    }

    public WorkspaceInfo workspaceA;
    public WorkspaceInfo workspaceB;
    public WorkspaceInfo workspaceC;

    public NamespaceInfo namespaceA;
    public NamespaceInfo namespaceB;
    public NamespaceInfo namespaceC;

    public DataStoreInfo dataStoreA;
    public DataStoreInfo dataStoreB;
    public DataStoreInfo dataStoreC;

    public CoverageStoreInfo coverageStoreA;
    public WMSStoreInfo wmsStoreA;
    public WMTSStoreInfo wmtsStoreA;
    public FeatureTypeInfo featureTypeA;
    public CoverageInfo coverageA;
    public WMSLayerInfo wmsLayerA;
    public WMTSLayerInfo wmtsLayerA;
    public LayerInfo layerFeatureTypeA;
    public StyleInfo style1;
    public StyleInfo style2;
    public LayerGroupInfo layerGroup1;

    public CatalogTestData addObjects() {
        Catalog catalog = this.catalog.get();
        catalog.add(workspaceA);
        catalog.add(workspaceB);
        catalog.add(workspaceC);

        catalog.add(namespaceA);
        catalog.add(namespaceB);
        catalog.add(namespaceC);

        catalog.add(dataStoreA);
        catalog.add(dataStoreB);
        catalog.add(dataStoreC);

        catalog.add(coverageStoreA);
        catalog.add(wmsStoreA);
        catalog.add(wmtsStoreA);
        catalog.add(featureTypeA);
        catalog.add(coverageA);
        catalog.add(wmsLayerA);
        catalog.add(wmtsLayerA);
        catalog.add(style1);
        catalog.add(style2);
        catalog.add(layerFeatureTypeA);
        catalog.add(layerGroup1);
        return this;
    }

    public CatalogTestData createObjects() throws Exception {
        namespaceA = createNamespace("ns1", "wsName", "nsURI");
        namespaceB = createNamespace("ns2", "aaa", "nsURIaaa");
        namespaceC = createNamespace("ns3", "bbb", "nsURIbbb");
        workspaceA = createWorkspace("ws1", "wsName");
        workspaceB = createWorkspace("ws2", "aaa");
        workspaceC = createWorkspace("ws3", "bbb");

        dataStoreA = createDataStore("ds1", workspaceA, "dsName", "dsDescription", true);
        dataStoreB = createDataStore("ds2", workspaceB, "dsNameA", "dsDescription", true);
        dataStoreC = createDataStore("ds3", workspaceC, "dsNameB", "dsDescription", true);

        featureTypeA =
                createFeatureType(
                        "ft1",
                        dataStoreA,
                        namespaceA,
                        "ftName",
                        "ftAbstract",
                        "ftDescription",
                        true);

        coverageStoreA =
                createCoverageStore("cs1", workspaceA, "csName", "fakeCoverageType", "file://fake");
        coverageA = createCoverage("cov1", coverageStoreA, "cvName");

        wmsStoreA = createWebMapServer("wms1", workspaceA, "wmsName", "http://fake.url", true);

        wmsLayerA = createWMSLayer("wmsl-1", wmsStoreA, namespaceA, "wmsLayer1", true);

        wmtsStoreA =
                createWebMapTileServer(
                        "wmts1", workspaceA, "wmtsName", "http://fake.wmts.url", true);

        wmtsLayerA = createWMTSLayer("wmtsl1", wmtsStoreA, namespaceA, "wmtsLayer", true);

        style1 = createStyle("style1", null, "styleName", "styleFilename");

        style2 = createStyle("style2", null, "style2", "style2.sld");

        layerFeatureTypeA = createLayer("layer1", featureTypeA, "Layer1", true, style1);

        layerGroup1 = createLayerGroup("lg1", null, "layerGroup", layerFeatureTypeA, style1);

        return this;
    }

    public LayerGroupInfo createLayerGroup(
            String id, WorkspaceInfo workspace, String name, PublishedInfo layer, StyleInfo style) {
        // not using factory cause SecuredCatalog would return SecuredLayerGroupInfo which has no id
        // setter
        LayerGroupInfo lg = new LayerGroupInfoImpl();
        OwsUtils.set(lg, "id", id);
        lg.setName(name);
        lg.setWorkspace(workspace);
        lg.getLayers().add(layer);
        lg.getStyles().add(style);
        return lg;
    }

    public LayerInfo createLayer(
            String id,
            ResourceInfo resource,
            String title,
            boolean enabled,
            StyleInfo defaultStyle,
            StyleInfo... additionalStyles) {
        LayerInfo lyr = catalog.get().getFactory().createLayer();
        OwsUtils.set(lyr, "id", id);
        lyr.setResource(resource);
        lyr.setEnabled(enabled);
        lyr.setDefaultStyle(defaultStyle);
        lyr.setTitle(title);
        for (int i = 0; null != additionalStyles && i < additionalStyles.length; i++) {
            lyr.getStyles().add(additionalStyles[i]);
        }
        return lyr;
    }

    public StyleInfo createStyle(String name) {
        return createStyle(name + "-id", null, name, name + ".sld");
    }

    public StyleInfo createStyle(String id, WorkspaceInfo workspace, String name, String fileName) {
        StyleInfo st = catalog.get().getFactory().createStyle();
        OwsUtils.set(st, "id", id);
        st.setWorkspace(workspace);
        st.setName(name);
        st.setFilename(fileName);
        return st;
    }

    public WMTSLayerInfo createWMTSLayer(
            String id, StoreInfo store, NamespaceInfo namespace, String name, boolean enabled) {
        WMTSLayerInfo wmtsl = catalog.get().getFactory().createWMTSLayer();
        OwsUtils.set(wmtsl, "id", id);
        wmtsl.setStore(store);
        wmtsl.setNamespace(namespace);
        wmtsl.setName(name);
        wmtsl.setEnabled(enabled);
        return wmtsl;
    }

    public WMTSStoreInfo createWebMapTileServer(
            String id, WorkspaceInfo workspace, String name, String url, boolean enabled) {
        WMTSStoreInfo wmtss = catalog.get().getFactory().createWebMapTileServer();
        OwsUtils.set(wmtss, "id", id);
        wmtss.setWorkspace(workspace);
        wmtss.setName(name);
        wmtss.setType("WMTS");
        wmtss.setCapabilitiesURL(url);
        wmtss.setEnabled(enabled);
        return wmtss;
    }

    public WMSLayerInfo createWMSLayer(
            String id, StoreInfo store, NamespaceInfo namespace, String name, boolean enabled) {
        WMSLayerInfo wmsl = catalog.get().getFactory().createWMSLayer();
        OwsUtils.set(wmsl, "id", id);
        wmsl.setStore(store);
        wmsl.setNamespace(namespace);
        wmsl.setName(name);
        wmsl.setEnabled(enabled);
        return wmsl;
    }

    public WMSStoreInfo createWebMapServer(
            String id, WorkspaceInfo wspace, String name, String url, boolean enabled) {
        WMSStoreInfo wms = catalog.get().getFactory().createWebMapServer();
        OwsUtils.set(wms, "id", id);
        wms.setName(name);
        wms.setType("WMS");
        wms.setCapabilitiesURL(url);
        wms.setWorkspace(wspace);
        wms.setEnabled(enabled);
        return wms;
    }

    public CoverageInfo createCoverage(String id, CoverageStoreInfo cstore, String name) {
        CoverageInfo coverage = catalog.get().getFactory().createCoverage();
        OwsUtils.set(coverage, "id", id);
        coverage.setName(name);
        coverage.setStore(cstore);
        return coverage;
    }

    public CoverageStoreInfo createCoverageStore(
            String id, WorkspaceInfo ws, String name, String coverageType, String uri) {
        CoverageStoreInfo cstore = catalog.get().getFactory().createCoverageStore();
        OwsUtils.set(cstore, "id", id);
        cstore.setName(name);
        cstore.setType(coverageType);
        cstore.setURL(uri);
        cstore.setWorkspace(ws);
        return cstore;
    }

    public FeatureTypeInfo createFeatureType(
            String id,
            DataStoreInfo ds,
            NamespaceInfo ns,
            String name,
            String ftAbstract,
            String ftDescription,
            boolean enabled) {
        FeatureTypeInfo fttype = catalog.get().getFactory().createFeatureType();
        OwsUtils.set(fttype, "id", id);
        fttype.setEnabled(true);
        fttype.setName(name);
        fttype.setAbstract(ftAbstract);
        fttype.setDescription(ftDescription);
        fttype.setStore(ds);
        fttype.setNamespace(ns);
        return fttype;
    }

    public void assertEqualsLenientConnectionParameters(Info info1, Info info2) {
        if (info1 != null && info2 != null) {
            if (info1 instanceof DataStoreInfo) {
                DataStoreInfo ds1 = (DataStoreInfo) info1;
                DataStoreInfo ds2 = (DataStoreInfo) info2;
                Map<String, Serializable> p1 = new HashMap<>(ds1.getConnectionParameters());
                Map<String, Serializable> p2 = new HashMap<>(ds2.getConnectionParameters());
                p1.forEach(
                        (k, v) ->
                                ds1.getConnectionParameters()
                                        .put(k, Converters.convert(v, String.class)));
                p2.forEach(
                        (k, v) ->
                                ds2.getConnectionParameters()
                                        .put(k, Converters.convert(v, String.class)));
                assertEquals(ds1.getConnectionParameters(), ds2.getConnectionParameters());
            }
        }
        assertEquals(info1, info2);
    }

    public DataStoreInfo createDataStore(
            String id, WorkspaceInfo ws, String name, String description, boolean enabled) {
        DataStoreInfoImpl dstore = (DataStoreInfoImpl) catalog.get().getFactory().createDataStore();
        OwsUtils.set(dstore, "id", id);
        dstore.setEnabled(enabled);
        dstore.setName(name);
        dstore.setDescription(description);
        dstore.setWorkspace(ws);
        dstore.setConnectionParameters(new HashMap<>());
        dstore.getConnectionParameters().put("param1", "test value");
        dstore.getConnectionParameters().put("param2", Integer.valueOf(1000));
        return dstore;
    }

    public WorkspaceInfo createWorkspace(String name) {
        return createWorkspace(name + "-id", name);
    }

    public WorkspaceInfo createWorkspace(String id, String name) {
        WorkspaceInfo workspace = catalog.get().getFactory().createWorkspace();
        OwsUtils.set(workspace, "id", id);
        workspace.setName(name);
        return workspace;
    }

    public NamespaceInfo createNamespace(String name, String uri) {
        return createNamespace(name + "-id", name, uri);
    }

    public NamespaceInfo createNamespace(String id, String name, String uri) {
        Catalog cat = catalog.get();
        CatalogFactory factory = cat.getFactory();
        NamespaceInfo namesapce = factory.createNamespace();
        OwsUtils.set(namesapce, "id", id);
        namesapce.setPrefix(name);
        namesapce.setURI(uri);
        return namesapce;
    }
}
