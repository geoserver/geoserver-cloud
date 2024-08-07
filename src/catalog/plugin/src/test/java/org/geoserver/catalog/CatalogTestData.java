/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.CatalogMode;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfo.ServiceLevel;
import org.geoserver.wfs.WFSInfoImpl;
import org.geoserver.wms.CacheConfiguration;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.WPSInfoImpl;
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.process.factory.AnnotationDrivenProcessFactory;
import org.geotools.util.Converters;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides or populates a catalog; use {@link CatalogTestData#empty
 * CatalogTestData.empty(Supplier<Catalog>)} to start up with an empty catalog but having the test
 * data {@link #createCatalogObjects() ready to be used}, or {@link CatalogTestData#initialized
 * CatalogTestData.initialized(Supplier<Catalog>)} to pre-populate the catalog with the {@link
 * #createCatalogObjects() test objects} before running the tests.
 */
@Accessors(fluent = true)
public class CatalogTestData {

    private Supplier<Catalog> catalog;
    private Supplier<GeoServer> configCatalog = () -> null;

    private boolean initializeCatalog;
    private boolean initializeConfig;

    private final @Getter CatalogFaker faker;

    private CatalogTestData(
            Supplier<Catalog> catalog,
            Supplier<GeoServer> config,
            boolean initCatalog,
            boolean initConfig) {
        this.catalog = catalog;
        this.configCatalog = config;
        this.initializeCatalog = initCatalog;
        this.initializeConfig = initConfig;
        this.faker = new CatalogFaker(catalog, config);
    }

    private CatalogTestData() {
        this.initializeCatalog = false;
        this.initializeConfig = false;
        CatalogPlugin cat = new CatalogPlugin();
        GeoServerImpl geoserver = new GeoServerImpl();
        geoserver.setCatalog(cat);
        this.catalog = () -> cat;
        this.configCatalog = () -> geoserver;
        this.faker = new CatalogFaker(cat, geoserver);
    }

    public static CatalogTestData empty() {
        return new CatalogTestData();
    }

    public static CatalogTestData empty(Supplier<Catalog> catalog, Supplier<GeoServer> config) {
        return new CatalogTestData(catalog, config, false, false);
    }

    public static CatalogTestData initialized(
            Supplier<Catalog> catalog, Supplier<GeoServer> config) {
        return new CatalogTestData(catalog, config, true, true);
    }

    public Catalog getCatalog() {
        return catalog.get();
    }

    public GeoServer getGeoServer() {
        return configCatalog.get();
    }

    private CatalogFactory getFactory() {
        return catalog.get().getFactory();
    }

    public CatalogTestData initCatalog(boolean addData) {
        this.initializeCatalog = addData;
        return this;
    }

    public CatalogTestData initConfig(boolean addData) {
        this.initializeConfig = addData;
        return this;
    }

    public CatalogTestData initialize() {
        initCatalog();
        initConfig();
        return this;
    }

    public CatalogTestData initCatalog() {
        createCatalogObjects();
        if (initializeCatalog) {
            deleteAll(catalog.get());
            addObjects();
        }
        return this;
    }

    public void after() {
        if (initializeCatalog) {
            deleteAll(catalog.get());
        }
        if (initializeConfig) {
            deleteAll(configCatalog.get());
        }
    }

    public void deleteAll() {
        deleteAll(configCatalog.get());
        deleteAll(catalog.get());
    }

    public void deleteAll(GeoServer gs) {
        if (gs != null) {
            gs.getServices().forEach(gs::remove);
            catalog.get()
                    .getWorkspaces()
                    .forEach(
                            ws -> {
                                SettingsInfo settings = gs.getSettings(ws);
                                if (settings != null) gs.remove(settings);
                                gs.getServices(ws).forEach(gs::remove);
                            });
        }
    }

    public void deleteAll(Catalog catalog) {
        CascadeDeleteVisitor deleteVisitor = new CascadeDeleteVisitor(catalog);
        catalog.getLayerGroups().forEach(lg -> lg.accept(deleteVisitor));
        catalog.getLayers().forEach(lg -> lg.accept(deleteVisitor));
        catalog.getResources(ResourceInfo.class).forEach(lg -> lg.accept(deleteVisitor));
        catalog.getStores(StoreInfo.class).forEach(lg -> lg.accept(deleteVisitor));
        catalog.getWorkspaces().forEach(ws -> ws.accept(deleteVisitor));
        catalog.getNamespaces().forEach(ws -> ws.accept(deleteVisitor));
        // bypass catalog's check for default style
        catalog.getStyles().forEach(catalog.getFacade()::remove);
    }

    public GeoServerInfo global;
    public LoggingInfo logging;
    public SettingsInfo workspaceASettings;
    public WMSInfo wmsService;
    public WFSInfo wfsService;
    public WCSInfo wcsService;
    public WPSInfo wpsService;

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
        Catalog cat = this.catalog.get();
        workspaceA = add(workspaceA, cat::add, cat::getWorkspace);
        workspaceB = add(workspaceB, cat::add, cat::getWorkspace);
        workspaceC = add(workspaceC, cat::add, cat::getWorkspace);

        namespaceA = add(namespaceA, cat::add, cat::getNamespace);
        namespaceB = add(namespaceB, cat::add, cat::getNamespace);
        namespaceC = add(namespaceC, cat::add, cat::getNamespace);

        dataStoreA = add(dataStoreA, cat::add, cat::getDataStore);
        dataStoreB = add(dataStoreB, cat::add, cat::getDataStore);
        dataStoreC = add(dataStoreC, cat::add, cat::getDataStore);

        coverageStoreA = add(coverageStoreA, cat::add, cat::getCoverageStore);
        wmsStoreA = add(wmsStoreA, cat::add, id -> cat.getStore(id, WMSStoreInfo.class));
        wmtsStoreA = add(wmtsStoreA, cat::add, id -> cat.getStore(id, WMTSStoreInfo.class));

        featureTypeA = add(featureTypeA, cat::add, cat::getFeatureType);
        coverageA = add(coverageA, cat::add, cat::getCoverage);
        wmsLayerA = add(wmsLayerA, cat::add, id -> cat.getResource(id, WMSLayerInfo.class));
        wmtsLayerA = add(wmtsLayerA, cat::add, id -> cat.getResource(id, WMTSLayerInfo.class));

        style1 = add(style1, cat::add, cat::getStyle);
        style2 = add(style2, cat::add, cat::getStyle);

        layerFeatureTypeA = add(layerFeatureTypeA, cat::add, cat::getLayer);
        layerGroup1 = add(layerGroup1, cat::add, cat::getLayerGroup);

        return this;
    }

    private <T extends Info> @NonNull T add(T orig, Consumer<T> adder, Function<String, T> fetch) {
        adder.accept(orig);
        return fetch.apply(orig.getId());
    }

    public CatalogTestData createCatalogObjects() {
        namespaceA = faker().namespace("ns1", "wsName", "nsURI");
        namespaceB = faker().namespace("ns2", "aaa", "nsURIaaa");
        namespaceC = faker().namespace("ns3", "bbb", "nsURIbbb");
        workspaceA = faker().workspaceInfo("ws1", "wsName");
        workspaceB = faker().workspaceInfo("ws2", "aaa");
        workspaceC = faker().workspaceInfo("ws3", "bbb");

        dataStoreA = faker().dataStoreInfo("ds1", workspaceA, "dsName", "dsDescription", true);
        dataStoreB = faker().dataStoreInfo("ds2", workspaceB, "dsNameA", "dsDescription", true);
        dataStoreC = faker().dataStoreInfo("ds3", workspaceC, "dsNameB", "dsDescription", true);

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

        style1 = createStyle("style1", null, "style1", "styleFilename");

        style2 = createStyle("style2", null, "style2", "style2.sld");

        layerFeatureTypeA = createLayer("layer1", featureTypeA, "Layer1", true, style1);

        layerGroup1 = createLayerGroup("lg1", null, "layerGroup", layerFeatureTypeA, style1);

        return this;
    }

    public CatalogTestData initConfig() {
        createConfigObjects();
        if (initializeConfig) {
            GeoServer geoServer = configCatalog.get();
            if (geoServer == null) {
                throw new IllegalStateException(
                        "No GeoServer provided, either disable config initialization or provide a GeoServer instance");
            }
            if (geoServer.getCatalog() == null) {
                throw new IllegalStateException("GeoServer.getCatalog() is null");
            }
            deleteAll(geoServer);

            geoServer.setGlobal(global);
            geoServer.setLogging(logging);
            geoServer.add(wmsService);
            geoServer.add(wfsService);
            geoServer.add(wpsService);
            geoServer.add(wcsService);

            geoServer.add(workspaceASettings);
        }
        return this;
    }

    public CatalogTestData createConfigObjects() {
        global = faker().geoServerInfo();
        logging = faker().loggingInfo();
        workspaceASettings = faker().settingsInfo(workspaceA);
        wmsService = faker().serviceInfo("wms", WMSInfoImpl::new);
        wfsService = faker().serviceInfo("wfs", WFSInfoImpl::new);
        wcsService = faker().serviceInfo("wcs", WCSInfoImpl::new);
        wpsService = faker().serviceInfo("wps", WPSInfoImpl::new);

        // ignore simple boolean properties

        wmsService.setCacheConfiguration(new CacheConfiguration());
        wmsService.setRemoteStyleMaxRequestTime(2000);
        wmsService.setRemoteStyleTimeout(500);

        wfsService.setMaxFeatures(50);
        wfsService.setServiceLevel(ServiceLevel.COMPLETE);
        wfsService.setMaxNumberOfFeaturesForPreview(10);

        wcsService.setOverviewPolicy(OverviewPolicy.QUALITY);

        wpsService.setConnectionTimeout(1000);
        wpsService.setResourceExpirationTimeout(2000);
        wpsService.setMaxSynchronousProcesses(4);
        wpsService.setMaxAsynchronousProcesses(16);
        ProcessGroupInfo pgi = new ProcessGroupInfoImpl();
        pgi.setEnabled(true);
        pgi.setFactoryClass(AnnotationDrivenProcessFactory.class);
        wpsService.getProcessGroups().add(pgi);
        wpsService.setCatalogMode(CatalogMode.CHALLENGE);
        wpsService.setMaxComplexInputSize(1024);
        wpsService.setMaxAsynchronousExecutionTime(1);
        wpsService.setMaxAsynchronousTotalTime(2);
        wpsService.setMaxSynchronousExecutionTime(3);
        wpsService.setMaxSynchronousTotalTime(4);
        return this;
    }

    public LayerGroupInfo createLayerGroup(
            String name, @Nullable WorkspaceInfo ws, PublishedInfo layer) {
        StyleInfo style = layer instanceof LayerInfo l ? l.getDefaultStyle() : null;
        return createLayerGroup(null, ws, name, layer, style);
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
        OwsUtils.resolveCollections(lg);
        return lg;
    }

    public LayerInfo createLayer(ResourceInfo resource, StyleInfo defaultStyle) {
        String title = resource.getName() + " title";
        return createLayer(null, resource, title, true, defaultStyle);
    }

    public LayerInfo createLayer(
            String id,
            ResourceInfo resource,
            String title,
            boolean enabled,
            StyleInfo defaultStyle,
            StyleInfo... additionalStyles) {
        LayerInfo lyr = getFactory().createLayer();
        OwsUtils.set(lyr, "id", id);
        lyr.setResource(resource);
        lyr.setEnabled(enabled);
        lyr.setDefaultStyle(defaultStyle);
        lyr.setTitle(title);
        for (int i = 0; null != additionalStyles && i < additionalStyles.length; i++) {
            lyr.getStyles().add(additionalStyles[i]);
        }
        OwsUtils.resolveCollections(lyr);
        return lyr;
    }

    public StyleInfo createStyle(@NonNull String name) {
        return createStyle(name, (WorkspaceInfo) null);
    }

    public StyleInfo createStyle(@NonNull String name, WorkspaceInfo workspace) {
        String id = name + "-id";
        String fileName = name + ".sld";
        return createStyle(id, workspace, name, fileName);
    }

    public StyleInfo createStyle(String id, WorkspaceInfo workspace, String name, String fileName) {
        StyleInfo st = getFactory().createStyle();
        OwsUtils.set(st, "id", id);
        st.setWorkspace(workspace);
        st.setName(name);
        st.setFilename(fileName);
        OwsUtils.resolveCollections(st);
        return st;
    }

    public WMTSLayerInfo createWMTSLayer(
            String id, StoreInfo store, NamespaceInfo namespace, String name, boolean enabled) {
        WMTSLayerInfo wmtsl = getFactory().createWMTSLayer();
        OwsUtils.set(wmtsl, "id", id);
        wmtsl.setStore(store);
        wmtsl.setNamespace(namespace);
        wmtsl.setName(name);
        wmtsl.setEnabled(enabled);
        OwsUtils.resolveCollections(wmtsl);
        return wmtsl;
    }

    public WMTSStoreInfo createWebMapTileServer(
            String id, WorkspaceInfo workspace, String name, String url, boolean enabled) {
        WMTSStoreInfo wmtss = getFactory().createWebMapTileServer();
        OwsUtils.set(wmtss, "id", id);
        wmtss.setWorkspace(workspace);
        wmtss.setName(name);
        wmtss.setType("WMTS");
        wmtss.setCapabilitiesURL(url);
        wmtss.setEnabled(enabled);
        OwsUtils.resolveCollections(wmtss);
        return wmtss;
    }

    public WMSLayerInfo createWMSLayer(
            String id, StoreInfo store, NamespaceInfo namespace, String name, boolean enabled) {
        WMSLayerInfo wmsl = getFactory().createWMSLayer();
        OwsUtils.set(wmsl, "id", id);
        wmsl.setStore(store);
        wmsl.setNamespace(namespace);
        wmsl.setName(name);
        wmsl.setEnabled(enabled);
        OwsUtils.resolveCollections(wmsl);
        return wmsl;
    }

    public WMSStoreInfo createWebMapServer(
            String id, WorkspaceInfo wspace, String name, String url, boolean enabled) {
        WMSStoreInfo wms = getFactory().createWebMapServer();
        OwsUtils.set(wms, "id", id);
        wms.setName(name);
        wms.setType("WMS");
        wms.setCapabilitiesURL(url);
        wms.setWorkspace(wspace);
        wms.setEnabled(enabled);
        OwsUtils.resolveCollections(wms);
        return wms;
    }

    public CoverageInfo createCoverage(String name) {
        String id = name + "-id";
        return createCoverage(id, coverageStoreA, name);
    }

    public CoverageInfo createCoverage(String id, CoverageStoreInfo cstore, String name) {
        CoverageInfo coverage = getFactory().createCoverage();
        OwsUtils.set(coverage, "id", id);
        coverage.setName(name);
        coverage.setStore(cstore);
        OwsUtils.resolveCollections(coverage);
        return coverage;
    }

    public CoverageStoreInfo createCoverageStore(
            String id, WorkspaceInfo ws, String name, String coverageType, String uri) {
        CoverageStoreInfo cstore = getFactory().createCoverageStore();
        OwsUtils.set(cstore, "id", id);
        cstore.setName(name);
        cstore.setType(coverageType);
        cstore.setURL(uri);
        cstore.setWorkspace(ws);
        OwsUtils.resolveCollections(cstore);
        return cstore;
    }

    public FeatureTypeInfo createFeatureType(String name) {
        String id = name + "-id";
        String ftAbstract = name + " abstract";
        String ftDescription = name + " description";
        return createFeatureType(id, dataStoreA, namespaceA, name, ftAbstract, ftDescription, true);
    }

    public FeatureTypeInfo createFeatureType(
            String id,
            DataStoreInfo ds,
            NamespaceInfo ns,
            String name,
            String ftAbstract,
            String ftDescription,
            boolean enabled) {
        FeatureTypeInfo fttype = getFactory().createFeatureType();
        OwsUtils.set(fttype, "id", id);
        fttype.setEnabled(enabled);
        fttype.setName(name);
        fttype.setAbstract(ftAbstract);
        fttype.setDescription(ftDescription);
        fttype.setStore(ds);
        fttype.setNamespace(ns);
        OwsUtils.resolveCollections(fttype);
        return fttype;
    }

    public void assertEqualsLenientConnectionParameters(Info info1, Info info2) {
        if (info1 != null && info2 != null && info1 instanceof DataStoreInfo ds1) {
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
        assertEquals(info1, info2);
    }

    public void assertInternationalStringPropertiesEqual(Info info1, Info info2) {
        ClassProperties props = new ClassProperties(info1.getClass());
        List<String> istringProps =
                props.properties().stream()
                        .filter(p -> props.getter(p, InternationalString.class) != null)
                        .toList();
        for (String isp : istringProps) {
            InternationalString i1 = (InternationalString) OwsUtils.get(info1, isp);
            InternationalString i2 = (InternationalString) OwsUtils.get(info2, isp);

            Supplier<String> msg =
                    () ->
                            "%s.%s:InternationalString"
                                    .formatted(info1.getClass().getSimpleName(), isp);
            assertEquals(i1, i2, msg);
        }
    }
}
