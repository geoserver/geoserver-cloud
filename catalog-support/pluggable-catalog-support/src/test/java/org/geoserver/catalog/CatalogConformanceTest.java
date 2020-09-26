/*
 * (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog;

import static com.google.common.collect.Sets.newHashSet;
import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.asc;
import static org.geoserver.catalog.Predicates.contains;
import static org.geoserver.catalog.Predicates.desc;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.or;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CatalogPropertyAccessor;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.RunnerBase;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.AccessMode;
import org.geoserver.security.SecuredResourceNameChangeListener;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.filter.sort.SortBy;

/**
 * Initially a verbatim copy of {@code gs-main}'s {@code
 * org.geoserver.catalog.impl.CatalogImplTest}, adapted to not subclass {@code
 * GeoServerSystemTestSupport} as all that machinery is not really necessary, plus, {@code
 * GeoServerSystemTestSupport} instantiates {@code org.geoserver.catalog.impl.CatalogImpl}
 * indirectly, which defeats our purpose of testing different catalog/facade implementations with
 * this test class as a contract conformance check.
 */
public abstract class CatalogConformanceTest {

    protected Catalog rawCatalog;

    public CatalogTestData data;

    private DataAccessRuleDAO dataAccessRuleDAO;

    public @Rule TemporaryFolder tmpFolder = new TemporaryFolder();

    protected void addLayerAccessRule(
            String workspace, String layer, AccessMode mode, String... roles) throws IOException {
        DataAccessRuleDAO dao = this.dataAccessRuleDAO;
        DataAccessRule rule = new DataAccessRule();
        rule.setRoot(workspace);
        rule.setLayer(layer);
        rule.setAccessMode(mode);
        rule.getRoles().addAll(Arrays.asList(roles));
        dao.addRule(rule);
        dao.storeRules();
    }

    protected abstract Catalog createCatalog();

    public static @BeforeClass void oneTimeSetup() {
        GeoServerExtensionsHelper.setIsSpringContext(false);
        if (null == GeoServerExtensions.bean("sldHandler"))
            GeoServerExtensionsHelper.singleton("sldHandler", new SLDHandler(), StyleHandler.class);
    }

    @Before
    public void setUp() throws Exception {
        rawCatalog = createCatalog();
        rawCatalog.setResourceLoader(new GeoServerResourceLoader());
        GeoServerDataDirectory dd = new GeoServerDataDirectory(tmpFolder.getRoot());
        dataAccessRuleDAO = new DataAccessRuleDAO(dd, rawCatalog);
        dataAccessRuleDAO.reload();

        data = CatalogTestData.empty(() -> rawCatalog).createCatalogObjects();
    }

    @After
    public void deleteAll() {
        Catalog catalog = this.rawCatalog;
        Collection<CatalogListener> listeners = new ArrayList<>(catalog.getListeners());
        for (CatalogListener listener : listeners) {
            if (listener instanceof TestListener || listener instanceof ExceptionThrowingListener)
                catalog.removeListener(listener);
        }
        data.deleteAll(catalog);
    }

    protected void addWorkspace() {
        rawCatalog.add(data.workspaceA);
    }

    protected void addNamespace() {
        rawCatalog.add(data.namespaceA);
    }

    protected void addDataStore() {
        addWorkspace();
        rawCatalog.add(data.dataStoreA);
    }

    protected void addCoverageStore() {
        addWorkspace();
        rawCatalog.add(data.coverageStoreA);
    }

    protected void addWMSStore() {
        addWorkspace();
        rawCatalog.add(data.wmsStoreA);
    }

    protected void addWMTSStore() {
        addWorkspace();
        rawCatalog.add(data.wmtsStoreA);
    }

    protected void addFeatureType() {
        addDataStore();
        addNamespace();
        rawCatalog.add(data.featureTypeA);
    }

    protected void addCoverage() {
        addCoverageStore();
        addNamespace();
        rawCatalog.add(data.coverageA);
    }

    protected void addWMSLayer() {
        addWMSStore();
        addNamespace();
        rawCatalog.add(data.wmsLayerA);
    }

    protected void addWMTSLayer() {
        addWMTSStore();
        addNamespace();
        rawCatalog.add(data.wmtsLayerA);
    }

    protected void addStyle() {
        rawCatalog.add(data.style1);
    }

    protected void addDefaultStyle() {
        StyleInfo defaultStyle = data.createStyle(StyleInfo.DEFAULT_LINE);
        rawCatalog.add(defaultStyle);
    }

    protected void addLayer() {
        addFeatureType();
        addStyle();
        rawCatalog.add(data.layerFeatureTypeA);
    }

    protected void addLayerGroup() {
        addLayer();
        rawCatalog.add(data.layerGroup1);
    }

    @Test
    public void testAddNamespace() {
        assertTrue(rawCatalog.getNamespaces().isEmpty());
        rawCatalog.add(data.namespaceA);
        assertEquals(1, rawCatalog.getNamespaces().size());

        NamespaceInfo ns2 = rawCatalog.getFactory().createNamespace();

        try {
            rawCatalog.add(ns2);
            fail("adding without a prefix should throw exception");
        } catch (Exception e) {
        }

        ns2.setPrefix("ns2Prefix");
        try {
            rawCatalog.add(ns2);
            fail("adding without a uri should throw exception");
        } catch (Exception e) {
        }

        ns2.setURI("bad uri");
        try {
            rawCatalog.add(ns2);
            fail("adding an invalid uri should throw exception");
        } catch (Exception e) {
        }

        ns2.setURI("ns2URI");

        try {
            rawCatalog.getNamespaces().add(ns2);
            fail("adding directly should throw an exception");
        } catch (Exception e) {
        }

        rawCatalog.add(ns2);
    }

    @Test
    public void testAddIsolatedNamespace() {
        // create non isolated namespace
        NamespaceInfoImpl namespace1 = new NamespaceInfoImpl();
        namespace1.setPrefix("isolated_namespace_1");
        namespace1.setURI("http://www.isolated_namespace.com");
        // create isolated namespace with the same URI
        NamespaceInfoImpl namespace2 = new NamespaceInfoImpl();
        namespace2.setPrefix("isolated_namespace_2");
        namespace2.setURI("http://www.isolated_namespace.com");
        namespace2.setIsolated(true);
        try {
            // add the namespaces to the catalog
            rawCatalog.add(namespace1);
            rawCatalog.add(namespace2);
            // retrieve the non isolated namespace by prefix
            NamespaceInfo foundNamespace1 = rawCatalog.getNamespaceByPrefix("isolated_namespace_1");
            assertThat(foundNamespace1.getPrefix(), is("isolated_namespace_1"));
            assertThat(foundNamespace1.getURI(), is("http://www.isolated_namespace.com"));
            assertThat(foundNamespace1.isIsolated(), is(false));
            // retrieve the isolated namespace by prefix
            NamespaceInfo foundNamespace2 = rawCatalog.getNamespaceByPrefix("isolated_namespace_2");
            assertThat(foundNamespace2.getPrefix(), is("isolated_namespace_2"));
            assertThat(foundNamespace2.getURI(), is("http://www.isolated_namespace.com"));
            assertThat(foundNamespace2.isIsolated(), is(true));
            // retrieve the namespace by URI, the non isolated one should be returned
            NamespaceInfo foundNamespace3 =
                    rawCatalog.getNamespaceByURI("http://www.isolated_namespace.com");
            assertThat(foundNamespace3.getPrefix(), is("isolated_namespace_1"));
            assertThat(foundNamespace3.getURI(), is("http://www.isolated_namespace.com"));
            assertThat(foundNamespace3.isIsolated(), is(false));
            // remove the non isolated namespace
            rawCatalog.remove(foundNamespace1);
            // retrieve the namespace by URI, NULL should be returned
            NamespaceInfo foundNamespace4 =
                    rawCatalog.getNamespaceByURI("http://www.isolated_namespace.com");
            assertThat(foundNamespace4, nullValue());
        } finally {
            // remove the namespaces
            rawCatalog.remove(namespace1);
            rawCatalog.remove(namespace2);
        }
    }

    @Test
    public void testRemoveNamespace() {
        rawCatalog.add(data.namespaceA);
        assertEquals(1, rawCatalog.getNamespaces().size());

        try {
            assertFalse(rawCatalog.getNamespaces().remove(data.namespaceA));
            fail("removing directly should throw an exception");
        } catch (Exception e) {
        }

        rawCatalog.remove(data.namespaceA);
        assertTrue(rawCatalog.getNamespaces().isEmpty());
    }

    @Test
    public void testGetNamespaceById() {
        rawCatalog.add(data.namespaceA);
        NamespaceInfo ns2 = rawCatalog.getNamespace(data.namespaceA.getId());

        assertNotNull(ns2);
        assertNotSame(data.namespaceA, ns2);
        assertEquals(data.namespaceA, ns2);
    }

    @Test
    public void testGetNamespaceByPrefix() {
        rawCatalog.add(data.namespaceA);

        NamespaceInfo ns2 = rawCatalog.getNamespaceByPrefix(data.namespaceA.getPrefix());
        assertNotNull(ns2);
        assertNotSame(data.namespaceA, ns2);
        assertEquals(data.namespaceA, ns2);

        NamespaceInfo ns3 = rawCatalog.getNamespaceByPrefix(null);
        assertNotNull(ns3);
        assertNotSame(data.namespaceA, ns3);
        assertEquals(data.namespaceA, ns3);

        NamespaceInfo ns4 = rawCatalog.getNamespaceByPrefix(Catalog.DEFAULT);
        assertNotNull(ns4);
        assertNotSame(data.namespaceA, ns4);
        assertEquals(data.namespaceA, ns4);
    }

    @Test
    public void testGetNamespaceByURI() {
        rawCatalog.add(data.namespaceA);
        NamespaceInfo ns2 = rawCatalog.getNamespaceByURI(data.namespaceA.getURI());

        assertNotNull(ns2);
        assertNotSame(data.namespaceA, ns2);
        assertEquals(data.namespaceA, ns2);
    }

    @Test
    public void testSetDefaultNamespaceInvalid() {
        try {
            rawCatalog.setDefaultNamespace(data.namespaceA);
            fail("Default namespace must exist in catalog");
        } catch (IllegalArgumentException e) {
            assertEquals("No such namespace: 'wsName'", e.getMessage());
        }
    }

    @Test
    public void testModifyNamespace() {
        rawCatalog.add(data.namespaceA);

        NamespaceInfo ns2 = rawCatalog.getNamespaceByPrefix(data.namespaceA.getPrefix());
        ns2.setPrefix(null);
        ns2.setURI(null);

        try {
            rawCatalog.save(ns2);
            fail("setting prefix to null should throw exception");
        } catch (Exception e) {
        }

        ns2.setPrefix("ns2Prefix");
        try {
            rawCatalog.save(ns2);
            fail("setting uri to null should throw exception");
        } catch (Exception e) {
        }

        ns2.setURI("ns2URI");

        NamespaceInfo ns3 = rawCatalog.getNamespaceByPrefix(data.namespaceA.getPrefix());
        assertEquals(data.namespaceA.getPrefix(), ns3.getPrefix());
        assertEquals(data.namespaceA.getURI(), ns3.getURI());

        rawCatalog.save(ns2);
        // ns3 = catalog.getNamespaceByPrefix(ns.getPrefix());
        ns3 = rawCatalog.getNamespaceByPrefix("ns2Prefix");
        assertEquals(ns2, ns3);
        assertEquals("ns2Prefix", ns3.getPrefix());
        assertEquals("ns2URI", ns3.getURI());
    }

    @Test
    public void testNamespaceEvents() {
        TestListener l = new TestListener();
        rawCatalog.addListener(l);

        NamespaceInfo ns = rawCatalog.getFactory().createNamespace();
        ns.setPrefix("ns2Prefix");
        ns.setURI("ns2URI");

        assertTrue(l.added.isEmpty());
        assertTrue(l.modified.isEmpty());
        rawCatalog.add(ns);
        assertEquals(1, l.added.size());
        assertEquals(ns, l.added.get(0).getSource());
        assertEquals(1, l.modified.size());
        assertEquals(rawCatalog, l.modified.get(0).getSource());
        assertEquals("defaultNamespace", l.modified.get(0).getPropertyNames().get(0));
        assertEquals(1, l.postModified.size());
        assertEquals(rawCatalog, l.postModified.get(0).getSource());
        assertEquals("defaultNamespace", l.postModified.get(0).getPropertyNames().get(0));

        ns = rawCatalog.getNamespaceByPrefix("ns2Prefix");
        ns.setURI("changed");
        rawCatalog.save(ns);

        assertEquals(2, l.modified.size());
        assertEquals(1, l.modified.get(1).getPropertyNames().size());
        assertTrue(l.modified.get(1).getPropertyNames().get(0).equalsIgnoreCase("uri"));
        assertTrue(l.modified.get(1).getOldValues().contains("ns2URI"));
        assertTrue(l.modified.get(1).getNewValues().contains("changed"));
        assertEquals(2, l.postModified.size());
        assertEquals(1, l.postModified.get(1).getPropertyNames().size());
        assertTrue(l.postModified.get(1).getPropertyNames().get(0).equalsIgnoreCase("uri"));
        assertTrue(l.postModified.get(1).getOldValues().contains("ns2URI"));
        assertTrue(l.postModified.get(1).getNewValues().contains("changed"));

        assertTrue(l.removed.isEmpty());
        rawCatalog.remove(ns);
        assertEquals(1, l.removed.size());
        assertEquals(ns, l.removed.get(0).getSource());
    }

    @Test
    public void testAddWorkspace() {
        assertTrue(rawCatalog.getWorkspaces().isEmpty());
        rawCatalog.add(data.workspaceA);
        assertEquals(1, rawCatalog.getWorkspaces().size());

        WorkspaceInfo ws2 = rawCatalog.getFactory().createWorkspace();

        try {
            rawCatalog.getWorkspaces().add(ws2);
            fail("adding directly should throw an exception");
        } catch (Exception e) {
        }

        try {
            rawCatalog.add(ws2);
            fail("addign without a name should throw an exception");
        } catch (Exception e) {
        }

        ws2.setName("ws2");
        rawCatalog.add(ws2);
    }

    @Test
    public void testRemoveWorkspace() {
        rawCatalog.add(data.workspaceA);
        assertEquals(1, rawCatalog.getWorkspaces().size());

        try {
            assertFalse(rawCatalog.getWorkspaces().remove(data.workspaceA));
            fail("removing directly should throw an exception");
        } catch (Exception e) {
        }

        rawCatalog.remove(data.workspaceA);
        assertTrue(rawCatalog.getWorkspaces().isEmpty());
    }

    @Test
    public void testAddIsolatedWorkspace() {
        // create isolated workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName("isolated_workspace");
        workspace.setIsolated(true);
        try {
            // add it to the catalog
            rawCatalog.add(workspace);
            // retrieve the isolated workspace
            WorkspaceInfo foundWorkspace = rawCatalog.getWorkspaceByName("isolated_workspace");
            assertThat(foundWorkspace.isIsolated(), is(true));
        } finally {
            // remove the isolated workspace
            rawCatalog.remove(workspace);
        }
    }

    @Test
    public void testAutoSetDefaultWorkspace() {
        rawCatalog.add(data.workspaceA);
        assertEquals(1, rawCatalog.getWorkspaces().size());
        assertEquals(data.workspaceA, rawCatalog.getDefaultWorkspace());
        assertNull(rawCatalog.getDefaultNamespace());
    }

    @Test
    public void testRemoveDefaultWorkspace() {
        rawCatalog.add(data.workspaceA);
        assertNotNull(rawCatalog.getDefaultWorkspace());
        rawCatalog.remove(data.workspaceA);
        assertNull(rawCatalog.getDefaultWorkspace());
    }

    @Test
    public void testAutoCascadeDefaultWorksapce() {
        CatalogFactory factory = rawCatalog.getFactory();
        WorkspaceInfo ws1 = factory.createWorkspace();
        ws1.setName("ws1Name");
        WorkspaceInfo ws2 = factory.createWorkspace();
        ws2.setName("ws2Name");
        rawCatalog.add(ws1);
        rawCatalog.add(ws2);
        assertEquals(ws1, rawCatalog.getDefaultWorkspace());
        rawCatalog.remove(ws1);
        assertEquals(ws2, rawCatalog.getDefaultWorkspace());
    }

    @Test
    public void testAutoSetDefaultNamespace() {
        rawCatalog.add(data.namespaceA);
        assertEquals(1, rawCatalog.getNamespaces().size());
        assertEquals(data.namespaceA, rawCatalog.getDefaultNamespace());
    }

    @Test
    public void testRemoveDefaultNamespace() {
        rawCatalog.add(data.namespaceA);
        rawCatalog.remove(data.namespaceA);
        assertNull(rawCatalog.getDefaultNamespace());
    }

    @Test
    public void testAutoCascadeDefaultNamespace() {
        CatalogFactory factory = rawCatalog.getFactory();
        NamespaceInfo ns1 = factory.createNamespace();
        ns1.setPrefix("1");
        ns1.setURI("http://www.geoserver.org/1");
        NamespaceInfo ns2 = factory.createNamespace();
        ns2.setPrefix("2");
        ns2.setURI("http://www.geoserver.org/2");
        rawCatalog.add(ns1);
        rawCatalog.add(ns2);
        assertEquals(ns1, rawCatalog.getDefaultNamespace());
        rawCatalog.remove(ns1);
        assertEquals(ns2, rawCatalog.getDefaultNamespace());
    }

    @Test
    public void testAutoSetDefaultStore() {
        rawCatalog.add(data.workspaceA);
        rawCatalog.add(data.dataStoreA);
        assertEquals(1, rawCatalog.getDataStores().size());
        assertEquals(data.dataStoreA, rawCatalog.getDefaultDataStore(data.workspaceA));
    }

    @Test
    public void testRemoveDefaultStore() {
        rawCatalog.add(data.workspaceA);
        rawCatalog.add(data.dataStoreA);
        rawCatalog.remove(data.dataStoreA);
        assertNull(rawCatalog.getDefaultDataStore(data.workspaceA));
    }

    @Test
    public void testGetWorkspaceById() {
        rawCatalog.add(data.workspaceA);
        WorkspaceInfo ws2 = rawCatalog.getWorkspace(data.workspaceA.getId());

        assertNotNull(ws2);
        assertNotSame(data.workspaceA, ws2);
        assertEquals(data.workspaceA, ws2);
    }

    @Test
    public void testGetWorkspaceByName() {
        rawCatalog.add(data.workspaceA);
        WorkspaceInfo ws2 = rawCatalog.getWorkspaceByName(data.workspaceA.getName());

        assertNotNull(ws2);
        assertNotSame(data.workspaceA, ws2);
        assertEquals(data.workspaceA, ws2);

        WorkspaceInfo ws3 = rawCatalog.getWorkspaceByName(null);
        assertNotNull(ws3);
        assertNotSame(data.workspaceA, ws3);
        assertEquals(data.workspaceA, ws3);

        WorkspaceInfo ws4 = rawCatalog.getWorkspaceByName(Catalog.DEFAULT);
        assertNotNull(ws4);
        assertNotSame(data.workspaceA, ws4);
        assertEquals(data.workspaceA, ws4);
    }

    @Test
    public void testSetDefaultWorkspaceInvalid() {
        try {
            rawCatalog.setDefaultWorkspace(data.workspaceA);
            fail("Default workspace must exist in catalog");
        } catch (IllegalArgumentException e) {
            assertEquals("No such workspace: 'wsName'", e.getMessage());
        }
    }

    @Test
    public void testModifyWorkspace() {
        rawCatalog.add(data.workspaceA);

        WorkspaceInfo ws2 = rawCatalog.getWorkspaceByName(data.workspaceA.getName());
        ws2.setName(null);
        try {
            rawCatalog.save(ws2);
            fail("setting name to null should throw exception");
        } catch (Exception e) {
        }

        ws2.setName("ws2");

        WorkspaceInfo ws3 = rawCatalog.getWorkspaceByName(data.workspaceA.getName());
        assertEquals("wsName", ws3.getName());

        rawCatalog.save(ws2);
        ws3 = rawCatalog.getWorkspaceByName(ws2.getName());
        assertEquals(ws2, ws3);
        assertEquals("ws2", ws3.getName());
    }

    @Test
    public void testWorkspaceEvents() {
        TestListener l = new TestListener();
        rawCatalog.addListener(l);

        WorkspaceInfo ws = rawCatalog.getFactory().createWorkspace();
        ws.setName("ws2");

        assertTrue(l.added.isEmpty());
        assertTrue(l.modified.isEmpty());
        rawCatalog.add(ws);
        assertEquals(1, l.added.size());
        assertEquals(ws, l.added.get(0).getSource());
        assertEquals(rawCatalog, l.modified.get(0).getSource());
        assertEquals("defaultWorkspace", l.modified.get(0).getPropertyNames().get(0));
        assertEquals(rawCatalog, l.postModified.get(0).getSource());
        assertEquals("defaultWorkspace", l.postModified.get(0).getPropertyNames().get(0));

        ws = rawCatalog.getWorkspaceByName("ws2");
        ws.setName("changed");

        rawCatalog.save(ws);
        assertEquals(2, l.modified.size());
        assertTrue(l.modified.get(1).getPropertyNames().contains("name"));
        assertTrue(l.modified.get(1).getOldValues().contains("ws2"));
        assertTrue(l.modified.get(1).getNewValues().contains("changed"));
        assertTrue(l.postModified.get(1).getPropertyNames().contains("name"));
        assertTrue(l.postModified.get(1).getOldValues().contains("ws2"));
        assertTrue(l.postModified.get(1).getNewValues().contains("changed"));

        assertTrue(l.removed.isEmpty());
        rawCatalog.remove(ws);
        assertEquals(1, l.removed.size());
        assertEquals(ws, l.removed.get(0).getSource());
    }

    @Test
    public void testAddDataStore() {
        assertTrue(rawCatalog.getDataStores().isEmpty());

        data.dataStoreA.setWorkspace(null);
        try {
            rawCatalog.add(data.dataStoreA);
            fail("adding with no workspace should throw exception");
        } catch (Exception e) {
        }

        data.dataStoreA.setWorkspace(data.workspaceA);
        rawCatalog.add(data.workspaceA);
        rawCatalog.add(data.dataStoreA);

        assertEquals(1, rawCatalog.getDataStores().size());

        DataStoreInfo retrieved = rawCatalog.getDataStore(data.dataStoreA.getId());
        assertNotNull(retrieved);
        assertSame(rawCatalog, retrieved.getCatalog());

        DataStoreInfo ds2 = rawCatalog.getFactory().createDataStore();
        try {
            rawCatalog.add(ds2);
            fail("adding without a name should throw exception");
        } catch (Exception e) {
        }

        ds2.setName("ds2Name");
        try {
            rawCatalog.getDataStores().add(ds2);
            fail("adding directly should throw an exception");
        } catch (Exception e) {
        }

        ds2.setWorkspace(data.workspaceA);

        rawCatalog.add(ds2);
        assertEquals(2, rawCatalog.getDataStores().size());
    }

    @Test
    public void testAddDataStoreDefaultWorkspace() {
        rawCatalog.add(data.workspaceA);
        rawCatalog.setDefaultWorkspace(data.workspaceA);

        DataStoreInfo ds2 = rawCatalog.getFactory().createDataStore();
        ds2.setName("ds2Name");
        rawCatalog.add(ds2);

        assertEquals(data.workspaceA, ds2.getWorkspace());
    }

    @Test
    public void testRemoveDataStore() {
        addDataStore();
        assertEquals(1, rawCatalog.getDataStores().size());

        try {
            assertFalse(rawCatalog.getDataStores().remove(data.dataStoreA));
            fail("removing directly should throw an exception");
        } catch (Exception e) {
        }

        rawCatalog.remove(data.dataStoreA);
        assertTrue(rawCatalog.getDataStores().isEmpty());
    }

    @Test
    public void testGetDataStoreById() {
        addDataStore();

        DataStoreInfo ds2 = rawCatalog.getDataStore(data.dataStoreA.getId());
        assertNotNull(ds2);
        assertNotSame(data.dataStoreA, ds2);
        assertEquals(data.dataStoreA, ds2);
        assertSame(rawCatalog, ds2.getCatalog());
    }

    @Test
    public void testGetDataStoreByName() {
        addDataStore();

        DataStoreInfo ds2 = rawCatalog.getDataStoreByName(data.dataStoreA.getName());
        assertNotNull(ds2);
        assertNotSame(data.dataStoreA, ds2);
        assertEquals(data.dataStoreA, ds2);
        assertSame(rawCatalog, ds2.getCatalog());

        DataStoreInfo ds3 = rawCatalog.getDataStoreByName(data.workspaceA, null);
        assertNotNull(ds3);
        assertNotSame(data.dataStoreA, ds3);
        assertEquals(data.dataStoreA, ds3);

        DataStoreInfo ds4 = rawCatalog.getDataStoreByName(data.workspaceA, Catalog.DEFAULT);
        assertNotNull(ds4);
        assertNotSame(data.dataStoreA, ds4);
        assertEquals(data.dataStoreA, ds4);

        DataStoreInfo ds5 = rawCatalog.getDataStoreByName(Catalog.DEFAULT, Catalog.DEFAULT);
        assertNotNull(ds5);
        assertNotSame(data.dataStoreA, ds5);
        assertEquals(data.dataStoreA, ds5);
    }

    @Test
    public void testGetStoreByName() {
        addDataStore();

        StoreInfo ds2 = rawCatalog.getStoreByName(data.dataStoreA.getName(), StoreInfo.class);
        assertNotNull(ds2);
        assertNotSame(data.dataStoreA, ds2);
        assertEquals(data.dataStoreA, ds2);
        assertSame(rawCatalog, ds2.getCatalog());

        StoreInfo ds3 = rawCatalog.getStoreByName(data.workspaceA, null, StoreInfo.class);
        assertNotNull(ds3);
        assertNotSame(data.dataStoreA, ds3);
        assertEquals(data.dataStoreA, ds3);

        StoreInfo ds4 =
                rawCatalog.getStoreByName(data.workspaceA, Catalog.DEFAULT, StoreInfo.class);
        assertNotNull(ds4);
        assertNotSame(data.dataStoreA, ds4);
        assertEquals(data.dataStoreA, ds4);

        StoreInfo ds5 =
                rawCatalog.getStoreByName(Catalog.DEFAULT, Catalog.DEFAULT, StoreInfo.class);
        assertNotNull(ds5);
        assertNotSame(data.dataStoreA, ds5);
        assertEquals(data.dataStoreA, ds5);

        StoreInfo ds6 = rawCatalog.getStoreByName((String) null, null, StoreInfo.class);
        assertNotNull(ds6);
        assertNotSame(data.dataStoreA, ds6);
        assertEquals(data.dataStoreA, ds3);

        StoreInfo ds7 =
                rawCatalog.getStoreByName(Catalog.DEFAULT, Catalog.DEFAULT, StoreInfo.class);
        assertNotNull(ds7);
        assertNotSame(data.dataStoreA, ds7);
        assertEquals(ds6, ds7);
    }

    @Test
    public void testModifyDataStore() {
        addDataStore();

        DataStoreInfo ds2 = rawCatalog.getDataStoreByName(data.dataStoreA.getName());
        ds2.setName("dsName2");
        ds2.setDescription("dsDescription2");

        DataStoreInfo ds3 = rawCatalog.getDataStoreByName(data.dataStoreA.getName());
        assertEquals("dsName", ds3.getName());
        assertEquals("dsDescription", ds3.getDescription());

        rawCatalog.save(ds2);
        ds3 = rawCatalog.getDataStoreByName("dsName2");
        assertEquals(ds2, ds3);
        assertEquals("dsName2", ds3.getName());
        assertEquals("dsDescription2", ds3.getDescription());
    }

    @Test
    public void testChangeDataStoreWorkspace() throws Exception {
        addDataStore();

        WorkspaceInfo ws2 = rawCatalog.getFactory().createWorkspace();
        ws2.setName("newWorkspace");
        rawCatalog.add(ws2);
        ws2 = rawCatalog.getWorkspaceByName(ws2.getName());

        DataStoreInfo ds2 = rawCatalog.getDataStoreByName(data.dataStoreA.getName());
        ds2.setWorkspace(ws2);
        rawCatalog.save(ds2);

        assertNull(rawCatalog.getDataStoreByName(data.workspaceA, ds2.getName()));
        assertNotNull(rawCatalog.getDataStoreByName(ws2, ds2.getName()));
    }

    @Test
    public void testDataStoreEvents() {
        addWorkspace();

        TestListener l = new TestListener();
        rawCatalog.addListener(l);

        assertEquals(0, l.added.size());
        rawCatalog.add(data.dataStoreA);
        assertEquals(1, l.added.size());
        assertEquals(data.dataStoreA, l.added.get(0).getSource());
        assertEquals(1, l.modified.size());
        assertEquals(rawCatalog, l.modified.get(0).getSource());
        assertEquals(1, l.postModified.size());
        assertEquals(rawCatalog, l.postModified.get(0).getSource());

        DataStoreInfo ds2 = rawCatalog.getDataStoreByName(data.dataStoreA.getName());
        ds2.setDescription("changed");

        assertEquals(1, l.modified.size());
        rawCatalog.save(ds2);
        assertEquals(2, l.modified.size());

        CatalogModifyEvent me = l.modified.get(1);
        assertEquals(ds2, me.getSource());
        assertEquals(1, me.getPropertyNames().size());
        assertEquals("description", me.getPropertyNames().get(0));

        assertEquals(1, me.getOldValues().size());
        assertEquals(1, me.getNewValues().size());

        assertEquals("dsDescription", me.getOldValues().get(0));
        assertEquals("changed", me.getNewValues().get(0));

        CatalogPostModifyEvent pme = l.postModified.get(1);
        assertEquals(ds2, pme.getSource());
        assertEquals(1, pme.getPropertyNames().size());
        assertEquals("description", pme.getPropertyNames().get(0));

        assertEquals(1, pme.getOldValues().size());
        assertEquals(1, pme.getNewValues().size());

        assertEquals("dsDescription", pme.getOldValues().get(0));
        assertEquals("changed", pme.getNewValues().get(0));

        assertEquals(0, l.removed.size());
        rawCatalog.remove(data.dataStoreA);

        assertEquals(1, l.removed.size());
        assertEquals(data.dataStoreA, l.removed.get(0).getSource());
    }

    @Test
    public void testAddFeatureType() {
        assertTrue(rawCatalog.getFeatureTypes().isEmpty());

        addFeatureType();
        assertEquals(1, rawCatalog.getFeatureTypes().size());

        FeatureTypeInfo ft2 = rawCatalog.getFactory().createFeatureType();
        try {
            rawCatalog.add(ft2);
            fail("adding with no name should throw exception");
        } catch (Exception e) {
        }

        ft2.setName("ft2Name");

        try {
            rawCatalog.add(ft2);
            fail("adding with no store should throw exception");
        } catch (Exception e) {
        }

        ft2.setStore(data.dataStoreA);
        ft2.getKeywords().add(new Keyword("keyword"));

        rawCatalog.add(ft2);
        FeatureTypeInfo retrieved = rawCatalog.getFeatureTypeByName("ft2Name");
        assertSame(rawCatalog, retrieved.getCatalog());

        FeatureTypeInfo ft3 = rawCatalog.getFactory().createFeatureType();
        ft3.setName("ft3Name");
        try {
            rawCatalog.getFeatureTypes().add(ft3);
            fail("adding directly should throw an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testAddCoverage() {
        // set a default namespace
        assertNotNull(rawCatalog.getCoverages());
        assertTrue(rawCatalog.getCoverages().isEmpty());

        addCoverage();
        assertEquals(1, rawCatalog.getCoverages().size());

        CoverageInfo cv2 = rawCatalog.getFactory().createCoverage();
        try {
            rawCatalog.add(cv2);
            fail("adding with no name should throw exception");
        } catch (Exception e) {
        }

        cv2.setName("cv2Name");
        try {
            rawCatalog.add(cv2);
            fail("adding with no store should throw exception");
        } catch (Exception e) {
        }

        cv2.setStore(data.coverageStoreA);
        rawCatalog.add(cv2);
        assertEquals(2, rawCatalog.getCoverages().size());

        CoverageInfo fromCatalog = rawCatalog.getCoverageByName("cv2Name");
        assertNotNull(fromCatalog);
        assertSame(rawCatalog, fromCatalog.getCatalog());
        // ensure the collection properties are set to NullObjects and not to null
        assertNotNull(fromCatalog.getParameters());

        CoverageInfo cv3 = rawCatalog.getFactory().createCoverage();
        cv3.setName("cv3Name");
        try {
            rawCatalog.getCoverages().add(cv3);
            fail("adding directly should throw an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testAddWMSLayer() {
        // set a default namespace
        assertTrue(rawCatalog.getResources(WMSLayerInfo.class).isEmpty());
        addWMSLayer();
        assertEquals(1, rawCatalog.getResources(WMSLayerInfo.class).size());
    }

    @Test
    public void testAddWMTSLayer() {
        assertTrue(rawCatalog.getResources(WMTSLayerInfo.class).isEmpty());
        addWMTSLayer();
        assertEquals(1, rawCatalog.getResources(WMTSLayerInfo.class).size());
    }

    @Test
    public void testRemoveFeatureType() {
        addFeatureType();
        assertFalse(rawCatalog.getFeatureTypes().isEmpty());

        try {
            rawCatalog.getFeatureTypes().remove(data.featureTypeA);
            fail("removing directly should cause exception");
        } catch (Exception e) {
        }

        rawCatalog.remove(data.featureTypeA);
        assertTrue(rawCatalog.getFeatureTypes().isEmpty());
    }

    @Test
    public void testRemoveWMSLayer() {
        addWMSLayer();
        assertFalse(rawCatalog.getResources(WMSLayerInfo.class).isEmpty());

        rawCatalog.remove(data.wmsLayerA);
        assertTrue(rawCatalog.getResources(WMSLayerInfo.class).isEmpty());
    }

    @Test
    public void testRemoveWMTSLayer() {
        addWMTSLayer();
        assertFalse(rawCatalog.getResources(WMTSLayerInfo.class).isEmpty());

        rawCatalog.remove(data.wmtsLayerA);
        assertTrue(rawCatalog.getResources(WMTSLayerInfo.class).isEmpty());
    }

    @Test
    public void testGetFeatureTypeById() {
        addFeatureType();
        FeatureTypeInfo ft2 = rawCatalog.getFeatureType(data.featureTypeA.getId());

        assertNotNull(ft2);
        assertNotSame(data.featureTypeA, ft2);
        assertEquals(data.featureTypeA, ft2);
        assertSame(rawCatalog, ft2.getCatalog());
    }

    @Test
    public void testGetFeatureTypeByName() {
        addFeatureType();
        FeatureTypeInfo ft2 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());

        assertNotNull(ft2);
        assertNotSame(data.featureTypeA, ft2);
        assertEquals(data.featureTypeA, ft2);
        assertSame(rawCatalog, ft2.getCatalog());

        NamespaceInfo ns2 = rawCatalog.getFactory().createNamespace();
        ns2.setPrefix("ns2Prefix");
        ns2.setURI("ns2URI");
        rawCatalog.add(ns2);

        FeatureTypeInfo ft3 = rawCatalog.getFactory().createFeatureType();
        ft3.setName("ft3Name");
        ft3.setStore(data.dataStoreA);
        ft3.setNamespace(ns2);
        rawCatalog.add(ft3);

        FeatureTypeInfo ft4 = rawCatalog.getFeatureTypeByName(ns2.getPrefix(), ft3.getName());
        assertNotNull(ft4);
        assertNotSame(ft4, ft3);
        assertEquals(ft3, ft4);

        ft4 = rawCatalog.getFeatureTypeByName(ns2.getURI(), ft3.getName());
        assertNotNull(ft4);
        assertNotSame(ft4, ft3);
        assertEquals(ft3, ft4);
    }

    @Test
    public void testGetFeatureTypesByStore() {
        rawCatalog.add(data.namespaceA);
        rawCatalog.add(data.workspaceA);

        rawCatalog.setDefaultNamespace(data.namespaceA);
        rawCatalog.setDefaultWorkspace(data.workspaceA);

        DataStoreInfo ds1 = rawCatalog.getFactory().createDataStore();
        ds1.setName("ds1");
        rawCatalog.add(ds1);

        FeatureTypeInfo ft1 = rawCatalog.getFactory().createFeatureType();
        ft1.setName("ft1");
        ft1.setStore(ds1);
        rawCatalog.add(ft1);

        FeatureTypeInfo ft2 = rawCatalog.getFactory().createFeatureType();
        ft2.setName("ft2");
        ft2.setStore(ds1);
        rawCatalog.add(ft2);

        DataStoreInfo ds2 = rawCatalog.getFactory().createDataStore();
        ds2.setName("ds2");
        rawCatalog.add(ds2);

        FeatureTypeInfo ft3 = rawCatalog.getFactory().createFeatureType();
        ft3.setName("ft3");
        ft3.setStore(ds2);
        rawCatalog.add(ft3);

        List<ResourceInfo> r = rawCatalog.getResourcesByStore(ds1, ResourceInfo.class);
        assertEquals(2, r.size());
        assertTrue(r.contains(ft1));
        assertTrue(r.contains(ft2));
        Catalog resourceCatalog = r.get(0).getCatalog();
        assertNotNull(resourceCatalog);
        assertSame(rawCatalog, resourceCatalog);
        resourceCatalog = r.get(1).getCatalog();
        assertNotNull(resourceCatalog);
        assertSame(rawCatalog, resourceCatalog);
    }

    @Test
    public void testModifyFeatureType() {
        addFeatureType();

        FeatureTypeInfo ft2 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        ft2.setDescription("ft2Description");
        ft2.getKeywords().add(new Keyword("ft2"));

        FeatureTypeInfo ft3 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        assertEquals("ftName", ft3.getName());
        assertEquals("ftDescription", ft3.getDescription());
        assertTrue(ft3.getKeywords().isEmpty());

        rawCatalog.save(ft2);
        ft3 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        assertEquals(ft2, ft3);
        assertEquals("ft2Description", ft3.getDescription());
        assertEquals(1, ft3.getKeywords().size());
    }

    @Test
    public void testModifyMetadataLinks() {
        addFeatureType();

        FeatureTypeInfo ft2 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        MetadataLinkInfo ml = rawCatalog.getFactory().createMetadataLink();
        ml.setContent("http://www.geoserver.org/meta");
        ml.setType("text/plain");
        ml.setMetadataType("iso");
        ft2.getMetadataLinks().clear();
        ft2.getMetadataLinks().add(ml);
        rawCatalog.save(ft2);

        FeatureTypeInfo ft3 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        MetadataLinkInfo ml3 = ft3.getMetadataLinks().get(0);
        ml3.setType("application/json");

        // do not save and grab another, the metadata link must not have been modified
        FeatureTypeInfo ft4 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        MetadataLinkInfo ml4 = ft4.getMetadataLinks().get(0);
        assertEquals("text/plain", ml4.getType());

        // now save and grab yet another, the modification must have happened
        rawCatalog.save(ft3);
        FeatureTypeInfo ft5 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        MetadataLinkInfo ml5 = ft5.getMetadataLinks().get(0);
        assertEquals("application/json", ml5.getType());
    }

    @Test
    public void testModifyDataLinks() {
        addFeatureType();

        FeatureTypeInfo ft2 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        DataLinkInfo ml = rawCatalog.getFactory().createDataLink();
        ml.setContent("http://www.geoserver.org/meta");
        ml.setType("text/plain");
        ft2.getDataLinks().clear();
        ft2.getDataLinks().add(ml);
        rawCatalog.save(ft2);

        FeatureTypeInfo ft3 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        DataLinkInfo ml3 = ft3.getDataLinks().get(0);
        ml3.setType("application/json");

        // do not save and grab another, the metadata link must not have been modified
        FeatureTypeInfo ft4 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        DataLinkInfo ml4 = ft4.getDataLinks().get(0);
        assertEquals("text/plain", ml4.getType());

        // now save and grab yet another, the modification must have happened
        rawCatalog.save(ft3);
        FeatureTypeInfo ft5 = rawCatalog.getFeatureTypeByName(data.featureTypeA.getName());
        DataLinkInfo ml5 = ft5.getDataLinks().get(0);
        assertEquals("application/json", ml5.getType());
    }

    @Test
    public void testFeatureTypeEvents() {
        // set default namespace
        addNamespace();
        addDataStore();

        TestListener l = new TestListener();
        rawCatalog.addListener(l);

        FeatureTypeInfo ft = rawCatalog.getFactory().createFeatureType();
        ft.setName("ftName");
        ft.setDescription("ftDescription");
        ft.setStore(data.dataStoreA);

        assertTrue(l.added.isEmpty());
        rawCatalog.add(ft);

        assertEquals(1, l.added.size());
        assertEquals(ft, l.added.get(0).getSource());

        ft = rawCatalog.getFeatureTypeByName("ftName");
        ft.setDescription("changed");
        assertTrue(l.modified.isEmpty());
        rawCatalog.save(ft);
        assertEquals(1, l.modified.size());
        assertEquals(ft, l.modified.get(0).getSource());
        assertTrue(l.modified.get(0).getPropertyNames().contains("description"));
        assertTrue(l.modified.get(0).getOldValues().contains("ftDescription"));
        assertTrue(l.modified.get(0).getNewValues().contains("changed"));
        assertEquals(1, l.modified.size());
        assertEquals(ft, l.postModified.get(0).getSource());
        assertTrue(l.postModified.get(0).getPropertyNames().contains("description"));
        assertTrue(l.postModified.get(0).getOldValues().contains("ftDescription"));
        assertTrue(l.postModified.get(0).getNewValues().contains("changed"));

        assertTrue(l.removed.isEmpty());
        rawCatalog.remove(ft);
        assertEquals(1, l.removed.size());
        assertEquals(ft, l.removed.get(0).getSource());
    }

    @Test
    public void testModifyMetadata() {
        // set default namespace
        addNamespace();
        addDataStore();

        TestListener l = new TestListener();
        rawCatalog.addListener(l);

        FeatureTypeInfo ft = rawCatalog.getFactory().createFeatureType();
        ft.setName("ftName");
        ft.setDescription("ftDescription");
        ft.setStore(data.dataStoreA);

        assertTrue(l.added.isEmpty());
        rawCatalog.add(ft);

        assertEquals(1, l.added.size());
        assertEquals(ft, l.added.get(0).getSource());

        ft = rawCatalog.getFeatureTypeByName("ftName");
        ft.getMetadata().put("newValue", "abcd");
        MetadataMap newMetadata = new MetadataMap(ft.getMetadata());
        rawCatalog.save(ft);
        assertEquals(1, l.modified.size());
        assertEquals(ft, l.modified.get(0).getSource());
        assertTrue(l.modified.get(0).getPropertyNames().contains("metadata"));
        assertTrue(l.modified.get(0).getOldValues().contains(new MetadataMap()));
        assertTrue(l.modified.get(0).getNewValues().contains(newMetadata));
    }

    @Test
    public void testAddLayer() {
        assertTrue(rawCatalog.getLayers().isEmpty());
        addLayer();

        assertEquals(1, rawCatalog.getLayers().size());

        LayerInfo l2 = rawCatalog.getFactory().createLayer();
        try {
            rawCatalog.add(l2);
            fail("adding with no name should throw exception");
        } catch (Exception e) {
        }

        // l2.setName( "l2" );
        try {
            rawCatalog.add(l2);
            fail("adding with no resource should throw exception");
        } catch (Exception e) {
        }

        l2.setResource(data.featureTypeA);
        // try {
        // catalog.add( l2 );
        // fail( "adding with no default style should throw exception");
        // }
        // catch( Exception e) {}
        //
        l2.setDefaultStyle(data.style1);

        try {
            rawCatalog.add(l2);
            fail(
                    "Adding a second layer for the same resource should throw exception, layer name is tied to resource name and would end up with two layers named the same or a broken catalog");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("already exists"));
        }

        assertEquals(1, rawCatalog.getLayers().size());
    }

    @Test
    public void testGetLayerById() {
        addLayer();

        LayerInfo l2 = rawCatalog.getLayer(data.layerFeatureTypeA.getId());
        assertNotNull(l2);
        assertNotSame(data.layerFeatureTypeA, l2);
        assertEquals(data.layerFeatureTypeA, l2);
        assertSame(rawCatalog, l2.getResource().getCatalog());
    }

    @Test
    public void testGetLayerByName() {
        addLayer();

        LayerInfo l2 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        assertNotNull(l2);
        assertNotSame(data.layerFeatureTypeA, l2);
        assertEquals(data.layerFeatureTypeA, l2);
    }

    @Test
    public void testGetLayerByNameWithoutColon() {
        // create two workspaces
        rawCatalog.add(data.namespaceB);
        rawCatalog.add(data.namespaceC);

        rawCatalog.add(data.workspaceB);
        rawCatalog.add(data.workspaceC);

        rawCatalog.setDefaultNamespace(data.namespaceC);
        rawCatalog.setDefaultWorkspace(data.workspaceC);

        rawCatalog.add(data.dataStoreB);
        rawCatalog.add(data.dataStoreC);

        // create three resources, aaa:bar, bbb:bar, aaa:bar2
        FeatureTypeInfo ftA = rawCatalog.getFactory().createFeatureType();
        ftA.setEnabled(true);
        ftA.setName("bar");
        ftA.setAbstract("ftAbstract");
        ftA.setDescription("ftDescription");
        ftA.setStore(data.dataStoreB);
        ftA.setNamespace(data.namespaceB);

        FeatureTypeInfo ftB = rawCatalog.getFactory().createFeatureType();
        ftB.setName("bar");
        ftB.setAbstract("ftAbstract");
        ftB.setDescription("ftDescription");
        ftB.setStore(data.dataStoreC);
        ftB.setNamespace(data.namespaceC);

        FeatureTypeInfo ftC = rawCatalog.getFactory().createFeatureType();
        ftC.setName("bar2");
        ftC.setAbstract("ftAbstract");
        ftC.setDescription("ftDescription");
        ftC.setStore(data.dataStoreB);
        ftC.setNamespace(data.namespaceB);
        ftC.setEnabled(true);
        ftB.setEnabled(true);

        rawCatalog.add(ftA);
        rawCatalog.add(ftB);
        rawCatalog.add(ftC);

        addStyle();

        LayerInfo lA = rawCatalog.getFactory().createLayer();
        lA.setResource(ftA);
        lA.setDefaultStyle(data.style1);
        lA.setEnabled(true);

        LayerInfo lB = rawCatalog.getFactory().createLayer();
        lB.setResource(ftB);
        lB.setDefaultStyle(data.style1);
        lB.setEnabled(true);

        LayerInfo lC = rawCatalog.getFactory().createLayer();
        lC.setResource(ftC);
        lC.setDefaultStyle(data.style1);
        lC.setEnabled(true);

        rawCatalog.add(lA);
        rawCatalog.add(lB);
        rawCatalog.add(lC);

        // this search should give us back the bar in the default worksapce
        LayerInfo searchedResult = rawCatalog.getLayerByName("bar");
        assertNotNull(searchedResult);
        assertEquals(lB, searchedResult);

        // this search should give us back the bar in the other workspace
        searchedResult = rawCatalog.getLayerByName("aaa:bar");
        assertNotNull(searchedResult);
        assertEquals(lA, searchedResult);

        // unqualified, it should give us the only bar2 available
        searchedResult = rawCatalog.getLayerByName("bar2");
        assertNotNull(searchedResult);
        assertEquals(lC, searchedResult);

        // qualified should work the same
        searchedResult = rawCatalog.getLayerByName("aaa:bar2");
        assertNotNull(searchedResult);
        assertEquals(lC, searchedResult);

        // with the wrong workspace, should give us nothing
        searchedResult = rawCatalog.getLayerByName("bbb:bar2");
        assertNull(searchedResult);
    }

    @Test
    public void testGetLayerByNameWithColon() {
        addNamespace();
        addDataStore();
        FeatureTypeInfo ft = rawCatalog.getFactory().createFeatureType();
        ft.setEnabled(true);
        ft.setName("foo:bar");
        ft.setAbstract("ftAbstract");
        ft.setDescription("ftDescription");
        ft.setStore(data.dataStoreA);
        ft.setNamespace(data.namespaceA);
        rawCatalog.add(ft);

        addStyle();
        LayerInfo l = rawCatalog.getFactory().createLayer();
        l.setResource(ft);
        l.setEnabled(true);
        l.setDefaultStyle(data.style1);

        rawCatalog.add(l);
        assertNotNull(rawCatalog.getLayerByName("foo:bar"));
    }

    @Test
    public void testGetLayerByResource() {
        addLayer();

        List<LayerInfo> layers = rawCatalog.getLayers(data.featureTypeA);
        assertEquals(1, layers.size());
        LayerInfo l2 = layers.get(0);

        assertNotSame(data.layerFeatureTypeA, l2);
        assertEquals(data.layerFeatureTypeA, l2);
    }

    @Test
    public void testRemoveLayer() {
        addLayer();
        assertEquals(1, rawCatalog.getLayers().size());

        rawCatalog.remove(data.layerFeatureTypeA);
        assertTrue(rawCatalog.getLayers().isEmpty());
    }

    @Test
    public void testRemoveLayerAndAssociatedDataRules() throws IOException {
        DataAccessRuleDAO dao = this.dataAccessRuleDAO;
        CatalogListener listener = new SecuredResourceNameChangeListener(rawCatalog, dao);
        addLayer();
        assertEquals(1, rawCatalog.getLayers().size());

        String workspaceName =
                data.layerFeatureTypeA.getResource().getStore().getWorkspace().getName();
        addLayerAccessRule(workspaceName, data.layerFeatureTypeA.getName(), AccessMode.WRITE, "*");
        assertTrue(layerHasSecurityRule(dao, workspaceName, data.layerFeatureTypeA.getName()));
        rawCatalog.remove(data.layerFeatureTypeA);
        assertTrue(rawCatalog.getLayers().isEmpty());
        dao.reload();
        assertFalse(layerHasSecurityRule(dao, workspaceName, data.layerFeatureTypeA.getName()));
        rawCatalog.removeListener(listener);
    }

    private boolean layerHasSecurityRule(
            DataAccessRuleDAO dao, String workspaceName, String layerName) {

        List<DataAccessRule> rules = dao.getRules();
        for (DataAccessRule rule : rules) {
            if (rule.getRoot().equalsIgnoreCase(workspaceName)
                    && rule.getLayer().equalsIgnoreCase(layerName)) return true;
        }

        return false;
    }

    @Test
    public void testModifyLayer() {
        addLayer();

        LayerInfo l2 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        // l2.setName( null );
        l2.setResource(null);

        LayerInfo l3 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        assertEquals(data.layerFeatureTypeA.getName(), l3.getName());

        // try {
        // catalog.save(l2);
        // fail( "setting name to null should throw exception");
        // }
        // catch( Exception e ) {}
        //
        // l2.setName( "changed" );
        try {
            rawCatalog.save(l2);
            fail("setting resource to null should throw exception");
        } catch (Exception e) {
        }

        l2.setResource(data.featureTypeA);
        rawCatalog.save(l2);

        // TODO: reinstate with resource/publishing split done
        // l3 = catalog.getLayerByName( "changed" );
        l3 = rawCatalog.getLayerByName(data.featureTypeA.getName());
        assertNotNull(l3);
    }

    @Test
    public void testModifyLayerDefaultStyle() {
        // create new style
        CatalogFactory factory = rawCatalog.getFactory();
        StyleInfo s2 = factory.createStyle();
        s2.setName("styleName2");
        s2.setFilename("styleFilename2");
        rawCatalog.add(s2);

        // change the layer style
        addLayer();
        LayerInfo l2 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        l2.setDefaultStyle(rawCatalog.getStyleByName("styleName2"));
        rawCatalog.save(l2);

        // get back and compare with itself
        LayerInfo l3 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        LayerInfo l4 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        assertEquals(l3, l4);
    }

    @Test
    public void testEnableLayer() {
        addLayer();

        LayerInfo l2 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        assertTrue(l2.isEnabled());
        assertTrue(l2.enabled());
        assertTrue(l2.getResource().isEnabled());

        l2.setEnabled(false);
        rawCatalog.save(l2);
        // GR: if not saving also the associated resource, we're assuming saving the layer also
        // saves its ResourceInfo, which is wrong, but works on the in-memory catalog by accident
        rawCatalog.save(l2.getResource());

        l2 = rawCatalog.getLayerByName(l2.getName());
        assertFalse(l2.isEnabled());
        assertFalse(l2.enabled());
        assertFalse(l2.getResource().isEnabled());
    }

    @Test
    public void testLayerEvents() {
        addFeatureType();
        addStyle();

        TestListener tl = new TestListener();
        rawCatalog.addListener(tl);

        assertTrue(tl.added.isEmpty());
        rawCatalog.add(data.layerFeatureTypeA);
        assertEquals(1, tl.added.size());
        assertEquals(data.layerFeatureTypeA, tl.added.get(0).getSource());

        LayerInfo l2 = rawCatalog.getLayerByName(data.layerFeatureTypeA.getName());
        l2.setPath("newPath");

        assertTrue(tl.modified.isEmpty());
        rawCatalog.save(l2);
        assertEquals(1, tl.modified.size());
        assertEquals(l2, tl.modified.get(0).getSource());
        assertTrue(tl.modified.get(0).getPropertyNames().contains("path"));
        assertTrue(tl.modified.get(0).getOldValues().contains(null));
        assertTrue(tl.modified.get(0).getNewValues().contains("newPath"));
        assertEquals(1, tl.postModified.size());
        assertEquals(l2, tl.postModified.get(0).getSource());
        assertTrue(tl.postModified.get(0).getPropertyNames().contains("path"));
        assertTrue(tl.postModified.get(0).getOldValues().contains(null));
        assertTrue(tl.postModified.get(0).getNewValues().contains("newPath"));

        assertTrue(tl.removed.isEmpty());
        rawCatalog.remove(l2);
        assertEquals(1, tl.removed.size());
        assertEquals(l2, tl.removed.get(0).getSource());
    }

    @Test
    public void testAddStyle() {
        assertTrue(rawCatalog.getStyles().isEmpty());

        addStyle();
        assertEquals(1, rawCatalog.getStyles().size());

        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        try {
            rawCatalog.add(s2);
            fail("adding without name should throw exception");
        } catch (Exception e) {
        }

        s2.setName("s2Name");
        try {
            rawCatalog.add(s2);
            fail("adding without fileName should throw exception");
        } catch (Exception e) {
        }

        s2.setFilename("s2Filename");
        try {
            rawCatalog.getStyles().add(s2);
            fail("adding directly should throw exception");
        } catch (Exception e) {
        }

        rawCatalog.add(s2);
        assertEquals(2, rawCatalog.getStyles().size());
    }

    @Test
    public void testAddStyleWithNameConflict() throws Exception {
        addWorkspace();
        addStyle();

        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName(data.style1.getName());
        s2.setFilename(data.style1.getFilename());

        try {
            rawCatalog.add(s2);
            fail("Shoudl have failed with existing global style with same name");
        } catch (IllegalArgumentException expected) {
        }

        List<StyleInfo> currStyles = rawCatalog.getStyles();

        // should pass after setting workspace
        s2.setWorkspace(data.workspaceA);
        rawCatalog.add(s2);

        assertFalse(
                new HashSet<StyleInfo>(currStyles)
                        .equals(new HashSet<StyleInfo>(rawCatalog.getStyles())));

        StyleInfo s3 = rawCatalog.getFactory().createStyle();
        s3.setName(s2.getName());
        s3.setFilename(s2.getFilename());

        try {
            rawCatalog.add(s3);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        s3.setWorkspace(data.workspaceA);
        try {
            rawCatalog.add(s3);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetStyleById() {
        addStyle();

        StyleInfo s2 = rawCatalog.getStyle(data.style1.getId());
        assertNotNull(s2);
        assertNotSame(data.style1, s2);
        assertEquals(data.style1, s2);
    }

    @Test
    public void testGetStyleByName() {
        addStyle();

        StyleInfo s2 = rawCatalog.getStyleByName(data.style1.getName());
        assertNotNull(s2);
        assertNotSame(data.style1, s2);
        assertEquals(data.style1, s2);
    }

    @Test
    public void testGetStyleByNameWithWorkspace() {
        addWorkspace();
        addStyle();

        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName("styleNameWithWorkspace");
        s2.setFilename("styleFilenameWithWorkspace");
        s2.setWorkspace(data.workspaceA);
        rawCatalog.add(s2);

        assertNotNull(rawCatalog.getStyleByName("styleNameWithWorkspace"));
        assertNotNull(
                rawCatalog.getStyleByName(data.workspaceA.getName(), "styleNameWithWorkspace"));
        assertNotNull(rawCatalog.getStyleByName(data.workspaceA, "styleNameWithWorkspace"));
        assertNull(rawCatalog.getStyleByName((WorkspaceInfo) null, "styleNameWithWorkspace"));

        assertNull(rawCatalog.getStyleByName(data.workspaceA.getName(), "style1"));
        assertNull(rawCatalog.getStyleByName(data.workspaceA, "style1"));
        assertNotNull(rawCatalog.getStyleByName((WorkspaceInfo) null, "style1"));
    }

    @Test
    public void testGetStyleByNameWithWorkspace2() throws Exception {
        addWorkspace();

        WorkspaceInfo ws2 = rawCatalog.getFactory().createWorkspace();
        ws2.setName("wsName2");
        rawCatalog.add(ws2);

        // add style with same name in each workspace
        StyleInfo s1 = rawCatalog.getFactory().createStyle();
        s1.setName("foo");
        s1.setFilename("foo1.sld");
        s1.setWorkspace(data.workspaceA);
        rawCatalog.add(s1);

        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName("foo");
        s2.setFilename("foo2.sld");
        s2.setWorkspace(ws2);
        rawCatalog.add(s2);

        assertEquals(s1, rawCatalog.getStyleByName("foo"));

        assertEquals(s1, rawCatalog.getStyleByName(data.workspaceA.getName(), "foo"));
        assertEquals(s1, rawCatalog.getStyleByName(data.workspaceA, "foo"));

        assertEquals(s2, rawCatalog.getStyleByName(ws2.getName(), "foo"));
        assertEquals(s2, rawCatalog.getStyleByName(ws2, "foo"));
    }

    @Test
    public void testGetStyles() {
        addWorkspace();
        addStyle();

        assertEquals(1, rawCatalog.getStyles().size());
        assertEquals(0, rawCatalog.getStylesByWorkspace(data.workspaceA.getName()).size());
        assertEquals(0, rawCatalog.getStylesByWorkspace(data.workspaceA).size());
        assertEquals(0, rawCatalog.getStylesByWorkspace((WorkspaceInfo) null).size());

        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName("styleNameWithWorkspace");
        s2.setFilename("styleFilenameWithWorkspace");
        s2.setWorkspace(data.workspaceA);
        rawCatalog.add(s2);

        assertEquals(2, rawCatalog.getStyles().size());
        assertEquals(1, rawCatalog.getStylesByWorkspace(data.workspaceA.getName()).size());
        assertEquals(1, rawCatalog.getStylesByWorkspace(data.workspaceA).size());
        assertEquals(1, rawCatalog.getStylesByWorkspace((WorkspaceInfo) null).size());
    }

    @Test
    public void testModifyStyle() {
        addStyle();

        StyleInfo s2 = rawCatalog.getStyleByName(data.style1.getName());
        s2.setName(null);
        s2.setFilename(null);

        StyleInfo s3 = rawCatalog.getStyleByName(data.style1.getName());
        assertEquals(data.style1, s3);

        try {
            rawCatalog.save(s2);
            fail("setting name to null should fail");
        } catch (Exception e) {
        }

        s2.setName(data.style1.getName());
        try {
            rawCatalog.save(s2);
            fail("setting filename to null should fail");
        } catch (Exception e) {
        }

        s2.setName("s2Name");
        s2.setFilename("s2Name.sld");
        rawCatalog.save(s2);

        s3 = rawCatalog.getStyleByName("style1");
        assertNull(s3);

        s3 = rawCatalog.getStyleByName(s2.getName());
        assertEquals(s2, s3);
    }

    @Test
    public void testModifyDefaultStyle() {
        addWorkspace();
        addDefaultStyle();
        StyleInfo s = rawCatalog.getStyleByName(StyleInfo.DEFAULT_LINE);

        s.setName("foo");

        try {
            rawCatalog.save(s);
            fail("changing name of default style should fail");
        } catch (Exception e) {
        }

        s = rawCatalog.getStyleByName(StyleInfo.DEFAULT_LINE);
        s.setWorkspace(data.workspaceA);
        try {
            rawCatalog.save(s);
            fail("changing workspace of default style should fail");
        } catch (Exception e) {
        }
    }

    @Test
    public void testRemoveStyle() {
        addStyle();
        assertEquals(1, rawCatalog.getStyles().size());

        rawCatalog.remove(data.style1);
        assertTrue(rawCatalog.getStyles().isEmpty());
    }

    @Test
    public void testRemoveDefaultStyle() {
        addWorkspace();
        addDefaultStyle();
        StyleInfo s = rawCatalog.getStyleByName(StyleInfo.DEFAULT_LINE);

        try {
            rawCatalog.remove(s);
            fail("removing default style should fail");
        } catch (Exception e) {
        }
    }

    @Test
    public void testStyleEvents() {
        TestListener l = new TestListener();
        rawCatalog.addListener(l);

        assertTrue(l.added.isEmpty());
        rawCatalog.add(data.style1);
        assertEquals(1, l.added.size());
        assertEquals(data.style1, l.added.get(0).getSource());

        StyleInfo s2 = rawCatalog.getStyleByName(data.style1.getName());
        s2.setFilename("changed");

        assertTrue(l.modified.isEmpty());
        assertTrue(l.postModified.isEmpty());
        rawCatalog.save(s2);
        assertEquals(1, l.modified.size());
        assertEquals(s2, l.modified.get(0).getSource());
        assertTrue(l.modified.get(0).getPropertyNames().contains("filename"));
        assertTrue(l.modified.get(0).getOldValues().contains("styleFilename"));
        assertTrue(l.modified.get(0).getNewValues().contains("changed"));
        assertEquals(1, l.postModified.size());
        assertEquals(s2, l.postModified.get(0).getSource());
        assertTrue(l.postModified.get(0).getPropertyNames().contains("filename"));
        assertTrue(l.postModified.get(0).getOldValues().contains("styleFilename"));
        assertTrue(l.postModified.get(0).getNewValues().contains("changed"));

        assertTrue(l.removed.isEmpty());
        rawCatalog.remove(s2);
        assertEquals(1, l.removed.size());
        assertEquals(s2, l.removed.get(0).getSource());
    }

    @Test
    public void testProxyBehaviour() throws Exception {
        testAddLayer();

        // l = catalog.getLayerByName( "layerName");
        LayerInfo l = rawCatalog.getLayerByName(data.featureTypeA.getName());
        assertTrue(l instanceof Proxy);

        ResourceInfo r = l.getResource();
        assertTrue(r instanceof Proxy);

        String oldName = data.featureTypeA.getName();
        r.setName("changed");
        rawCatalog.save(r);

        assertNull(rawCatalog.getLayerByName(oldName));
        l = rawCatalog.getLayerByName(r.getName());
        assertNotNull(l);
        assertEquals("changed", l.getResource().getName());
    }

    @Test
    public void testProxyListBehaviour() throws Exception {
        rawCatalog.add(data.style1);

        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName("a" + data.style1.getName());
        s2.setFilename("a.sld");
        rawCatalog.add(s2);

        List<StyleInfo> styles = rawCatalog.getStyles();
        assertEquals(2, styles.size());

        // test immutability
        Comparator<StyleInfo> comparator =
                new Comparator<StyleInfo>() {

                    public int compare(StyleInfo o1, StyleInfo o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                };
        try {
            Collections.sort(styles, comparator);
            fail("Expected runtime exception, immutable collection");
        } catch (RuntimeException e) {
            assertTrue(true);
        }

        styles = new ArrayList<StyleInfo>(styles);
        Collections.sort(styles, comparator);

        assertEquals("a" + data.style1.getName(), styles.get(0).getName());
        assertEquals(data.style1.getName(), styles.get(1).getName());
    }

    @Test
    public void testExceptionThrowingListener() throws Exception {
        ExceptionThrowingListener l = new ExceptionThrowingListener();
        rawCatalog.addListener(l);

        l.throwCatalogException = false;

        WorkspaceInfo ws = rawCatalog.getFactory().createWorkspace();
        ws.setName("foo");

        // no exception thrown back
        rawCatalog.add(ws);

        l.throwCatalogException = true;
        ws = rawCatalog.getFactory().createWorkspace();
        ws.setName("bar");

        try {
            rawCatalog.add(ws);
            fail();
        } catch (CatalogException ce) {
            // good
        }
    }

    @Test
    public void testAddWMSStore() {
        assertTrue(rawCatalog.getStores(WMSStoreInfo.class).isEmpty());
        addWMSStore();
        assertEquals(1, rawCatalog.getStores(WMSStoreInfo.class).size());

        WMSStoreInfo retrieved = rawCatalog.getStore(data.wmsStoreA.getId(), WMSStoreInfo.class);
        assertNotNull(retrieved);
        assertSame(rawCatalog, retrieved.getCatalog());

        WMSStoreInfo wms2 = rawCatalog.getFactory().createWebMapServer();
        wms2.setName("wms2Name");
        wms2.setWorkspace(data.workspaceA);

        rawCatalog.add(wms2);
        assertEquals(2, rawCatalog.getStores(WMSStoreInfo.class).size());
    }

    @Test
    public void testAddWMTSStore() {
        assertTrue(rawCatalog.getStores(WMTSStoreInfo.class).isEmpty());
        addWMTSStore();
        assertEquals(1, rawCatalog.getStores(WMTSStoreInfo.class).size());

        WMTSStoreInfo retrieved = rawCatalog.getStore(data.wmtsStoreA.getId(), WMTSStoreInfo.class);
        assertNotNull(retrieved);
        assertSame(rawCatalog, retrieved.getCatalog());

        WMTSStoreInfo wmts2 = rawCatalog.getFactory().createWebMapTileServer();
        wmts2.setName("wmts2Name");
        wmts2.setWorkspace(data.workspaceA);

        rawCatalog.add(wmts2);
        assertEquals(2, rawCatalog.getStores(WMTSStoreInfo.class).size());
    }

    protected int GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_TEST_COUNT = 500;
    private static final int GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_THREAD_COUNT = 10;

    /**
     * This test cannot work, the catalog subsystem is not thread safe, that's why we have the
     * configuration locks. Re-enable when the catalog subsystem is made thread safe.
     */
    @Test
    @Ignore
    public void testGetLayerByIdWithConcurrentAdd() throws Exception {
        addDataStore();
        addNamespace();
        addStyle();
        rawCatalog.add(data.featureTypeA);

        LayerInfo layer = rawCatalog.getFactory().createLayer();
        layer.setResource(data.featureTypeA);
        rawCatalog.add(layer);
        String id = layer.getId();

        CountDownLatch ready =
                new CountDownLatch(GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_THREAD_COUNT + 1);
        CountDownLatch done = new CountDownLatch(GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_THREAD_COUNT);

        List<RunnerBase> runners = new ArrayList<RunnerBase>();
        for (int i = 0; i < GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_THREAD_COUNT; i++) {
            RunnerBase runner = new LayerAddRunner(ready, done, i);
            new Thread(runner).start();
            runners.add(runner);
        }

        // note that test thread is ready
        ready.countDown();
        // wait for all threads to reach latch in order to maximize likelihood of contention
        ready.await();

        for (int i = 0; i < GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_TEST_COUNT; i++) {
            rawCatalog.getLayer(id);
        }

        // make sure worker threads are done
        done.await();

        RunnerBase.checkForRunnerExceptions(runners);
    }

    @Test
    public void testAddLayerGroupNameConflict() throws Exception {
        addLayerGroup();

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();

        lg2.setName("layerGroup");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        try {
            rawCatalog.add(lg2);
            fail("should have failed because same name and no workspace set");
        } catch (IllegalArgumentException expected) {
        }

        // setting a workspace shluld pass
        lg2.setWorkspace(data.workspaceA);
        rawCatalog.add(lg2);
    }

    @Test
    public void testAddLayerGroupWithWorkspaceWithResourceFromAnotherWorkspace() {
        WorkspaceInfo ws = rawCatalog.getFactory().createWorkspace();
        ws.setName("other");
        rawCatalog.add(ws);

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setWorkspace(ws);
        lg2.setName("layerGroup2");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        try {
            rawCatalog.add(lg2);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetLayerGroupByName() {
        addLayerGroup();
        assertNotNull(rawCatalog.getLayerGroupByName("layerGroup"));
        assertNotNull(rawCatalog.getLayerGroupByName((WorkspaceInfo) null, "layerGroup"));
        assertNull(rawCatalog.getLayerGroupByName(rawCatalog.getDefaultWorkspace(), "layerGroup"));

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setWorkspace(data.workspaceA);
        assertEquals(data.workspaceA, rawCatalog.getDefaultWorkspace());
        lg2.setName("layerGroup2");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        rawCatalog.add(lg2);

        // When in the default workspace, we should be able to find it without the prefix
        assertNotNull(rawCatalog.getLayerGroupByName("layerGroup2"));
        assertNotNull(rawCatalog.getLayerGroupByName(data.workspaceA.getName() + ":layerGroup2"));
        assertNotNull(
                rawCatalog.getLayerGroupByName(rawCatalog.getDefaultWorkspace(), "layerGroup2"));
        assertNull(rawCatalog.getLayerGroupByName("cite", "layerGroup2"));

        // Repeat in a non-default workspace
        WorkspaceInfo ws2 = rawCatalog.getFactory().createWorkspace();
        ws2.setName("ws2");
        rawCatalog.add(ws2);
        rawCatalog.setDefaultWorkspace(ws2);

        assertNull(
                "layerGroup2 is not global, should not be found",
                rawCatalog.getLayerGroupByName("layerGroup2"));
        assertNotNull(rawCatalog.getLayerGroupByName(data.workspaceA.getName() + ":layerGroup2"));
        assertNotNull(rawCatalog.getLayerGroupByName(data.workspaceA, "layerGroup2"));
        assertNull(rawCatalog.getLayerGroupByName("cite", "layerGroup2"));
    }

    @Test
    public void testRemoveLayerGroupAndAssociatedDataRules() throws IOException {
        DataAccessRuleDAO dao = this.dataAccessRuleDAO;
        CatalogListener listener = new SecuredResourceNameChangeListener(rawCatalog, dao);
        addLayer();
        CatalogFactory factory = rawCatalog.getFactory();
        LayerGroupInfo lg = factory.createLayerGroup();
        String lgName = "MyFakeWorkspace:layerGroup";
        lg.setName(lgName);
        lg.setWorkspace(data.workspaceA);
        lg.getLayers().add(data.layerFeatureTypeA);
        lg.getStyles().add(data.style1);
        rawCatalog.add(lg);
        String workspaceName = data.workspaceA.getName();
        assertNotNull(rawCatalog.getLayerGroupByName(workspaceName, lg.getName()));

        addLayerAccessRule(workspaceName, lg.getName(), AccessMode.WRITE, "*");
        assertTrue(layerHasSecurityRule(dao, workspaceName, lg.getName()));
        rawCatalog.remove(lg);
        assertNull(rawCatalog.getLayerGroupByName(workspaceName, lg.getName()));
        assertFalse(layerHasSecurityRule(dao, workspaceName, lg.getName()));
        rawCatalog.removeListener(listener);
    }

    @Test
    public void testGetLayerGroupByNameWithColon() {
        addLayer();
        CatalogFactory factory = rawCatalog.getFactory();
        LayerGroupInfo lg = factory.createLayerGroup();

        String lgName = "MyFakeWorkspace:layerGroup";
        lg.setName(lgName);
        lg.setWorkspace(data.workspaceA);
        lg.getLayers().add(data.layerFeatureTypeA);
        lg.getStyles().add(data.style1);
        rawCatalog.add(lg);

        // lg is not global, should not be found at least we specify a prefixed name
        assertNull(
                "MyFakeWorkspace:layerGroup is not global, should not be found",
                rawCatalog.getLayerGroupByName(lgName));

        assertEquals(lg, rawCatalog.getLayerGroupByName(data.workspaceA.getName(), lgName));
        assertEquals(lg, rawCatalog.getLayerGroupByName(data.workspaceA, lgName));
        assertEquals(lg, rawCatalog.getLayerGroupByName(data.workspaceA.getName() + ":" + lgName));
    }

    @Test
    public void testGetLayerGroupByNameWithWorkspace() {
        addLayer();
        assertEquals(data.workspaceA, rawCatalog.getDefaultWorkspace());

        CatalogFactory factory = rawCatalog.getFactory();
        LayerGroupInfo lg1 = factory.createLayerGroup();
        lg1.setName("lg");
        lg1.setWorkspace(data.workspaceA);
        lg1.getLayers().add(data.layerFeatureTypeA);
        lg1.getStyles().add(data.style1);
        rawCatalog.add(lg1);

        WorkspaceInfo ws2 = factory.createWorkspace();
        ws2.setName("ws2");
        rawCatalog.add(ws2);

        NamespaceInfo ns2 = factory.createNamespace();
        // namespace prefix shall match workspace name, until we decide it cannot
        ns2.setPrefix("ns2");
        // ns2.setPrefix(ws2.getName());
        ns2.setURI("http://ns2");
        rawCatalog.add(ns2);

        DataStoreInfo ds2 = factory.createDataStore();
        ds2.setEnabled(true);
        ds2.setName("dsName");
        ds2.setDescription("dsDescription");
        ds2.setWorkspace(ws2);
        rawCatalog.add(ds2);

        FeatureTypeInfo ft2 = factory.createFeatureType();
        ft2.setEnabled(true);
        ft2.setName("ftName");
        ft2.setAbstract("ftAbstract");
        ft2.setDescription("ftDescription");
        ft2.setStore(ds2);
        ft2.setNamespace(ns2);
        rawCatalog.add(ft2);

        StyleInfo s2 = factory.createStyle();
        s2.setName("style1");
        s2.setFilename("styleFilename");
        s2.setWorkspace(ws2);
        rawCatalog.add(s2);

        LayerInfo l2 = factory.createLayer();
        l2.setResource(ft2);
        l2.setEnabled(true);
        l2.setDefaultStyle(s2);
        rawCatalog.add(l2);

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setName("lg");
        lg2.setWorkspace(ws2);
        lg2.getLayers().add(l2);
        lg2.getStyles().add(s2);
        rawCatalog.add(lg2);

        // lg is not global, but it is in the default workspace, so it should be found if we don't
        // specify the workspace
        assertEquals(lg1, rawCatalog.getLayerGroupByName("lg"));

        assertEquals(lg1, rawCatalog.getLayerGroupByName(data.workspaceA.getName(), "lg"));
        assertEquals(lg1, rawCatalog.getLayerGroupByName(data.workspaceA, "lg"));
        assertEquals(lg1, rawCatalog.getLayerGroupByName(data.workspaceA.getName() + ":lg"));

        assertEquals(lg2, rawCatalog.getLayerGroupByName(ws2, "lg"));
        assertEquals(lg2, rawCatalog.getLayerGroupByName(ws2, "lg"));
        assertEquals(lg2, rawCatalog.getLayerGroupByName(ws2.getName() + ":lg"));
    }

    @Test
    public void testGetLayerGroups() {
        addLayerGroup();
        assertEquals(1, rawCatalog.getLayerGroups().size());
        assertEquals(0, rawCatalog.getLayerGroupsByWorkspace(data.workspaceA.getName()).size());
        assertEquals(0, rawCatalog.getLayerGroupsByWorkspace(data.workspaceA).size());
        assertEquals(0, rawCatalog.getLayerGroupsByWorkspace((WorkspaceInfo) null).size());

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setWorkspace(rawCatalog.getDefaultWorkspace());
        lg2.setName("layerGroup2");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        rawCatalog.add(lg2);

        assertEquals(2, rawCatalog.getLayerGroups().size());
        assertEquals(1, rawCatalog.getLayerGroupsByWorkspace(data.workspaceA.getName()).size());
        assertEquals(1, rawCatalog.getLayerGroupsByWorkspace(data.workspaceA).size());
        assertEquals(1, rawCatalog.getLayerGroupsByWorkspace((WorkspaceInfo) null).size());
    }

    @Test
    public void testLayerGroupTitle() {
        addLayer();
        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        // lg2.setWorkspace(catalog.getDefaultWorkspace());
        lg2.setName("layerGroup2");
        lg2.setTitle("layerGroup2 title");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        rawCatalog.add(lg2);

        assertEquals(1, rawCatalog.getLayerGroups().size());

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals("layerGroup2 title", lg2.getTitle());

        lg2.setTitle("another title");
        rawCatalog.save(lg2);

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals("another title", lg2.getTitle());
    }

    @Test
    public void testLayerGroupAbstract() {
        addLayer();
        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        // lg2.setWorkspace(catalog.getDefaultWorkspace());
        lg2.setName("layerGroup2");
        lg2.setAbstract("layerGroup2 abstract");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        rawCatalog.add(lg2);

        assertEquals(1, rawCatalog.getLayerGroups().size());

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals("layerGroup2 abstract", lg2.getAbstract());

        lg2.setAbstract("another abstract");
        rawCatalog.save(lg2);

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals("another abstract", lg2.getAbstract());
    }

    @Test
    public void testLayerGroupType() {
        addLayer();
        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setWorkspace(null);
        lg2.setName("layerGroup2");
        lg2.setMode(LayerGroupInfo.Mode.NAMED);
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        rawCatalog.add(lg2);

        assertEquals(1, rawCatalog.getLayerGroups().size());

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals(LayerGroupInfo.Mode.NAMED, lg2.getMode());

        lg2.setMode(LayerGroupInfo.Mode.SINGLE);
        rawCatalog.save(lg2);

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals(LayerGroupInfo.Mode.SINGLE, lg2.getMode());
    }

    @Test
    public void testLayerGroupRootLayer() {
        addLayer();
        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setWorkspace(null);
        lg2.setName("layerGroup2");
        lg2.getLayers().add(data.layerFeatureTypeA);
        lg2.getStyles().add(data.style1);
        lg2.setRootLayer(data.layerFeatureTypeA);

        lg2.setMode(LayerGroupInfo.Mode.SINGLE);
        try {
            rawCatalog.add(lg2);
            fail("only EO layer groups can have a root layer");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        lg2.setMode(LayerGroupInfo.Mode.NAMED);
        try {
            rawCatalog.add(lg2);
            fail("only EO layer groups can have a root layer");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        lg2.setMode(LayerGroupInfo.Mode.CONTAINER);
        try {
            rawCatalog.add(lg2);
            fail("only EO layer groups can have a root layer");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        lg2.setMode(LayerGroupInfo.Mode.EO);
        lg2.setRootLayer(null);
        try {
            rawCatalog.add(lg2);
            fail("EO layer groups must have a root layer");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        lg2.setRootLayer(data.layerFeatureTypeA);
        try {
            rawCatalog.add(lg2);
            fail("EO layer groups must have a root layer style");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        lg2.setRootLayerStyle(data.style1);

        rawCatalog.add(lg2);
        assertEquals(1, rawCatalog.getLayerGroups().size());

        lg2 = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals(LayerGroupInfo.Mode.EO, lg2.getMode());
        assertEquals(data.layerFeatureTypeA, lg2.getRootLayer());
        assertEquals(data.style1, lg2.getRootLayerStyle());
    }

    @Test
    public void testLayerGroupNullLayerReferences() {
        addLayer();
        LayerGroupInfo lg = rawCatalog.getFactory().createLayerGroup();
        lg.setWorkspace(null);
        lg.setName("layerGroup2");
        lg.getLayers().add(null);
        lg.getStyles().add(null);
        lg.getLayers().add(data.layerFeatureTypeA);
        lg.getStyles().add(data.style1);
        lg.getLayers().add(null);
        lg.getStyles().add(null);

        rawCatalog.add(lg);
        LayerGroupInfo resolved = rawCatalog.getLayerGroupByName("layerGroup2");
        assertEquals(1, resolved.layers().size());
        assertEquals(1, resolved.styles().size());
        assertEquals(data.style1, resolved.styles().get(0));
    }

    @Test
    public void testLayerGroupRenderingLayers() {
        addDataStore();
        addNamespace();
        FeatureTypeInfo ft1, ft2, ft3;
        rawCatalog.add(ft1 = newFeatureType("ft1", data.dataStoreA));
        rawCatalog.add(ft2 = newFeatureType("ft2", data.dataStoreA));
        rawCatalog.add(ft3 = newFeatureType("ft3", data.dataStoreA));

        StyleInfo s1, s2, s3;
        rawCatalog.add(s1 = newStyle("s1", "s1Filename"));
        rawCatalog.add(s2 = newStyle("s2", "s2Filename"));
        rawCatalog.add(s3 = newStyle("s3", "s3Filename"));

        LayerInfo l1, l2, l3;
        rawCatalog.add(l1 = newLayer(ft1, s1));
        rawCatalog.add(l2 = newLayer(ft2, s2));
        rawCatalog.add(l3 = newLayer(ft3, s3));

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();
        lg2.setWorkspace(rawCatalog.getDefaultWorkspace());
        lg2.setName("layerGroup2");
        lg2.getLayers().add(l1);
        lg2.getLayers().add(l2);
        lg2.getLayers().add(l3);
        lg2.getStyles().add(s1);
        lg2.getStyles().add(s2);
        lg2.getStyles().add(s3);

        lg2.setRootLayer(data.layerFeatureTypeA);
        lg2.setRootLayerStyle(data.style1);

        lg2.setMode(LayerGroupInfo.Mode.SINGLE);
        assertEquals(lg2.getLayers(), lg2.layers());
        assertEquals(lg2.getStyles(), lg2.styles());

        lg2.setMode(LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        assertEquals(lg2.getLayers(), lg2.layers());
        assertEquals(lg2.getStyles(), lg2.styles());

        lg2.setMode(LayerGroupInfo.Mode.NAMED);
        assertEquals(lg2.getLayers(), lg2.layers());
        assertEquals(lg2.getStyles(), lg2.styles());

        lg2.setMode(LayerGroupInfo.Mode.CONTAINER);
        try {
            assertEquals(lg2.getLayers(), lg2.layers());
            fail("Layer group of Type Container can not be rendered");
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
        try {
            assertEquals(lg2.getStyles(), lg2.styles());
            fail("Layer group of Type Container can not be rendered");
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }

        lg2.setMode(LayerGroupInfo.Mode.EO);
        assertEquals(1, lg2.layers().size());
        assertEquals(1, lg2.styles().size());
        assertEquals(data.layerFeatureTypeA, lg2.layers().iterator().next());
        assertEquals(data.style1, lg2.styles().iterator().next());
    }

    @Test
    public void testRemoveLayerGroupInLayerGroup() throws Exception {
        addLayerGroup();

        LayerGroupInfo lg2 = rawCatalog.getFactory().createLayerGroup();

        lg2.setName("layerGroup2");
        lg2.getLayers().add(data.layerGroup1);
        lg2.getStyles().add(data.style1);
        rawCatalog.add(lg2);

        try {
            rawCatalog.remove(data.layerGroup1);
            fail("should have failed because lg is in another lg");
        } catch (IllegalArgumentException expected) {
        }

        // removing the containing layer first should work
        rawCatalog.remove(lg2);
        rawCatalog.remove(data.layerGroup1);
    }

    protected static class TestListener implements CatalogListener {
        public List<CatalogAddEvent> added = new CopyOnWriteArrayList<>();
        public List<CatalogModifyEvent> modified = new CopyOnWriteArrayList<>();
        public List<CatalogPostModifyEvent> postModified = new CopyOnWriteArrayList<>();
        public List<CatalogRemoveEvent> removed = new CopyOnWriteArrayList<>();

        public void handleAddEvent(CatalogAddEvent event) {
            added.add(event);
        }

        public void handleModifyEvent(CatalogModifyEvent event) {
            modified.add(event);
        }

        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            postModified.add(event);
        }

        public void handleRemoveEvent(CatalogRemoveEvent event) {
            removed.add(event);
        }

        public void reloaded() {}
    }

    protected static class ExceptionThrowingListener implements CatalogListener {

        public boolean throwCatalogException;

        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            if (throwCatalogException) {
                throw new CatalogException("expected");
            } else {
                throw new RuntimeException("expected");
            }
        }

        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {}

        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {}

        public void reloaded() {}
    }

    class LayerAddRunner extends RunnerBase {

        private int idx;

        protected LayerAddRunner(CountDownLatch ready, CountDownLatch done, int idx) {
            super(ready, done);
            this.idx = idx;
        }

        protected void runInternal() throws Exception {
            CatalogFactory factory = rawCatalog.getFactory();
            for (int i = 0; i < GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_TEST_COUNT; i++) {
                // GR: Adding a new feature type info too, we can't really add multiple layers per
                // feature type yet. Setting the name of the layer changes the name of the resource,
                // then all previous layers for that resource get screwed
                String name = "LAYER-" + i + "-" + idx;
                FeatureTypeInfo resource = factory.createFeatureType();
                resource.setName(name);
                resource.setNamespace(data.namespaceA);
                resource.setStore(data.dataStoreA);
                rawCatalog.add(resource);

                LayerInfo layer = factory.createLayer();
                layer.setResource(resource);
                layer.setName(name);
                rawCatalog.add(layer);
            }
        }
    };

    @Test
    public void testGet() {
        addDataStore();
        addNamespace();

        FeatureTypeInfo ft1 = newFeatureType("ft1", data.dataStoreA);
        ft1.getKeywords().add(new Keyword("kw1_ft1"));
        ft1.getKeywords().add(new Keyword("kw2_ft1"));
        ft1.getKeywords().add(new Keyword("repeatedKw"));

        FeatureTypeInfo ft2 = newFeatureType("ft2", data.dataStoreA);
        ft2.getKeywords().add(new Keyword("kw1_ft2"));
        ft2.getKeywords().add(new Keyword("kw2_ft2"));
        ft2.getKeywords().add(new Keyword("repeatedKw"));

        rawCatalog.add(ft1);
        rawCatalog.add(ft2);

        StyleInfo s1, s2, s3;
        rawCatalog.add(s1 = newStyle("s1", "s1Filename"));
        rawCatalog.add(s2 = newStyle("s2", "s2Filename"));
        rawCatalog.add(s3 = newStyle("s3", "s3Filename"));

        LayerInfo l1 = newLayer(ft1, s1, s2, s3);
        LayerInfo l2 = newLayer(ft2, s2, s1, s3);
        rawCatalog.add(l1);
        rawCatalog.add(l2);

        Filter filter = acceptAll();
        try {
            rawCatalog.get(null, filter);
            fail("Expected precondition validation exception");
        } catch (RuntimeException nullCheck) {
            assertTrue(true);
        }
        try {
            rawCatalog.get(FeatureTypeInfo.class, null);
            fail("Expected precondition validation exception");
        } catch (RuntimeException nullCheck) {
            assertTrue(true);
        }

        try {
            rawCatalog.get(FeatureTypeInfo.class, filter);
            fail("Expected IAE on multiple results");
        } catch (IllegalArgumentException multipleResults) {
            assertTrue(true);
        }

        filter = equal("id", ft1.getId());
        FeatureTypeInfo featureTypeInfo = rawCatalog.get(FeatureTypeInfo.class, filter);
        assertEquals(ft1.getId(), featureTypeInfo.getId());
        assertSame(rawCatalog, featureTypeInfo.getCatalog());

        filter = equal("name", ft2.getName());
        assertEquals(ft2.getName(), rawCatalog.get(ResourceInfo.class, filter).getName());

        filter = equal("keywords[1].value", ft1.getKeywords().get(0).getValue());
        assertEquals(ft1.getName(), rawCatalog.get(ResourceInfo.class, filter).getName());

        filter = equal("keywords[2]", ft2.getKeywords().get(1));
        assertEquals(ft2.getName(), rawCatalog.get(FeatureTypeInfo.class, filter).getName());

        filter = equal("keywords[3].value", "repeatedKw");
        try {
            rawCatalog.get(FeatureTypeInfo.class, filter).getName();
            fail("Expected IAE on multiple results");
        } catch (IllegalArgumentException multipleResults) {
            assertTrue(true);
        }

        filter = equal("defaultStyle.filename", "s1Filename");
        assertEquals(l1.getId(), rawCatalog.get(LayerInfo.class, filter).getId());

        filter = equal("defaultStyle.name", s2.getName());
        assertEquals(l2.getId(), rawCatalog.get(LayerInfo.class, filter).getId());
        // Waiting for fix of MultiCompareFilterImpl.evaluate for Sets
        // filter = equal("styles", l2.getStyles(), MatchAction.ALL);
        // assertEquals(l2.getId(), catalog.get(LayerInfo.class, filter).getId());

        filter = equal("styles.id", s2.getId(), MatchAction.ONE);
        assertEquals(l1.getId(), rawCatalog.get(LayerInfo.class, filter).getId());

        filter = equal("styles.id", s3.getId(), MatchAction.ANY); // s3 is shared by l1 and l2
        try {
            rawCatalog.get(LayerInfo.class, filter);
            fail("Expected IAE on multiple results");
        } catch (IllegalArgumentException multipleResults) {
            assertTrue(true);
        }
    }

    @Test
    public void testListPredicate() {
        addDataStore();
        addNamespace();

        FeatureTypeInfo ft1, ft2, ft3;

        rawCatalog.add(ft1 = newFeatureType("ft1", data.dataStoreA));
        rawCatalog.add(ft2 = newFeatureType("ft2", data.dataStoreA));
        rawCatalog.add(ft3 = newFeatureType("ft3", data.dataStoreA));
        ft1 = rawCatalog.getFeatureType(ft1.getId());
        ft2 = rawCatalog.getFeatureType(ft2.getId());
        ft3 = rawCatalog.getFeatureType(ft3.getId());

        Filter filter = acceptAll();
        Set<? extends CatalogInfo> expected;
        Set<? extends CatalogInfo> actual;

        expected = Sets.newHashSet(ft1, ft2, ft3);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(3, actual.size());
        assertEquals(expected, actual);

        filter = contains("name", "t");
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertTrue(expected.equals(actual));
        assertEquals(expected, actual);

        filter = or(contains("name", "t2"), contains("name", "t1"));
        expected = Sets.newHashSet(ft1, ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        StyleInfo s1, s2, s3, s4, s5, s6;
        rawCatalog.add(s1 = newStyle("s1", "s1Filename"));
        rawCatalog.add(s2 = newStyle("s2", "s2Filename"));
        rawCatalog.add(s3 = newStyle("s3", "s3Filename"));
        rawCatalog.add(s4 = newStyle("s4", "s4Filename"));
        rawCatalog.add(s5 = newStyle("s5", "s5Filename"));
        rawCatalog.add(s6 = newStyle("s6", "s6Filename"));

        @SuppressWarnings("unused")
        LayerInfo l1, l2, l3;
        rawCatalog.add(l1 = newLayer(ft1, s1));
        rawCatalog.add(l2 = newLayer(ft2, s2, s3, s4));
        rawCatalog.add(l3 = newLayer(ft3, s3, s5, s6));

        filter = contains("styles.name", "s6");
        expected = Sets.newHashSet(l3);
        actual = Sets.newHashSet(rawCatalog.list(LayerInfo.class, filter));
        assertEquals(expected, actual);

        filter = equal("defaultStyle.name", "s1");
        expected = Sets.newHashSet(l1);
        actual = Sets.newHashSet(rawCatalog.list(LayerInfo.class, filter));
        assertEquals(expected, actual);

        filter = or(contains("styles.name", "s6"), equal("defaultStyle.name", "s1"));
        expected = Sets.newHashSet(l1, l3);
        actual = Sets.newHashSet(rawCatalog.list(LayerInfo.class, filter));
        assertEquals(expected, actual);

        filter = acceptAll();
        ArrayList<LayerInfo> naturalOrder =
                Lists.newArrayList(rawCatalog.list(LayerInfo.class, filter));
        assertEquals(3, naturalOrder.size());

        int offset = 0, limit = 2;
        assertEquals(
                naturalOrder.subList(0, 2),
                Lists.newArrayList(rawCatalog.list(LayerInfo.class, filter, offset, limit, null)));

        offset = 1;
        assertEquals(
                naturalOrder.subList(1, 3),
                Lists.newArrayList(rawCatalog.list(LayerInfo.class, filter, offset, limit, null)));

        limit = 1;
        assertEquals(
                naturalOrder.subList(1, 2),
                Lists.newArrayList(rawCatalog.list(LayerInfo.class, filter, offset, limit, null)));
    }

    /**
     * This tests more advanced filters: multi-valued filters, opposite equations, field equations
     */
    @Test
    public void testListPredicateExtended() {
        addDataStore();
        addNamespace();

        final FilterFactory factory = CommonFactoryFinder.getFilterFactory();

        FeatureTypeInfo ft1, ft2, ft3;

        rawCatalog.add(ft1 = newFeatureType("ft1", data.dataStoreA));
        rawCatalog.add(ft2 = newFeatureType("ft2", data.dataStoreA));
        rawCatalog.add(ft3 = newFeatureType("ft3", data.dataStoreA));
        ft1 = rawCatalog.getFeatureType(ft1.getId());
        ft2 = rawCatalog.getFeatureType(ft2.getId());
        ft3 = rawCatalog.getFeatureType(ft3.getId());
        ft1.getKeywords().add(new Keyword("keyword1"));
        ft1.getKeywords().add(new Keyword("keyword2"));
        ft1.getKeywords().add(new Keyword("ft1"));
        ft1.setDescription("ft1 description");
        rawCatalog.save(ft1);
        ft2.getKeywords().add(new Keyword("keyword1"));
        ft2.getKeywords().add(new Keyword("keyword1"));
        ft2.setDescription("ft2");
        rawCatalog.save(ft2);
        ft3.getKeywords().add(new Keyword("ft3"));
        ft3.getKeywords().add(new Keyword("ft3"));
        ft3.setDescription("FT3");
        rawCatalog.save(ft3);

        Filter filter = acceptAll();
        Set<? extends CatalogInfo> expected;
        Set<? extends CatalogInfo> actual;

        // opposite equality
        filter = factory.equal(factory.literal(ft1.getId()), factory.property("id"), true);
        expected = Sets.newHashSet(ft1);
        actual = Sets.newHashSet(rawCatalog.list(ResourceInfo.class, filter));
        assertEquals(expected, actual);

        // match case
        filter = factory.equal(factory.literal("FT1"), factory.property("name"), false);
        expected = Sets.newHashSet(ft1);
        actual = Sets.newHashSet(rawCatalog.list(ResourceInfo.class, filter));
        assertEquals(expected, actual);

        // equality of fields
        filter = factory.equal(factory.property("name"), factory.property("description"), true);
        expected = Sets.newHashSet(ft2);
        actual = Sets.newHashSet(rawCatalog.list(ResourceInfo.class, filter));
        assertEquals(expected, actual);

        // match case
        filter = factory.equal(factory.property("name"), factory.property("description"), false);
        expected = Sets.newHashSet(ft2, ft3);
        actual = Sets.newHashSet(rawCatalog.list(ResourceInfo.class, filter));
        assertEquals(expected, actual);

        // match action
        filter =
                factory.equal(
                        factory.literal(new Keyword("keyword1")),
                        factory.property("keywords"),
                        true,
                        MatchAction.ANY);
        expected = Sets.newHashSet(ft1, ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        filter =
                factory.equal(
                        factory.literal(new Keyword("keyword1")),
                        factory.property("keywords"),
                        true,
                        MatchAction.ALL);
        expected = Sets.newHashSet(ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        filter =
                factory.equal(
                        factory.literal(new Keyword("keyword1")),
                        factory.property("keywords"),
                        true,
                        MatchAction.ONE);
        expected = Sets.newHashSet(ft1);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        // match action - like
        filter =
                factory.like(
                        factory.property("keywords"),
                        "key*d1",
                        "*",
                        "?",
                        "\\",
                        true,
                        MatchAction.ANY);
        expected = Sets.newHashSet(ft1, ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        filter =
                factory.like(
                        factory.property("keywords"),
                        "key*d1",
                        "*",
                        "?",
                        "\\",
                        true,
                        MatchAction.ALL);
        expected = Sets.newHashSet(ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        filter =
                factory.like(
                        factory.property("keywords"),
                        "key*d1",
                        "*",
                        "?",
                        "\\",
                        true,
                        MatchAction.ONE);
        expected = Sets.newHashSet(ft1);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        // multivalued literals
        List<Object> values = new ArrayList<>();
        values.add("ft1");
        values.add("ft2");
        filter =
                factory.equal(
                        factory.literal(values), factory.property("name"), true, MatchAction.ANY);
        expected = Sets.newHashSet(ft1, ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        values = new ArrayList<>();
        values.add("ft1");
        values.add("ft1");
        filter =
                factory.equal(
                        factory.literal(values), factory.property("name"), true, MatchAction.ALL);
        expected = Sets.newHashSet(ft1);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        values = new ArrayList<>();
        values.add("ft1");
        values.add("ft2");
        filter =
                factory.equal(
                        factory.literal(values), factory.property("name"), true, MatchAction.ALL);
        expected = Sets.newHashSet();
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        values = new ArrayList<>();
        values.add("ft1");
        values.add("ft1");
        values.add("ft2");
        filter =
                factory.equal(
                        factory.literal(values), factory.property("name"), true, MatchAction.ONE);
        expected = Sets.newHashSet(ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        // multivalued literals with multivalued fields

        values = new ArrayList<>();
        values.add(new Keyword("keyword1"));
        values.add(new Keyword("keyword2"));
        filter =
                factory.equal(
                        factory.literal(values),
                        factory.property("keywords"),
                        true,
                        MatchAction.ANY);
        expected = Sets.newHashSet(ft1, ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        values = new ArrayList<>();
        values.add(new Keyword("keyword1"));
        values.add(new Keyword("keyword1"));
        filter =
                factory.equal(
                        factory.literal(values),
                        factory.property("keywords"),
                        true,
                        MatchAction.ALL);
        expected = Sets.newHashSet(ft2);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);

        values = new ArrayList<>();
        values.add(new Keyword("keyword1"));
        values.add(new Keyword("blah"));
        filter =
                factory.equal(
                        factory.literal(values),
                        factory.property("keywords"),
                        true,
                        MatchAction.ONE);
        expected = Sets.newHashSet(ft1);
        actual = Sets.newHashSet(rawCatalog.list(FeatureTypeInfo.class, filter));
        assertEquals(expected, actual);
    }

    @Test
    public void testOrderBy() {
        addDataStore();
        addNamespace();

        FeatureTypeInfo ft1 = newFeatureType("ft1", data.dataStoreA);
        FeatureTypeInfo ft2 = newFeatureType("ft2", data.dataStoreA);
        FeatureTypeInfo ft3 = newFeatureType("ft3", data.dataStoreA);

        ft2.getKeywords().add(new Keyword("keyword1"));
        ft2.getKeywords().add(new Keyword("keyword2"));

        rawCatalog.add(ft1);
        rawCatalog.add(ft2);
        rawCatalog.add(ft3);

        StyleInfo s1, s2, s3, s4, s5, s6;
        rawCatalog.add(s1 = newStyle("s1", "s1Filename"));
        rawCatalog.add(s2 = newStyle("s2", "s2Filename"));
        rawCatalog.add(s3 = newStyle("s3", "s3Filename"));
        rawCatalog.add(s4 = newStyle("s4", "s4Filename"));
        rawCatalog.add(s5 = newStyle("s5", "s5Filename"));
        rawCatalog.add(s6 = newStyle("s6", "s6Filename"));

        LayerInfo l1 = newLayer(ft1, s1);
        LayerInfo l2 = newLayer(ft2, s1, s3, s4);
        LayerInfo l3 = newLayer(ft3, s2, s5, s6);
        rawCatalog.add(l1);
        rawCatalog.add(l2);
        rawCatalog.add(l3);

        assertEquals(3, rawCatalog.getLayers().size());

        Filter filter;
        SortBy sortOrder;
        List<LayerInfo> expected;

        filter = acceptAll();
        sortOrder = asc("resource.name");
        expected = Lists.newArrayList(l1, l2, l3);

        testOrderBy(LayerInfo.class, filter, null, null, sortOrder, expected);

        sortOrder = desc("resource.name");
        expected = Lists.newArrayList(l3, l2, l1);

        testOrderBy(LayerInfo.class, filter, null, null, sortOrder, expected);

        sortOrder = asc("defaultStyle.name");
        expected = Lists.newArrayList(l1, l2, l3);
        testOrderBy(LayerInfo.class, filter, null, null, sortOrder, expected);
        sortOrder = desc("defaultStyle.name");
        expected = Lists.newArrayList(l3, l2, l1);

        testOrderBy(LayerInfo.class, filter, null, null, sortOrder, expected);

        expected = Lists.newArrayList(l2, l1);
        testOrderBy(LayerInfo.class, filter, 1, null, sortOrder, expected);

        expected = Lists.newArrayList(l2);
        testOrderBy(LayerInfo.class, filter, 1, 1, sortOrder, expected);
        sortOrder = asc("defaultStyle.name");
        expected = Lists.newArrayList(l2, l3);
        testOrderBy(LayerInfo.class, filter, 1, 10, sortOrder, expected);

        filter = equal("styles.name", s3.getName());
        expected = Lists.newArrayList(l2);
        testOrderBy(LayerInfo.class, filter, 0, 10, sortOrder, expected);
    }

    private <T extends CatalogInfo> void testOrderBy(
            Class<T> clazz,
            Filter filter,
            Integer offset,
            Integer limit,
            SortBy sortOrder,
            List<T> expected) {

        CatalogPropertyAccessor pe = new CatalogPropertyAccessor();

        List<Object> props = new ArrayList<Object>();
        List<Object> actual = new ArrayList<Object>();
        String sortProperty = sortOrder.getPropertyName().getPropertyName();
        for (T info : expected) {
            Object pval = pe.getProperty(info, sortProperty);
            props.add(pval);
        }

        CloseableIterator<T> it = rawCatalog.list(clazz, filter, offset, limit, sortOrder);
        try {
            while (it.hasNext()) {
                Object property = pe.getProperty(it.next(), sortProperty);
                actual.add(property);
            }
        } finally {
            it.close();
        }

        assertEquals(props, actual);
    }

    @Test
    public void testFullTextSearch() {
        // test layer title search
        data.featureTypeA.setTitle("Global .5 deg Air Temperature [C]");
        data.coverageA.setTitle("Global .5 deg Dewpoint Depression [C]");

        data.featureTypeA.setDescription("FeatureType description");
        data.featureTypeA.setAbstract("GeoServer OpenSource GIS");
        data.coverageA.setDescription("Coverage description");
        data.coverageA.setAbstract("GeoServer uses GeoTools");

        data.layerFeatureTypeA.setResource(data.featureTypeA);

        addLayer();
        rawCatalog.add(data.coverageStoreA);
        rawCatalog.add(data.coverageA);

        LayerInfo l2 = newLayer(data.coverageA, data.style1);
        rawCatalog.add(l2);

        Filter filter = Predicates.fullTextSearch("Description");
        assertEquals(
                newHashSet(data.featureTypeA, data.coverageA),
                asSet(rawCatalog.list(ResourceInfo.class, filter)));
        assertEquals(
                newHashSet(data.featureTypeA),
                asSet(rawCatalog.list(FeatureTypeInfo.class, filter)));
        assertEquals(
                newHashSet(data.coverageA), asSet(rawCatalog.list(CoverageInfo.class, filter)));

        assertEquals(
                newHashSet(data.layerFeatureTypeA, l2),
                asSet(rawCatalog.list(LayerInfo.class, filter)));

        filter = Predicates.fullTextSearch("opensource");
        assertEquals(
                newHashSet(data.layerFeatureTypeA),
                asSet(rawCatalog.list(LayerInfo.class, filter)));

        filter = Predicates.fullTextSearch("geotools");
        assertEquals(newHashSet(l2), asSet(rawCatalog.list(LayerInfo.class, filter)));

        filter = Predicates.fullTextSearch("Global");
        assertEquals(
                newHashSet(data.layerFeatureTypeA, l2),
                asSet(rawCatalog.list(LayerInfo.class, filter)));

        filter = Predicates.fullTextSearch("Temperature");
        assertEquals(
                newHashSet(data.layerFeatureTypeA),
                asSet(rawCatalog.list(LayerInfo.class, filter)));

        filter = Predicates.fullTextSearch("Depression");
        assertEquals(newHashSet(l2), asSet(rawCatalog.list(LayerInfo.class, filter)));
    }

    @Test
    public void testFullTextSearchLayerGroupTitle() {
        addLayer();
        // geos-6882
        data.layerGroup1.setTitle("LayerGroup title");
        rawCatalog.add(data.layerGroup1);

        // test layer group title and abstract search
        Filter filter = Predicates.fullTextSearch("title");
        assertEquals(
                newHashSet(data.layerGroup1), asSet(rawCatalog.list(LayerGroupInfo.class, filter)));
    }

    @Test
    public void testFullTextSearchLayerGroupName() {
        addLayer();
        // geos-6882
        rawCatalog.add(data.layerGroup1);
        Filter filter = Predicates.fullTextSearch("Group");
        assertEquals(
                newHashSet(data.layerGroup1), asSet(rawCatalog.list(LayerGroupInfo.class, filter)));
    }

    @Test
    public void testFullTextSearchLayerGroupAbstract() {
        addLayer();
        data.layerGroup1.setAbstract("GeoServer OpenSource GIS");
        rawCatalog.add(data.layerGroup1);
        Filter filter = Predicates.fullTextSearch("geoserver");
        assertEquals(
                newHashSet(data.layerGroup1), asSet(rawCatalog.list(LayerGroupInfo.class, filter)));
    }

    @Test
    public void testFullTextSearchKeywords() {
        data.featureTypeA.getKeywords().add(new Keyword("air_temp"));
        data.featureTypeA.getKeywords().add(new Keyword("temperatureAir"));
        data.coverageA.getKeywords().add(new Keyword("dwpt_dprs"));
        data.coverageA.getKeywords().add(new Keyword("temperatureDewpointDepression"));

        data.layerFeatureTypeA.setResource(data.featureTypeA);
        addLayer();
        rawCatalog.add(data.coverageStoreA);
        rawCatalog.add(data.coverageA);
        LayerInfo l2 = newLayer(data.coverageA, data.style1);
        rawCatalog.add(l2);

        Filter filter = Predicates.fullTextSearch("temperature");
        assertEquals(
                newHashSet(data.layerFeatureTypeA, l2),
                asSet(rawCatalog.list(LayerInfo.class, filter)));
        assertEquals(
                newHashSet(data.featureTypeA, data.coverageA),
                asSet(rawCatalog.list(ResourceInfo.class, filter)));
        assertEquals(
                newHashSet(data.featureTypeA),
                asSet(rawCatalog.list(FeatureTypeInfo.class, filter)));
        assertEquals(
                newHashSet(data.coverageA), asSet(rawCatalog.list(CoverageInfo.class, filter)));

        filter = Predicates.fullTextSearch("air");
        assertEquals(
                newHashSet(data.layerFeatureTypeA),
                asSet(rawCatalog.list(LayerInfo.class, filter)));
        assertEquals(
                newHashSet(data.featureTypeA), asSet(rawCatalog.list(ResourceInfo.class, filter)));
        assertEquals(
                newHashSet(data.featureTypeA),
                asSet(rawCatalog.list(FeatureTypeInfo.class, filter)));
        assertEquals(newHashSet(), asSet(rawCatalog.list(CoverageInfo.class, filter)));

        filter = Predicates.fullTextSearch("dewpoint");
        assertEquals(newHashSet(l2), asSet(rawCatalog.list(LayerInfo.class, filter)));
        assertEquals(
                newHashSet(data.coverageA), asSet(rawCatalog.list(ResourceInfo.class, filter)));
        assertEquals(newHashSet(), asSet(rawCatalog.list(FeatureTypeInfo.class, filter)));
        assertEquals(
                newHashSet(data.coverageA), asSet(rawCatalog.list(CoverageInfo.class, filter)));

        filter = Predicates.fullTextSearch("pressure");
        assertEquals(newHashSet(), asSet(rawCatalog.list(LayerInfo.class, filter)));
        assertEquals(newHashSet(), asSet(rawCatalog.list(ResourceInfo.class, filter)));
        assertEquals(newHashSet(), asSet(rawCatalog.list(FeatureTypeInfo.class, filter)));
        assertEquals(newHashSet(), asSet(rawCatalog.list(CoverageInfo.class, filter)));
    }

    @Test
    public void testFullTextSearchAddedKeyword() {
        data.featureTypeA.getKeywords().add(new Keyword("air_temp"));
        data.featureTypeA.getKeywords().add(new Keyword("temperatureAir"));

        data.layerFeatureTypeA.setResource(data.featureTypeA);
        addLayer();

        LayerInfo lproxy = rawCatalog.getLayer(data.layerFeatureTypeA.getId());
        FeatureTypeInfo ftproxy = (FeatureTypeInfo) lproxy.getResource();

        ftproxy.getKeywords().add(new Keyword("newKeyword"));
        rawCatalog.save(ftproxy);

        Filter filter = Predicates.fullTextSearch("newKeyword");
        assertEquals(newHashSet(ftproxy), asSet(rawCatalog.list(FeatureTypeInfo.class, filter)));
        assertEquals(newHashSet(lproxy), asSet(rawCatalog.list(LayerInfo.class, filter)));
    }

    private <T> Set<T> asSet(CloseableIterator<T> list) {
        ImmutableSet<T> set;
        try {
            set = ImmutableSet.copyOf(list);
        } finally {
            list.close();
        }
        return set;
    }

    protected LayerInfo newLayer(
            ResourceInfo resource, StyleInfo defStyle, StyleInfo... extraStyles) {
        LayerInfo l2 = rawCatalog.getFactory().createLayer();
        l2.setResource(resource);
        l2.setDefaultStyle(defStyle);
        if (extraStyles != null) {
            for (StyleInfo es : extraStyles) {
                l2.getStyles().add(es);
            }
        }
        return l2;
    }

    protected StyleInfo newStyle(String name, String fileName) {
        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName(name);
        s2.setFilename(fileName);
        return s2;
    }

    protected FeatureTypeInfo newFeatureType(String name, DataStoreInfo ds) {
        FeatureTypeInfo ft2 = rawCatalog.getFactory().createFeatureType();
        ft2.setNamespace(data.namespaceA);
        ft2.setName(name);
        ft2.setStore(ds);
        return ft2;
    }

    @Test
    public void testConcurrentCatalogModification() throws Exception {
        Logger logger = Logging.getLogger(CatalogImpl.class);
        final int tasks = 8;
        ExecutorService executor = Executors.newFixedThreadPool(tasks / 2);
        Level previousLevel = logger.getLevel();
        // clear previous listeners
        new ArrayList<>(rawCatalog.getListeners()).forEach(l -> rawCatalog.removeListener(l));
        try {
            // disable logging for this test, it will stay a while in case of failure otherwise
            logger.setLevel(Level.OFF);
            ExecutorCompletionService<Void> completionService =
                    new ExecutorCompletionService<>(executor);
            for (int i = 0; i < tasks; i++) {
                completionService.submit(
                        () -> {
                            // attach listeners
                            List<TestListener> listeners = new ArrayList<>();
                            for (int j = 0; j < 3; j++) {
                                TestListener tl = new TestListener();
                                listeners.add(tl);
                                rawCatalog.addListener(tl);
                            }

                            // simulate catalog removals, check the events get to destination
                            CatalogInfo catalogInfo = new CoverageInfoImpl(rawCatalog);
                            rawCatalog.fireRemoved(catalogInfo);
                            // make sure each listener actually got the message
                            for (TestListener testListener : listeners) {
                                assertTrue(
                                        "Did not find the expected even in the listener",
                                        testListener
                                                .removed
                                                .stream()
                                                .anyMatch(
                                                        event -> event.getSource() == catalogInfo));
                            }

                            // clear the listeners
                            listeners.forEach(l -> rawCatalog.removeListener(l));
                        },
                        null);
            }
            for (int i = 0; i < tasks; ++i) {
                completionService.take().get();
            }
        } finally {
            executor.shutdown();
            logger.setLevel(previousLevel);
        }
    }

    @Test
    public void testChangeLayerGroupOrder() {
        addLayerGroup();

        // create second layer
        FeatureTypeInfo ft2 = rawCatalog.getFactory().createFeatureType();
        ft2.setName("ft2Name");
        ft2.setStore(data.dataStoreA);
        ft2.setNamespace(data.namespaceA);
        rawCatalog.add(ft2);
        LayerInfo l2 = rawCatalog.getFactory().createLayer();
        l2.setResource(ft2);
        l2.setDefaultStyle(data.style1);
        rawCatalog.add(l2);

        // add to the group
        LayerGroupInfo group = rawCatalog.getLayerGroupByName(data.layerGroup1.getName());
        group.getLayers().add(l2);
        group.getStyles().add(null);
        rawCatalog.save(group);

        // change the layer group order
        group = rawCatalog.getLayerGroupByName(data.layerGroup1.getName());
        PublishedInfo pi = group.getLayers().remove(1);
        group.getLayers().add(0, pi);
        rawCatalog.save(group);

        // create a new style
        StyleInfo s2 = rawCatalog.getFactory().createStyle();
        s2.setName("s2Name");
        s2.setFilename("s2Filename");
        rawCatalog.add(s2);

        // change the default style of l
        LayerInfo ll = rawCatalog.getLayerByName(data.layerFeatureTypeA.prefixedName());
        ll.setDefaultStyle(rawCatalog.getStyleByName(s2.getName()));
        rawCatalog.save(ll);

        // now check that the facade can be compared to itself
        LayerGroupInfo g1 = rawCatalog.getFacade().getLayerGroupByName(data.layerGroup1.getName());
        LayerGroupInfo g2 = rawCatalog.getFacade().getLayerGroupByName(data.layerGroup1.getName());
        assertTrue(LayerGroupInfo.equals(g1, g2));
    }

    @Test
    public void testIterablesHaveCatalogSet() {
        data.addObjects();
        {
            CloseableIterator<StoreInfo> stores = rawCatalog.list(StoreInfo.class, acceptAll());
            assertTrue(stores.hasNext());
            stores.forEachRemaining(s -> assertSame(rawCatalog, s.getCatalog()));
        }
        {
            CloseableIterator<ResourceInfo> resources =
                    rawCatalog.list(ResourceInfo.class, acceptAll());
            assertTrue(resources.hasNext());
            resources.forEachRemaining(r -> assertSame(rawCatalog, r.getCatalog()));
        }
        {
            CloseableIterator<LayerInfo> layers = rawCatalog.list(LayerInfo.class, acceptAll());
            assertTrue(layers.hasNext());
            layers.forEachRemaining(
                    r -> {
                        assertSame(rawCatalog, r.getResource().getCatalog());
                        assertSame(rawCatalog, r.getResource().getStore().getCatalog());
                    });
        }
        {
            CloseableIterator<LayerGroupInfo> groups =
                    rawCatalog.list(LayerGroupInfo.class, acceptAll());
            assertTrue(groups.hasNext());
            groups.forEachRemaining(
                    g -> {
                        List<PublishedInfo> layers = g.getLayers();
                        layers.forEach(
                                p -> {
                                    if (p instanceof LayerInfo) {
                                        LayerInfo l = (LayerInfo) p;
                                        assertSame(rawCatalog, l.getResource().getCatalog());
                                        assertSame(
                                                rawCatalog,
                                                l.getResource().getStore().getCatalog());
                                    }
                                });
                    });
        }
    }
}
