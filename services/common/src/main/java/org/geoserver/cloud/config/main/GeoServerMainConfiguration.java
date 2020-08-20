/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.main;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.ManifestLoader;
import org.geoserver.catalog.CascadedStoredQueryCallback;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogRepository;
import org.geoserver.catalog.CoverageReaderFileConverter;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.NamespaceWorkspaceConsistencyListener;
import org.geoserver.catalog.ResourcePoolInitializer;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.SLDPackageHandler;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.catalog.VirtualTableCallback;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.config.CapabilitiesCacheHeadersCallback;
import org.geoserver.config.CatalogTimeStampUpdater;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.LockProviderInitializer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.coverage.CoverageAccessInitializer;
import org.geoserver.jai.JAIInitializer;
import org.geoserver.logging.LoggingInitializer;
import org.geoserver.ows.ClasspathPublisher;
import org.geoserver.ows.DisabledServiceCheck;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.EnviromentInjectionCallback;
import org.geoserver.ows.FilePublisher;
import org.geoserver.ows.LocalWorkspaceCallback;
import org.geoserver.ows.LocalWorkspaceCatalogFilter;
import org.geoserver.ows.LocalWorkspaceURLMangler;
import org.geoserver.ows.OWSHandlerMapping;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.StylePublisher;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.platform.RenderingEngineStatus;
import org.geoserver.platform.SystemEnvironmentStatus;
import org.geoserver.platform.SystemPropertyStatus;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.geoserver.platform.resource.FileLockProvider;
import org.geoserver.platform.resource.GlobalLockProvider;
import org.geoserver.platform.resource.MemoryLockProvider;
import org.geoserver.platform.resource.NullLockProvider;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.geoserver.platform.util.VersionPropertyEditor;
import org.geoserver.security.DisabledResourceFilter;
import org.geoserver.security.DisabledServiceResourceFilter;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.decorators.DefaultSecureCatalogFactory;
import org.geoserver.security.decorators.DefaultSecureDataFactory;
import org.geoserver.threadlocals.AdminRequestThreadLocalTransfer;
import org.geoserver.threadlocals.AuthenticationThreadLocalTransfer;
import org.geoserver.threadlocals.EnvVarThreadLocalTransfer;
import org.geoserver.threadlocals.LocalPublishedThreadLocalTransfer;
import org.geoserver.threadlocals.LocalWorkspaceThreadLocalTransfer;
import org.geoserver.threadlocals.PublicThreadLocalTransfer;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.util.NearestMatchWarningAppender;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.vfny.geoserver.servlets.BufferStrategy;
import org.vfny.geoserver.servlets.FileStrategy;
import org.vfny.geoserver.servlets.PartialBufferStrategy2;
import org.vfny.geoserver.servlets.ServiceStrategyFactory;
import org.vfny.geoserver.servlets.SpeedStrategy;

@Configuration
@ComponentScan(basePackages = "org.geoserver.system.status")
public class GeoServerMainConfiguration {

    //    <bean class="org.geoserver.platform.ModuleStatusImpl">
    //    <constructor-arg index="0" value="gs-main"/>
    //    <constructor-arg index="1" value="GeoServer Main"/>
    //    </bean>
    public @Bean ModuleStatusImpl mainModuleStatus() {
        return new ModuleStatusImpl("gs-main", "GeoServer Main");
    }

    //	<bean class="org.geoserver.platform.RenderingEngineStatus"/>
    public @Bean RenderingEngineStatus renderingEngineStatus() {
        return new RenderingEngineStatus();
    }

    // <bean class="org.geoserver.platform.SystemPropertyStatus"/>
    public @Bean SystemPropertyStatus systemPropertyStatus() {
        return new SystemPropertyStatus();
    }

    // <bean class="org.geoserver.platform.SystemEnvironmentStatus"/>
    public @Bean SystemEnvironmentStatus systemEnvironmentStatus() {
        return new SystemEnvironmentStatus();
    }

    // <!--  lock providers -->
    // <bean id="nullLockProvider" class="org.geoserver.platform.resource.NullLockProvider"/>
    public @Bean NullLockProvider nullLockProvider() {
        return new NullLockProvider();
    }

    // <bean id="memoryLockProvider" class="org.geoserver.platform.resource.MemoryLockProvider"/>
    public @Bean MemoryLockProvider memoryLockProvider() {
        return new MemoryLockProvider();
    }

    // <bean id="fileLockProvider" class="org.geoserver.platform.resource.FileLockProvider"/>
    public @Bean FileLockProvider fileLockProvider() {
        return new FileLockProvider();
    }

    // <bean id="lockProvider" class="org.geoserver.platform.resource.GlobalLockProvider">
    //    <property name="delegate" ref="nullLockProvider"/>
    // </bean>
    public @Bean GlobalLockProvider lockProvider() {
        GlobalLockProvider p = new GlobalLockProvider();
        p.setDelegate(nullLockProvider());
        return p;
    }

    // <bean id="lockProviderInitializer" class="org.geoserver.config.LockProviderInitializer"/>
    public @Bean LockProviderInitializer lockProviderInitializer() {
        return new LockProviderInitializer();
    }

    // <!-- used by alternative resource stores -->
    // <bean id="resourceNotificationDispatcher"
    // class="org.geoserver.platform.resource.SimpleResourceNotificationDispatcher"/>
    public @Bean SimpleResourceNotificationDispatcher resourceNotificationDispatcher() {
        return new SimpleResourceNotificationDispatcher();
    }

    // <!-- resources -->
    // <bean id="dataDirectoryResourceStore"
    // class="org.geoserver.platform.resource.DataDirectoryResourceStore">
    //    <property name="lockProvider" ref="lockProvider"/>
    // </bean>
    public @Bean DataDirectoryResourceStore dataDirectoryResourceStore() {
        DataDirectoryResourceStore s = new DataDirectoryResourceStore();
        s.setLockProvider(lockProvider());
        return s;
    }

    // <bean id="resourceStore" class="org.geoserver.platform.resource.ResourceStoreFactory" />
    public @Bean ResourceStoreFactory resourceStore() {
        return new ResourceStoreFactory();
    }

    // <bean id="resourceLoader" class="org.geoserver.platform.GeoServerResourceLoader">
    //    <constructor-arg ref="resourceStore"/>
    // </bean>
    public @Bean GeoServerResourceLoader resourceLoader() throws Exception {
        return new GeoServerResourceLoader(resourceStore().getObject());
    }

    // <bean id="dataDirectory" class="org.geoserver.config.GeoServerDataDirectory">
    //   <constructor-arg ref="resourceLoader"/>
    // </bean>
    public @Bean GeoServerDataDirectory dataDirectory() throws Exception {
        return new GeoServerDataDirectory(resourceLoader());
    }

    // <bean id="manifestLoader" class="org.geoserver.ManifestLoader" lazy-init="false">
    //   <constructor-arg ref="resourceLoader"/>
    // </bean>
    public @Bean ManifestLoader manifestLoader() throws Exception {
        return new ManifestLoader(resourceLoader());
    }

    // <!-- extensions -->
    // <bean id="extensions" class="org.geoserver.platform.GeoServerExtensions"/>
    public @Bean GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    // <!-- environments -->
    // <bean id="environments" class="org.geoserver.platform.GeoServerEnvironment"
    // depends-on="extensions"/>
    public @Bean @DependsOn("extensions") GeoServerEnvironment environments() {
        return new GeoServerEnvironment();
    }

    // <!-- the shared filter factory -->
    // <bean id="filterFactory" class="org.geotools.filter.FilterFactoryImpl"/>
    public @Bean FilterFactory2 filterFactory() {
        return new FilterFactoryImpl();
    }

    // <!-- geotools factory iterator provider, commented
    // <bean id="factoryIteratorProvider" depends-on="extensions"
    //   class="org.geoserver.platform.GeoServerFactoryIteratorProvider"/>
    // -->

    // <!--
    //    core modules
    // -->

    // <!-- configuration module -->
    //    <!-- note: we use depends to ensure that all datastore plugins are
    //         loaded from the spring container before processing hte catalog -->

    // <bean id="rawCatalog" class="org.geoserver.catalog.impl.CatalogImpl"
    // depends-on="configurationLock">
    //     <property name="resourceLoader" ref="resourceLoader"/>
    // </bean>
    public @Bean CatalogImpl rawCatalog() throws Exception {
        CatalogImpl c = new CatalogImpl();
        c.setResourceLoader(resourceLoader());
        return c;
    }

    // <bean id="secureCatalog" class="org.geoserver.security.SecureCatalogImpl"
    // depends-on="accessRulesDao,extensions">
    //    <constructor-arg ref="rawCatalog" />
    // </bean>
    //	public @Bean @DependsOn({ "extensions" }) SecureCatalogImpl secureCatalog() throws Exception
    // {
    //		return new SecureCatalogImpl(rawCatalog());
    //	}

    @Bean(name = "secureCatalog")
    @ConditionalOnMissingBean(ResourceAccessManager.class)
    @DependsOn({"extensions"})
    public Catalog nonSecureSecureCatalog(@Qualifier("rawCatalog") Catalog rawCatalog)
            throws Exception {
        return rawCatalog;
    }

    @Bean(name = "secureCatalog")
    @ConditionalOnBean(ResourceAccessManager.class)
    @DependsOn({"extensions", "accessRulesDao"})
    public Catalog secureSecureCatalog(ResourceAccessManager ram) throws Exception {
        return new SecureCatalogImpl(rawCatalog(), ram);
    }

    // <bean id="advertisedCatalog" class="org.geoserver.catalog.impl.AdvertisedCatalog">
    //    <constructor-arg ref="secureCatalog" />
    //    <property name="layerGroupVisibilityPolicy">
    //    	<bean id="org.geoserver.catalog.LayerGroupVisibilityPolicy.HIDE_NEVER"
    //    		class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"/>
    //    </property>
    // </bean>
    public @Bean AdvertisedCatalog advertisedCatalog(
            @Qualifier("secureCatalog") Catalog secureCatalog) throws Exception {
        AdvertisedCatalog c = new AdvertisedCatalog(secureCatalog);
        c.setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy.HIDE_NEVER);
        return c;
    }

    // <bean id="localWorkspaceCatalog" class="org.geoserver.catalog.impl.LocalWorkspaceCatalog">
    //    <constructor-arg ref="advertisedCatalog" />
    // </bean>
    // <!-- Switch this when you want to enable the secure catalog by default -->
    // <!--alias name="secureCatalog" alias="catalog"/-->
    // <alias name="localWorkspaceCatalog" alias="catalog"/>
    @Bean(name = {"localWorkspaceCatalog", "catalog"})
    public LocalWorkspaceCatalog localWorkspaceCatalog(
            @Qualifier("secureCatalog") Catalog secureCatalog) throws Exception {
        return new LocalWorkspaceCatalog(advertisedCatalog(secureCatalog));
    }

    // <bean id="disabledResourceFilter" class="org.geoserver.security.DisabledResourceFilter"/>
    public @Bean DisabledResourceFilter disabledResourceFilter() {
        return new DisabledResourceFilter();
    }

    // <bean id="disabledServiceResourceFilter"
    // class="org.geoserver.security.DisabledServiceResourceFilter"/>
    public @Bean DisabledServiceResourceFilter disabledServiceResourceFilter() {
        return new DisabledServiceResourceFilter();
    }

    // <bean id="geoServer" class="org.geoserver.config.impl.GeoServerImpl">
    //  <property name="catalog" ref="catalog"/>
    // </bean>
    public @Bean GeoServerImpl geoServer(LocalWorkspaceCatalog catalog) {
        GeoServerImpl geoServerImpl = new GeoServerImpl();
        geoServerImpl.setCatalog(catalog);
        return geoServerImpl;
    }

    // <bean id="geoServerLoader" class="org.geoserver.config.GeoServerLoaderProxy">
    //  <constructor-arg ref="resourceLoader"/>
    // </bean>
    public @Bean GeoServerLoaderProxy geoServerLoader() throws Exception {
        return new GeoServerLoaderProxy(resourceLoader());
    }

    // <!--
    //    service strategies
    // -->
    // <bean id="serviceStrategyFactory"
    //    class="org.vfny.geoserver.servlets.ServiceStrategyFactory">
    //    <constructor-arg ref="geoServer"/>
    // </bean>
    public @Bean ServiceStrategyFactory serviceStrategyFactory(GeoServer geoserver) {
        return new ServiceStrategyFactory(geoserver);
    }

    //// <bean id="speedServiceStrategy" name="SPEED"
    ////    class="org.vfny.geoserver.servlets.SpeedStrategy"/>
    public @Bean(name = {"speedServiceStrategy", "SPEED"}) SpeedStrategy speedServiceStrategy() {
        return new SpeedStrategy();
    }

    //
    // <bean id="fileServiceStrategy" name="FILE"
    //    class="org.vfny.geoserver.servlets.FileStrategy"/>
    public @Bean(name = {"fileServiceStrategy", "FILE"}) FileStrategy fileServiceStrategy() {
        return new FileStrategy();
    }

    //
    // <bean id="bufferServiceStrategy" name="BUFFER"
    //    class="org.vfny.geoserver.servlets.BufferStrategy"/>
    public @Bean(name = {"bufferServiceStrategy", "BUFFER"}) BufferStrategy
            bufferServiceStrategy() {
        return new BufferStrategy();
    }

    //
    // <bean id="partialBufferServiceStrategy2" name="PARTIAL-BUFFER2"
    //    class="org.vfny.geoserver.servlets.PartialBufferStrategy2"/>
    public @Bean(name = {"partialBufferServiceStrategy2", "PARTIAL-BUFFER2"}) PartialBufferStrategy2
            partialBufferStrategy2() {
        return new PartialBufferStrategy2();
    }

    //
    // <!--
    //    custom property editors
    // -->
    // <bean id="customEditorConfigurer"
    // class="org.springframework.beans.factory.config.CustomEditorConfigurer">
    //    <property name="customEditors">
    //      <map>
    //          <entry key="org.geotools.util.Version"
    // value="org.geoserver.platform.util.VersionPropertyEditor"/>
    //      </map>
    //    </property>
    // </bean>
    public @Bean CustomEditorConfigurer customEditorConfigurer() {
        CustomEditorConfigurer c = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>();
        customEditors.put(org.geotools.util.Version.class, VersionPropertyEditor.class);
        c.setCustomEditors(customEditors);
        return c;
    }

    //
    //
    // <!--  dispatcher -->
    // <bean id="dispatcher" class="org.geoserver.ows.Dispatcher"/>
    public @Bean Dispatcher dispatcher() {
        return new Dispatcher();
    }

    //
    // <!-- file publisher, allows parts of the data dir to be published as static files (used
    //     to publish www and by wcs 1.1.1 ) -->
    // <bean id="filePublisher" class="org.geoserver.ows.FilePublisher">
    //  <constructor-arg ref="resourceLoader" />
    // </bean>
    public @Bean FilePublisher filePublisher() throws Exception {
        return new FilePublisher(resourceLoader());
    }

    //
    // <!-- style publisher, used to publish the global styles folder currently in workspaced, www
    // and by wcs 1.1.1 -->
    // <bean id="stylePublisher" class="org.geoserver.ows.StylePublisher">
    //  <constructor-arg ref="catalog" />
    // </bean>
    public @Bean StylePublisher stylePublisher(@Qualifier("catalog") Catalog catalog) {
        return new StylePublisher(catalog);
    }

    //
    // <!-- classpath publisher, allows resources in the classpath to be published as static files
    // -->
    // <bean id="classpathPublisher" class="org.geoserver.ows.ClasspathPublisher"/>
    public @Bean ClasspathPublisher classpathPublisher() {
        return new ClasspathPublisher();
    }

    //
    // <!-- jai initializer -->
    // <bean id="jaiInitializer" class="org.geoserver.jai.JAIInitializer"/>
    public @Bean JAIInitializer jaiInitializer() {
        return new JAIInitializer();
    }

    //
    // <!-- coverage access initializer -->
    // <bean id="coverageAccessInitializer"
    // class="org.geoserver.coverage.CoverageAccessInitializer">
    // </bean>
    public @Bean CoverageAccessInitializer coverageAccessInitializer() {
        return new CoverageAccessInitializer();
    }

    //
    // <!-- logging initializer -->
    // <bean id="loggingInitializer" class="org.geoserver.logging.LoggingInitializer">
    // <property name="resourceLoader" ref="resourceLoader"/>
    // </bean>
    public @Bean LoggingInitializer loggingInitializer() throws Exception {
        LoggingInitializer loggingInitializer = new LoggingInitializer();
        loggingInitializer.setResourceLoader(resourceLoader());
        return loggingInitializer;
    }

    // <!-- Entity resolver provider, to stop xml attacks via system entity
    // resolution -->
    // <!-- The depends-on is important to avoid circularities, when a depends-on is
    // setup a bean does not
    // start creation until the bean it depends onto is created. This ensures
    // GeoServer post-processing
    // inside GeoServerLoader ends up causing the initialization of
    // entityResolverProvider instead of the
    // opposite, via the ResourcePoolInitializer, and breaks the circularity that
    // would otherwise happen -->
    // <bean id="entityResolverProvider"
    // class="org.geoserver.util.EntityResolverProvider" depends-on="geoServer">
    // <constructor-arg ref="geoServer"/>
    // </bean>
    public @Bean EntityResolverProvider entityResolverProvider(GeoServer geoserver) {
        return new EntityResolverProvider(geoserver);
    }

    // <!-- resource pool initializer -->
    // <bean id="resourcePoolInitializer" class="org.geoserver.catalog.ResourcePoolInitializer">
    //  <constructor-arg ref="entityResolverProvider"/>
    // </bean>
    public @Bean ResourcePoolInitializer resourcePoolInitializer(
            EntityResolverProvider entityResolverProvider) {
        return new ResourcePoolInitializer(entityResolverProvider);
    }

    //
    // <!-- security wrapper factories  -->
    // <bean id="defaultDataSecurityFactory"
    // class="org.geoserver.security.decorators.DefaultSecureDataFactory"/>
    public @Bean DefaultSecureDataFactory defaultDataSecurityFactory() {
        return new DefaultSecureDataFactory();
    }

    // <bean id="defaultCatalogSecurityFactory"
    // class="org.geoserver.security.decorators.DefaultSecureCatalogFactory"/>
    public @Bean DefaultSecureCatalogFactory defaultCatalogSecurityFactory() {
        return new DefaultSecureCatalogFactory();
    }

    //
    // <bean id="disabledServiceChecker" class="org.geoserver.ows.DisabledServiceCheck"/>
    public @Bean DisabledServiceCheck disabledServiceChecker() {
        return new DisabledServiceCheck();
    }

    //
    // <bean id="dispatcherMapping"
    // class="org.geoserver.ows.OWSHandlerMapping">
    // <constructor-arg ref="catalog"/>
    // <property name="alwaysUseFullPath" value="true"/>
    // <property name="mappings">
    //  <props>
    //    <prop key="/ows">dispatcher</prop>
    //    <prop key="/ows/**">dispatcher</prop>
    //  </props>
    // </property>
    // </bean>
    public @Bean OWSHandlerMapping dispatcherMapping(@Qualifier("catalog") Catalog catalog) {
        OWSHandlerMapping m = new OWSHandlerMapping(catalog);
        m.setAlwaysUseFullPath(true);
        Properties mappings = new Properties();
        mappings.setProperty("/ows", "dispatcher");
        mappings.setProperty("/ows/**", "dispatcher");
        m.setMappings(mappings);
        return m;
    }

    //
    // <bean id="filePublisherMapping"
    // class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
    // <property name="alwaysUseFullPath" value="true"/>
    // <property name="mappings">
    //   <props>
    //    <prop key="/www/**">filePublisher</prop>
    //   </props>
    // </property>
    // </bean>
    public @Bean(name = "filePublisherMapping") SimpleUrlHandlerMapping filePublisherMapping() {
        Map<String, String> map = new HashMap<>();
        map.put("/www/**", "filePublisher");
        SimpleUrlHandlerMapping m = new SimpleUrlHandlerMapping(map);
        m.setAlwaysUseFullPath(true);
        return m;
    }

    //
    // <bean id="stylePublisherMapping"
    // class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
    // <property name="alwaysUseFullPath" value="true"/>
    // <property name="mappings">
    //   <props>
    //    <prop key="/styles/**">stylePublisher</prop>
    //   </props>
    // </property>
    // </bean>
    public @Bean(name = "stylePublisherMapping") SimpleUrlHandlerMapping stylePublisherMapping() {
        Map<String, String> map = new HashMap<>();
        map.put("/styles/**", "stylePublisher");
        SimpleUrlHandlerMapping m = new SimpleUrlHandlerMapping(map);
        m.setAlwaysUseFullPath(true);
        return m;
    }

    // <bean id="classpathPublisherMapping"
    // class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
    // <property name="alwaysUseFullPath" value="true"/>
    // <property name="mappings">
    //   <props>
    //    <prop key="/schemas/**">classpathPublisher</prop>
    //    <prop key="/j_acegi_security_check">classpathPublisher</prop>
    //    <prop key="/j_spring_security_check">classpathPublisher</prop>
    //    <prop key="/login">classpathPublisher</prop>
    //   </props>
    // </property>
    // </bean>
    public @Bean(name = "classpathPublisherMapping") SimpleUrlHandlerMapping
            classpathPublisherMapping() {
        Map<String, String> map = new HashMap<>();
        map.put("/schemas/**", "classpathPublisher");
        map.put("/j_acegi_security_check", "classpathPublisher");
        map.put("/j_spring_security_check", "classpathPublisher");
        map.put("/login", "classpathPublisher");
        SimpleUrlHandlerMapping m = new SimpleUrlHandlerMapping(map);
        m.setAlwaysUseFullPath(true);
        return m;
    }

    //
    // <!-- geotools repository adapter for catalog -->
    // <bean id="catalogRepository" class="org.geoserver.catalog.CatalogRepository">
    // <constructor-arg ref="catalog"/>
    // </bean>
    public @Bean CatalogRepository catalogRepository(@Qualifier("catalog") Catalog catalog) {
        return new CatalogRepository(catalog);
    }

    // <!-- the proxyfing URL mangler -->
    // <bean id="proxyfier" class="org.geoserver.ows.ProxifyingURLMangler">
    // <constructor-arg index="0" ref="geoServer"/>
    // </bean>
    public @Bean ProxifyingURLMangler proxyfier(GeoServer geoserver) {
        return new ProxifyingURLMangler(geoserver);
    }

    // <!-- URL mangler for workspace/layers accessed through the /ows?service=... service end
    // points -->
    // <bean id="owsDispatcherLocalWorkspaceURLManger"
    // class="org.geoserver.ows.LocalWorkspaceURLMangler">
    // <constructor-arg value="ows"/>
    // </bean>
    public @Bean LocalWorkspaceURLMangler owsDispatcherLocalWorkspaceURLManger() {
        return new LocalWorkspaceURLMangler("ows");
    }

    // <!-- xstream persister factory -->
    // <bean id="xstreamPersisterFactory"
    // class="org.geoserver.config.util.XStreamPersisterFactory"/>
    public @Bean XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    // <!-- workspace local callback -->
    // <bean id="workspaceLocal" class="org.geoserver.ows.LocalWorkspaceCallback">
    // <constructor-arg ref="geoServer"/>
    // </bean>
    public @Bean LocalWorkspaceCallback workspaceLocal(GeoServer gs) {
        return new LocalWorkspaceCallback(gs);
    }

    //
    // <bean id="updateSequenceListener" class="org.geoserver.config.UpdateSequenceListener"
    // lazy-init="false">
    // <constructor-arg ref="geoServer"/>
    // </bean>
    public @Bean UpdateSequenceListener updateSequenceListener(GeoServer geoserver) {
        return new UpdateSequenceListener(geoserver);
    }

    // <bean id="namespaceWorkspaceConsistencyListener"
    // class="org.geoserver.catalog.NamespaceWorkspaceConsistencyListener" lazy-init="false">
    // <constructor-arg ref="catalog"/>
    // </bean>
    public @Bean NamespaceWorkspaceConsistencyListener namespaceWorkspaceConsistencyListener(
            @Qualifier("catalog") Catalog catalog) {
        return new NamespaceWorkspaceConsistencyListener(catalog);
    }

    // <!-- Hides resources in other workspaces when using virtual services -->
    // <bean id="localWorkspaceFilter" class="org.geoserver.ows.LocalWorkspaceCatalogFilter">
    // <constructor-arg ref="rawCatalog"/>
    // </bean>
    public @Bean LocalWorkspaceCatalogFilter localWorkspaceFilter(
            @Qualifier("rawCatalog") Catalog rawCatalog) {
        return new LocalWorkspaceCatalogFilter(rawCatalog);
    }

    // <!-- Alllows to lock the config subsyste to that we serialize accesses to it -->
    // <bean id="configurationLock" class="org.geoserver.GeoServerConfigurationLock"/>
    public @Bean GeoServerConfigurationLock configurationLock() {
        return new GeoServerConfigurationLock();
    }

    // <!-- Automatically injects a env map into the env function -->
    // <bean id="enviromentInjector" class="org.geoserver.ows.EnviromentInjectionCallback"/>
    public @Bean EnviromentInjectionCallback enviromentInjector() {
        return new EnviromentInjectionCallback();
    }

    // <!-- Thread local transfers -->
    // <bean id="dispatcherRequestTransfer"
    // class="org.geoserver.threadlocals.PublicThreadLocalTransfer">
    // <constructor-arg index="0" value="org.geoserver.ows.Dispatcher"/>
    // <constructor-arg index="1" value="REQUEST"/>
    // </bean>
    public @Bean PublicThreadLocalTransfer dispatcherRequestTransfer()
            throws SecurityException, NoSuchFieldException {
        return new PublicThreadLocalTransfer(Dispatcher.class, "REQUEST");
    }

    // <bean id="adminRequestTransfer"
    // class="org.geoserver.threadlocals.AdminRequestThreadLocalTransfer"/>
    public @Bean AdminRequestThreadLocalTransfer adminRequestTransfer() {
        return new AdminRequestThreadLocalTransfer();
    }

    // <bean id="authenticationTransfer"
    // class="org.geoserver.threadlocals.AuthenticationThreadLocalTransfer"/>
    public @Bean AuthenticationThreadLocalTransfer authenticationTransfer() {
        return new AuthenticationThreadLocalTransfer();
    }

    // <bean id="localPublishedTransfer"
    // class="org.geoserver.threadlocals.LocalPublishedThreadLocalTransfer"/>
    public @Bean LocalPublishedThreadLocalTransfer localPublishedTransfer() {
        return new LocalPublishedThreadLocalTransfer();
    }

    // <bean id="localWorkspaceTransfer"
    // class="org.geoserver.threadlocals.LocalWorkspaceThreadLocalTransfer"/>
    public @Bean LocalWorkspaceThreadLocalTransfer localWorkspaceTransfer() {
        return new LocalWorkspaceThreadLocalTransfer();
    }

    // <bean id="envVariableTransfer" class="org.geoserver.threadlocals.EnvVarThreadLocalTransfer"/>
    public @Bean EnvVarThreadLocalTransfer envVariableTransfer() {
        return new EnvVarThreadLocalTransfer();
    }

    // <!-- default style handlers -->
    // <bean id="sldHandler" class="org.geoserver.catalog.SLDHandler"/>
    public @Bean SLDHandler sldHandler() {
        return new SLDHandler();
    }

    // <bean id="sldPackageHandler" class="org.geoserver.catalog.SLDPackageHandler">
    // <constructor-arg ref="sldHandler"/>
    // </bean>
    public @Bean SLDPackageHandler sldPackageHandler(SLDHandler sldHandler) {
        // REVISIT: needs subclassing because constructor is protected
        class AccessibleSLDPackageHandler extends SLDPackageHandler {
            protected AccessibleSLDPackageHandler(SLDHandler sldHandler) {
                super(sldHandler);
            }
        }
        return new AccessibleSLDPackageHandler(sldHandler);
    }

    // <!-- JDBC VirtualTable callback -->
    // <bean id="virtualTableCallback" class="org.geoserver.catalog.VirtualTableCallback"/>
    public @Bean VirtualTableCallback virtualTableCallback() {
        return new VirtualTableCallback();
    }

    // <!-- WFS Cascaded Stored Query callback  -->
    // <bean id="cascadedStoredQueryCallback"
    // class="org.geoserver.catalog.CascadedStoredQueryCallback"/>
    public @Bean CascadedStoredQueryCallback cascadedStoredQueryCallback() {
        return new CascadedStoredQueryCallback();
    }

    // <bean id="capabilitiesCachingHeadersCallback"
    // class="org.geoserver.config.CapabilitiesCacheHeadersCallback">
    // <constructor-arg ref="geoServer"/>
    // </bean>
    public @Bean CapabilitiesCacheHeadersCallback capabilitiesCachingHeadersCallback(GeoServer gs) {
        return new CapabilitiesCacheHeadersCallback(gs);
    }

    // <bean id="serviceResourceProvider" class="org.geoserver.catalog.ServiceResourceProvider">
    //	<constructor-arg ref="geoServer"/>
    // </bean>
    public @Bean ServiceResourceProvider serviceResourceProvider(GeoServer geoServer) {
        return new ServiceResourceProvider(geoServer);
    }

    // <!-- handle time stamping of layer/style creation/modification -->
    // <bean id="catalogTimeStampUpdater" class="org.geoserver.config.CatalogTimeStampUpdater">
    //	<constructor-arg ref="catalog"/>
    // </bean>
    public @Bean CatalogTimeStampUpdater catalogTimeStampUpdater(
            @Qualifier("catalog") Catalog catalog) {
        return new CatalogTimeStampUpdater(catalog);
    }

    // <bean id="nearestMatchWarningAppender"
    // class="org.geoserver.util.NearestMatchWarningAppender"/>
    public @Bean NearestMatchWarningAppender nearestMatchWarningAppender() {
        return new NearestMatchWarningAppender();
    }

    // <bean id="coverageReaderFileConverter"
    // class="org.geoserver.catalog.CoverageReaderFileConverter">
    // <constructor-arg ref="catalog"/>
    // </bean>
    public @Bean CoverageReaderFileConverter coverageReaderFileConverter(
            @Qualifier("catalog") Catalog catalog) {
        return new CoverageReaderFileConverter(catalog);
    }
}
