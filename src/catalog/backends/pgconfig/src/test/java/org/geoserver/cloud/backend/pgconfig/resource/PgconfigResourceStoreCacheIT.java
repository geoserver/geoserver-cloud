/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Execution(ExecutionMode.CONCURRENT)
class PgconfigResourceStoreCacheIT {

    private static final String STYLE = "workspaces/ws/styles/a.sld";
    private static final long ONE_MIB = 1024 * 1024;
    private static final String LOADER_LOG = "StylesFolderLoader log";

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File cacheDirectory;

    private StatementCounter counter;
    private ResourceStoreCache index;
    private PgconfigResourceStore store;

    @BeforeEach
    void setUp() {
        insertRows("workspaces", "workspaces/ws", "workspaces/ws/styles");
        insertFile(STYLE, "<sld/>");
        counter = new StatementCounter(db.getDataSource());
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        index = new ResourceStoreCache("gs-resources", PgconfigResourceStore.indexLoader(template), ONE_MIB);
        store = newStore(template, index);
    }

    @Test
    void indexLoadsInOneStatement() {
        Resource style = store.get(STYLE);

        assertThat(style.getType()).isEqualTo(RESOURCE);
        assertThat(counter.count()).isEqualTo(1);

        counter.reset();
        Resource absent = store.get("absent/path");

        assertThat(absent.getType()).isEqualTo(UNDEFINED);
        assertThat(counter.count()).isZero();
    }

    @Test
    void contentReadOnce() throws IOException {
        Resource style = store.get(STYLE);
        counter.reset();

        byte[] first = read(style);
        byte[] second = read(style);

        assertThat(second).isEqualTo(first).isEqualTo("<sld/>".getBytes(UTF_8));
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void listFromIndex() {
        Resource workspaces = store.get("workspaces");
        counter.reset();

        List<Resource> children = workspaces.list();

        assertThat(children).extracting(Resource::path).containsExactly("workspaces/ws");
        assertThat(counter.count()).isZero();
    }

    // the insert of the file sets the mtime of its directory, through a trigger
    @Test
    void writeUpdatesIndex() {
        store.get(STYLE);

        write("a/b.txt", "b");
        counter.reset();
        Type fileType = store.get("a/b.txt").getType();
        long directoryMtime = store.get("a").lastmodified();

        assertThat(counter.count()).isZero();
        assertThat(fileType).isEqualTo(RESOURCE);
        assertThat(directoryMtime).isEqualTo(mtimeInDatabase("a"));
    }

    @Test
    void deleteRemovesSubtree() {
        store.get(STYLE);

        assertThat(store.get("workspaces/ws").delete()).isTrue();
        counter.reset();

        assertThat(store.get("workspaces/ws").getType()).isEqualTo(UNDEFINED);
        assertThat(store.get(STYLE).getType()).isEqualTo(UNDEFINED);
        assertThat(store.get("workspaces").lastmodified()).isEqualTo(mtimeInDatabase("workspaces"));
        assertThat(counter.count()).isZero();
    }

    @Test
    void renameRepathsSubtree() {
        store.get(STYLE);

        assertThat(store.get("workspaces/ws").renameTo(store.get("workspaces/renamed")))
                .isTrue();
        counter.reset();

        assertThat(store.get("workspaces/ws").getType()).isEqualTo(UNDEFINED);
        assertThat(store.get(STYLE).getType()).isEqualTo(UNDEFINED);
        assertThat(store.get("workspaces/renamed").getType()).isEqualTo(DIRECTORY);
        Resource moved = store.get("workspaces/renamed/styles/a.sld");
        assertThat(moved.getType()).isEqualTo(RESOURCE);
        long movedMtime = moved.lastmodified();
        assertThat(counter.count()).isZero();
        // the update of its path sets the mtime of each moved row, through a trigger
        assertThat(movedMtime).isEqualTo(mtimeInDatabase("workspaces/renamed/styles/a.sld"));
    }

    // a change made with SQL reaches neither the store nor the events
    @Test
    void reloadNotifiesListeners() throws IOException {
        List<ResourceNotification.Kind> notified = new ArrayList<>();
        Resource style = store.get(STYLE);
        read(style);
        style.addListener(notification -> notified.add(notification.getKind()));
        String sql = "UPDATE resourcestore SET content = ?, mtime = mtime + interval '1 second' WHERE path = ?";
        db.getTemplate().update(sql, "<changed/>".getBytes(UTF_8), STYLE);

        index.clear();
        byte[] content = read(store.get(STYLE));

        assertThat(notified).containsExactly(ENTRY_MODIFY);
        assertThat(content).isEqualTo("<changed/>".getBytes(UTF_8));
    }

    // a lookup within a transaction sees its rows; a lookup outside it reads the index
    @Test
    void transactionReadsDatabase() {
        store.get(STYLE);
        TransactionTemplate transaction =
                new TransactionTemplate(new DataSourceTransactionManager(counter.dataSource()));

        Type typeInTransaction = transaction.execute(status -> {
            insertWith(new JdbcTemplate(counter.dataSource()), "workspaces/inserted.txt", RESOURCE, new byte[0]);
            return store.get("workspaces/inserted.txt").getType();
        });

        assertThat(typeInTransaction).isEqualTo(RESOURCE);
        assertThat(store.get("workspaces/inserted.txt").getType()).isEqualTo(UNDEFINED);
    }

    @Test
    void disabledQueriesEachLookup() {
        PgconfigResourceStore withoutIndex = newStore(new JdbcTemplate(counter.dataSource()), null);

        withoutIndex.get(STYLE);
        withoutIndex.get(STYLE);

        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    void indexHasFileSizes() {
        insertRows("sizes");
        insert("sizes/a.bin", RESOURCE, new byte[1_234]);

        assertThat(index.find("sizes/a.bin")).map(ResourceRow::size).contains(1_234L);
        assertThat(index.find("sizes")).map(ResourceRow::size).contains(0L);

        write("sizes/a.bin", "0123456789");

        assertThat(index.find("sizes/a.bin")).map(ResourceRow::size).contains(10L);
    }

    @Test
    void stylesFolderOneQuery() throws IOException {
        List<String> globalStyles = insertStylesFolder("styles", 20);
        insertRows("workspaces/ws1");
        List<String> workspaceStyles = insertStylesFolder("workspaces/ws1/styles", 5);
        store.get(STYLE);
        counter.reset();

        store.get(globalStyles.get(0)).file();

        assertThat(counter.count()).isEqualTo(1);
        for (String path : globalStyles) {
            assertCopyMatchesRow(path);
        }
        counter.reset();
        List<String> otherGlobalStyles = globalStyles.subList(1, globalStyles.size());
        for (String path : otherGlobalStyles) {
            store.get(path).file();
        }
        assertThat(counter.count()).isZero();

        for (String path : workspaceStyles) {
            store.get(path).file();
        }
        assertThat(counter.count()).isEqualTo(1);
        for (String path : workspaceStyles) {
            assertCopyMatchesRow(path);
        }
    }

    // the copies to write add up to 180 bytes, then 120: above the bound, each file is read alone
    @Test
    void largeFolderFileByFile() throws IOException {
        PgconfigResourceStore bounded = newStore(new JdbcTemplate(counter.dataSource()), index, 100);
        insertRows("styles");
        List<String> styles = List.of("styles/a.sld", "styles/b.sld", "styles/c.sld");
        for (String path : styles) {
            insert(path, RESOURCE, new byte[60]);
        }
        bounded.get(STYLE);

        for (String path : styles) {
            counter.reset();
            bounded.get(path).file();
            assertThat(counter.count()).as(path).isEqualTo(1);
            assertCopyMatchesRow(path);
        }
    }

    // a change made through another instance reaches the index, and leaves the local copies outdated
    @Test
    void outdatedCopyRewritten() throws IOException {
        List<String> styles = insertStylesFolder("styles", 3);
        store.get(styles.get(0)).file();
        List<String> changed = styles.subList(1, 3);
        String sql = "UPDATE resourcestore SET content = ?, mtime = mtime + interval '1 second' WHERE path = ?";
        for (String path : changed) {
            db.getTemplate().update(sql, "<changed/>".getBytes(UTF_8), path);
        }
        store.refresh(changed);
        counter.reset();

        store.get(changed.get(0)).file();

        assertThat(counter.count()).isEqualTo(1);
        for (String path : changed) {
            assertCopyMatchesRow(path);
        }
    }

    @Test
    void concurrentFolderOneLoad() throws Exception {
        List<String> styles = insertStylesFolder("styles", 8);
        store.get(STYLE);
        counter.reset();
        CyclicBarrier start = new CyclicBarrier(styles.size());
        ExecutorService threads = Executors.newFixedThreadPool(styles.size());
        try {
            List<Future<File>> files = new ArrayList<>();
            for (String path : styles) {
                files.add(threads.submit(() -> {
                    start.await();
                    return store.get(path).file();
                }));
            }
            for (Future<File> file : files) {
                file.get(10, TimeUnit.SECONDS);
            }
        } finally {
            threads.shutdownNow();
        }

        assertThat(counter.count()).isEqualTo(1);
        for (String path : styles) {
            assertCopyMatchesRow(path);
        }
    }

    @Test
    void folderLoadSkipsSubfolders() throws IOException {
        List<String> styles = insertStylesFolder("styles", 2);
        insertRows("styles/icons");
        insert("styles/icons/a.png", RESOURCE, new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        store.get(STYLE);
        counter.reset();

        store.get(styles.get(0)).file();

        assertThat(counter.count()).isEqualTo(1);
        assertCopyMatchesRow(styles.get(1));
        assertThat(localCopy("styles/icons/a.png")).doesNotExist();

        counter.reset();
        store.get("styles/icons/a.png").file();

        assertThat(counter.count()).isEqualTo(1);
        assertCopyMatchesRow("styles/icons/a.png");
    }

    // a directory holding a file at the path of a copy makes the write of that copy fail
    @Test
    @ResourceLock(LOADER_LOG)
    void unwritableCopyFailsAlone() throws IOException {
        List<String> styles = insertStylesFolder("styles", 3);
        String first = styles.get(0);
        String unwritable = styles.get(1);
        Files.createDirectories(localCopy(unwritable));
        Files.writeString(localCopy(unwritable).resolve("leftover.txt"), "a file in the way");
        store.get(STYLE);
        counter.reset();

        try (LogCapture log = LogCapture.of(StylesFolderLoader.class.getName())) {
            File copy = store.get(first).file();

            assertThat(counter.count()).isEqualTo(1);
            assertThat(log.messages(Level.WARN)).anyMatch(message -> message.contains(unwritable));
            assertThat(copy).hasBinaryContent(contentInDatabase(first));
            assertCopyMatchesRow(styles.get(2));
            Resource unwritableResource = store.get(unwritable);
            assertThatThrownBy(unwritableResource::file).isInstanceOf(DirectoryNotEmptyException.class);
        }
        assertThat(localCopy("styles")).isDirectoryNotContaining("glob:**.tmp");
    }

    // a file name above 255 bytes fits in the database, and not on the disk of the pod
    @Test
    @ResourceLock(LOADER_LOG)
    void longNameFailsAlone() throws IOException {
        List<String> styles = insertStylesFolder("styles", 2);
        String longName = "styles/%s.sld".formatted("a".repeat(300));
        insertFile(longName, "<sld/>");
        // as after an earlier copy in the folder: without the folder, the disk reports a missing folder instead
        Files.createDirectories(localCopy("styles"));
        store.get(STYLE);
        counter.reset();

        try (LogCapture log = LogCapture.of(StylesFolderLoader.class.getName())) {
            File copy = store.get(styles.get(0)).file();

            assertThat(counter.count()).isEqualTo(1);
            assertThat(log.messages(Level.WARN)).anyMatch(message -> message.contains(longName));
            assertThat(copy).hasBinaryContent(contentInDatabase(styles.get(0)));
            assertCopyMatchesRow(styles.get(1));
            Resource longNamed = store.get(longName);
            assertThatThrownBy(longNamed::file)
                    .isInstanceOf(FileSystemException.class)
                    .hasMessageContaining(Paths.name(longName));
        }
    }

    // a directory resource replaced by a file of the same path can leave an empty directory in the local copies
    @Test
    void emptyDirectoryReplaced() throws IOException {
        List<String> styles = insertStylesFolder("styles", 2);
        insertRows("templates");
        String template = "templates/header.ftl";
        insertFile(template, "<#-- header -->");
        emptyDirectoryAt(styles.get(1));
        emptyDirectoryAt(template);
        store.get(STYLE);

        store.get(styles.get(0)).file();
        assertCopyMatchesRow(styles.get(1));

        store.get(template).file();
        assertCopyMatchesRow(template);
    }

    @Test
    void otherFoldersFileByFile() throws IOException {
        insertRows("templates");
        List<String> templates = List.of("templates/header.ftl", "templates/footer.ftl");
        for (String path : templates) {
            insertFile(path, "<#-- %s -->".formatted(path));
        }
        store.get(STYLE);

        for (String path : templates) {
            counter.reset();
            store.get(path).file();
            assertThat(counter.count()).as(path).isEqualTo(1);
            assertCopyMatchesRow(path);
        }
    }

    private PgconfigResourceStore newStore(JdbcTemplate template, ResourceStoreCache index) {
        return newStore(template, index, StylesFolderLoader.MAX_FOLDER_BYTES);
    }

    private PgconfigResourceStore newStore(JdbcTemplate template, ResourceStoreCache index, long maxStylesFolderBytes) {
        Predicate<String> dbBackedFiles =
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns());
        FileSystemResourceStoreCache localCopies =
                FileSystemResourceStoreCache.ofProvidedDirectory(cacheDirectory.toPath(), dbBackedFiles);
        DataSource dataSource = template.getDataSource();
        ResourceChangePublisher changePublisher =
                new ResourceChangePublisher(new SimpleResourceNotificationDispatcher(), event -> {});
        return new PgconfigResourceStore(
                localCopies,
                template,
                new PgconfigLockProvider(lockRegistry()),
                PgconfigResourceStore.defaultIgnoredResources(),
                dbBackedFiles,
                new DataSourceTransactionManager(dataSource),
                changePublisher,
                index,
                maxStylesFolderBytes);
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

    private void insertRows(String... directories) {
        for (String directory : directories) {
            insert(directory, DIRECTORY, null);
        }
    }

    private void insertFile(String path, String content) {
        insert(path, RESOURCE, content.getBytes(UTF_8));
    }

    /** Inserts a directory holding style files, and returns the paths of the files. */
    private List<String> insertStylesFolder(String folder, int styles) {
        insertRows(folder);
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < styles; i++) {
            String path = "%s/style%02d.sld".formatted(folder, i);
            insertFile(path, "<sld name=\"%s\"/>".formatted(path));
            paths.add(path);
        }
        return paths;
    }

    private void insert(String path, Type type, byte[] content) {
        insertWith(db.getTemplate(), path, type, content);
    }

    private static void insertWith(JdbcTemplate template, String path, Type type, byte[] content) {
        String sql =
                """
                INSERT INTO resourcestore (parentid, path, "type", content)
                VALUES ((SELECT id FROM resourcestore WHERE path = ?), ?, ?, ?)
                """;
        String parentPath = Paths.parent(path);
        template.update(sql, parentPath, path, type.toString(), content);
    }

    // the column holds UTC wall-clock times
    private long mtimeInDatabase(String path) {
        String sql = "SELECT mtime FROM resourcestore WHERE path = ?";
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Timestamp mtime =
                db.getTemplate().queryForObject(sql, (resultSet, rowNum) -> resultSet.getTimestamp("mtime", utc), path);
        return mtime.getTime();
    }

    private byte[] contentInDatabase(String path) {
        String sql = "SELECT content FROM resourcestore WHERE path = ?";
        return db.getTemplate().queryForObject(sql, byte[].class, path);
    }

    /** Asserts that the local copy of a file holds the content of its row, stamped with the mtime of the row. */
    private void assertCopyMatchesRow(String path) throws IOException {
        Path copy = localCopy(path);
        assertThat(copy).as(path).hasBinaryContent(contentInDatabase(path));
        assertThat(Files.getLastModifiedTime(copy).toMillis()).as(path).isEqualTo(mtimeInDatabase(path));
    }

    private Path localCopy(String path) {
        return cacheDirectory.toPath().resolve(path);
    }

    /**
     * Creates an empty directory at the local path of a file, stamped with the mtime of its row: a directory with the
     * mtime of the row must not pass for the copy.
     */
    private void emptyDirectoryAt(String path) throws IOException {
        Path directory = Files.createDirectories(localCopy(path));
        Files.setLastModifiedTime(directory, FileTime.fromMillis(mtimeInDatabase(path)));
    }

    private void write(String path, String contents) {
        try (OutputStream out = store.get(path).out()) {
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
}
