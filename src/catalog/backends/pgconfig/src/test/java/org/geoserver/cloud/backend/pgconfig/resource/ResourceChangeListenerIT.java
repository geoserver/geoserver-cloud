/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.geoserver.platform.resource.Resource.Type.DIRECTORY;
import static org.geoserver.platform.resource.Resource.Type.RESOURCE;
import static org.geoserver.platform.resource.Resource.Type.UNDEFINED;
import static org.geoserver.platform.resource.ResourceNotification.Kind.ENTRY_MODIFY;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Two pods over one database: this pod reads its rows from an index and runs the listener, the other pod writes through
 * its own store. The other pod hands each of its events to the listener of this pod, marked remote, as the event bus
 * does.
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(ExecutionMode.CONCURRENT)
class ResourceChangeListenerIT {

    private static final String STYLE = "r/s.sld";
    private static final long ONE_MIB = 1024 * 1024;

    /** The tests capturing the log of the listener run one at a time: a capture receives the events of the logger. */
    private static final String LISTENER_LOG = "ResourceChangeListener log";

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File thisPodDirectory;

    @TempDir
    File otherPodDirectory;

    private StatementCounter counter;
    private ResourceStoreCache index;
    private PgconfigResourceStore store;
    private ResourceChangeListener listener;
    private PgconfigResourceStore otherPod;

    @BeforeEach
    void setUp() {
        insert("r", DIRECTORY, null);
        counter = new StatementCounter(db.getDataSource());
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        index = new ResourceStoreCache("gs-resources", PgconfigResourceStore.indexLoader(template), ONE_MIB);
        FileSystemResourceStoreCache localCopies = localCopies(thisPodDirectory);
        // the application context hands the local events of the store to the listener too
        store = newStore(template, localCopies, index, event -> listener.onResourceChanged((ResourceChanged) event));
        listener = new ResourceChangeListener(store, localCopies);
        // the transaction of the other pod still holds its connection when it announces the commit. Through the same
        // data source, the refresh of this pod would run on that connection
        JdbcTemplate otherPodTemplate = new JdbcTemplate(new DelegatingDataSource(db.getDataSource()));
        otherPod = newStore(otherPodTemplate, localCopies(otherPodDirectory), null, this::receivedFromOtherPod);
    }

    private void receivedFromOtherPod(GeoServerEvent event) {
        event.setRemote(true);
        listener.onResourceChanged((ResourceChanged) event);
    }

    @Test
    void remoteCreateSeenWithoutLookup() {
        store.get("r");
        counter.reset();

        write(otherPod, STYLE, "<sld/>");
        int refreshStatements = counter.count();
        counter.reset();
        Type type = store.get(STYLE).getType();

        assertThat(type).isEqualTo(RESOURCE);
        assertThat(counter.count()).isZero();
        assertThat(refreshStatements).isEqualTo(1);
    }

    @Test
    void remoteModifyRefreshesContent() throws IOException {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        assertThat(read(store.get(STYLE))).isEqualTo("<sld/>".getBytes(UTF_8));

        write(otherPod, STYLE, "<sld>changed</sld>");
        counter.reset();
        byte[] content = read(store.get(STYLE));

        assertThat(content).isEqualTo("<sld>changed</sld>".getBytes(UTF_8));
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void remoteDeleteDropsLocalCopy() {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        File localCopy = store.get(STYLE).file();
        assertThat(localCopy).exists();

        assertThat(otherPod.get(STYLE).delete()).isTrue();

        assertThat(localCopy).doesNotExist();
        assertThat(store.get(STYLE).getType()).isEqualTo(UNDEFINED);
    }

    @Test
    void remoteChangeNotifiesListener() {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        List<ResourceNotification> fileNotifications = listenTo(store, STYLE);
        List<ResourceNotification> directoryNotifications = listenTo(store, "r");

        write(otherPod, STYLE, "<sld>changed</sld>");

        assertThat(fileNotifications)
                .singleElement()
                .extracting(ResourceNotification::getKind)
                .isEqualTo(ENTRY_MODIFY);
        assertThat(directoryNotifications)
                .singleElement()
                .extracting(ResourceNotification::getKind)
                .isEqualTo(ENTRY_MODIFY);
    }

    @Test
    void localEventIgnored() {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        File localCopy = store.get(STYLE).file();
        List<ResourceNotification> notifications = listenTo(store, STYLE);
        ResourceChanged localDelete = ResourceChanged.createLocal(STYLE, Kind.DELETED, RESOURCE, 0L, List.of());
        counter.reset();

        listener.onResourceChanged(localDelete);

        assertThat(counter.count()).isZero();
        assertThat(localCopy).exists();
        assertThat(notifications).isEmpty();
        assertThat(store.get(STYLE).getType()).isEqualTo(RESOURCE);

        // the store notifies its listeners of its own write. The listener does not notify them again
        write(store, STYLE, "<sld>changed</sld>");
        assertThat(notifications).hasSize(1);
    }

    @Test
    @ResourceLock(LISTENER_LOG)
    void failingRefreshClearsAndContinues() {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        JdbcTemplate refusing = new JdbcTemplate(refusingConnections(db.getDataSource()));
        ResourceStoreCache loadedIndex =
                new ResourceStoreCache("gs-resources", PgconfigResourceStore.indexLoader(db.getTemplate()), ONE_MIB);
        FileSystemResourceStoreCache localCopies = localCopies(thisPodDirectory);
        PgconfigResourceStore failing = newStore(refusing, localCopies, loadedIndex, event -> {});
        ResourceChangeListener failingListener = new ResourceChangeListener(failing, localCopies);
        assertThat(loadedIndex.find(STYLE)).isPresent();
        List<ResourceNotification> notifications = listenTo(failing, STYLE);
        ResourceChanged remoteModify = ResourceChanged.createLocal(STYLE, Kind.MODIFIED, RESOURCE, 0L, List.of());
        remoteModify.setRemote(true);

        try (LogCapture log = LogCapture.of(ResourceChangeListener.class.getName())) {
            assertThatCode(() -> failingListener.onResourceChanged(remoteModify))
                    .doesNotThrowAnyException();

            assertThat(log.messages(Level.WARN)).singleElement().asString().contains(STYLE);
        }
        assertThat(loadedIndex.size()).isZero();
        assertThat(notifications).hasSize(1);
    }

    // removing the local copy needs no database
    @Test
    @ResourceLock(LISTENER_LOG)
    void failingRefreshDropsLocalCopy() {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        JdbcTemplate refusing = new JdbcTemplate(refusingConnections(db.getDataSource()));
        ResourceStoreCache loadedIndex =
                new ResourceStoreCache("gs-resources", PgconfigResourceStore.indexLoader(db.getTemplate()), ONE_MIB);
        FileSystemResourceStoreCache localCopies = localCopies(thisPodDirectory);
        File localCopy = store.get(STYLE).file();
        PgconfigResourceStore failing = newStore(refusing, localCopies, loadedIndex, event -> {});
        ResourceChangeListener failingListener = new ResourceChangeListener(failing, localCopies);
        ResourceChanged remoteDelete = ResourceChanged.createLocal(STYLE, Kind.DELETED, RESOURCE, 0L, List.of());
        remoteDelete.setRemote(true);

        try (LogCapture log = LogCapture.of(ResourceChangeListener.class.getName())) {
            failingListener.onResourceChanged(remoteDelete);
        }

        assertThat(localCopy).doesNotExist();
    }

    @Test
    @ResourceLock(LISTENER_LOG)
    void failingListenerIsLogged() {
        insert(STYLE, RESOURCE, "<sld/>".getBytes(UTF_8));
        store.getResourceNotificationDispatcher().addListener(STYLE, notification -> {
            throw new IllegalStateException("listener failed");
        });

        try (LogCapture log = LogCapture.of(ResourceChangeListener.class.getName())) {
            assertThatCode(() -> write(otherPod, STYLE, "<sld>changed</sld>")).doesNotThrowAnyException();

            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("notify the listeners");
        }
    }

    // the event of the deletion lists the descendants of the directory
    @Test
    void remoteDirectoryDeleteDropsSubtree() {
        insert("r/d", DIRECTORY, null);
        insert("r/d/a.sld", RESOURCE, "<sld/>".getBytes(UTF_8));
        assertThat(store.get("r/d/a.sld").getType()).isEqualTo(RESOURCE);

        assertThat(otherPod.get("r/d").delete()).isTrue();
        counter.reset();

        assertThat(store.get("r/d").getType()).isEqualTo(UNDEFINED);
        assertThat(store.get("r/d/a.sld").getType()).isEqualTo(UNDEFINED);
        assertThat(index.find("r/d/a.sld")).isEmpty();
        assertThat(counter.count()).isZero();
    }

    private static List<ResourceNotification> listenTo(PgconfigResourceStore pod, String path) {
        List<ResourceNotification> notifications = new ArrayList<>();
        pod.getResourceNotificationDispatcher().addListener(path, notifications::add);
        return notifications;
    }

    private static void write(PgconfigResourceStore pod, String path, String contents) {
        try (OutputStream out = pod.get(path).out()) {
            out.write(contents.getBytes(UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] read(Resource resource) throws IOException {
        try (InputStream in = resource.in()) {
            return in.readAllBytes();
        }
    }

    private PgconfigResourceStore newStore(
            JdbcTemplate template,
            FileSystemResourceStoreCache localCopies,
            ResourceStoreCache storeIndex,
            Consumer<GeoServerEvent> eventPublisher) {
        Predicate<String> dbBackedFiles =
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns());
        ResourceChangePublisher changePublisher =
                new ResourceChangePublisher(new SynchronizedResourceNotificationDispatcher(), eventPublisher);
        return new PgconfigResourceStore(
                localCopies,
                template,
                new PgconfigLockProvider(lockRegistry()),
                PgconfigResourceStore.defaultIgnoredResources(),
                dbBackedFiles,
                new DataSourceTransactionManager(template.getDataSource()),
                changePublisher,
                storeIndex);
    }

    private static FileSystemResourceStoreCache localCopies(File directory) {
        Predicate<String> dbBackedFiles =
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns());
        return FileSystemResourceStoreCache.ofProvidedDirectory(directory.toPath(), dbBackedFiles);
    }

    private JdbcLockRegistry lockRegistry() {
        DataSource dataSource = db.getDataSource();
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource, "test-instance");
        lockRepository.setPrefix("RESOURCE_");
        lockRepository.setTransactionManager(new DataSourceTransactionManager(dataSource));
        lockRepository.afterPropertiesSet();
        lockRepository.afterSingletonsInstantiated();
        return new JdbcLockRegistry(lockRepository);
    }

    /** A data source failing to open connections, like a database gone away. */
    private static DataSource refusingConnections(DataSource target) {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class}, (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        throw new SQLException("the database refuses connections");
                    }
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private void insert(String path, Type type, byte[] content) {
        String sql =
                """
                INSERT INTO resourcestore (parentid, path, "type", content)
                VALUES ((SELECT id FROM resourcestore WHERE path = ?), ?, ?, ?)
                """;
        String parentPath = Paths.parent(path);
        db.getTemplate().update(sql, parentPath, path, type.toString(), content);
    }
}
