/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.geoserver.platform.resource.Resource.Type.DIRECTORY;
import static org.geoserver.platform.resource.Resource.Type.RESOURCE;
import static org.geoserver.platform.resource.Resource.Type.UNDEFINED;
import static org.geoserver.platform.resource.ResourceMatchers.directory;
import static org.geoserver.platform.resource.ResourceMatchers.undefined;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceTheoryTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

/**
 * Note by inheriting from {@link ResourceTheoryTest}, this is a Junit 4 test class and must be
 * {@code public}
 */
@Slf4j
@RunWith(Theories.class)
public class PgconfigResourceTest extends ResourceTheoryTest {

    @ClassRule public static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    @Rule public TemporaryFolder tmpDir = new TemporaryFolder();
    private PgconfigResourceStore store;
    private File cacheDirectory;

    @DataPoints
    public static String[] testPaths() {
        return new String[] {
            "FileA",
            "FileB",
            "DirC",
            "DirC/FileD",
            "DirC/DirC1",
            "DirC/DirC1/DirC2",
            "DirC/DirC1/DirC2/FileC2",
            "DirC/DirC1/DirC2/FileC3",
            "DirE",
            "UndefF",
            "DirC/UndefF",
            "DirE/UndefF",
            "DirE/UndefD/UndefF"
        };
    }

    @BeforeClass
    public static void onetimeSetUp() {
        container.setUp();
    }

    @AfterClass
    public static void oneTimeTeardown() {
        container.tearDown();
    }

    @Before
    public void setUp() throws Exception {
        JdbcTemplate template = container.getTemplate();
        PgconfigLockProvider lockProvider = new PgconfigLockProvider(pgconfigLockRegistry());
        cacheDirectory = tmpDir.newFolder();
        store =
                new PgconfigResourceStore(
                        cacheDirectory.toPath(),
                        template,
                        lockProvider,
                        PgconfigResourceStore.defaultIgnoredDirs());
        setupTestData(template);
    }

    @After
    public void cleanDb() throws Exception {
        DataSource dataSource = container.getDataSource();
        new JdbcTemplate(dataSource).update("DELETE FROM resourcestore WHERE parentid IS NOT NULL");
    }

    private void setupTestData(JdbcTemplate template) throws Exception {
        for (String path : testPaths()) {
            boolean undef = Paths.name(path).contains("Undef");
            if (undef) {
                continue;
            }
            try {
                boolean dir = Paths.name(path).contains("Dir");
                String parentPath = Paths.parent(path);
                Objects.requireNonNull(parentPath);
                long parentId =
                        template.queryForObject(
                                "SELECT id FROM resourcestore WHERE path = ?",
                                Long.class,
                                parentPath);
                Resource.Type type = dir ? Type.DIRECTORY : Type.RESOURCE;
                byte[] contents = dir ? null : path.getBytes("UTF-8");
                String sql =
                        """
						INSERT INTO resourcestore (parentid, path, "type", content)
						VALUES (?, ?, ?, ?)
						""";
                template.update(sql, parentId, path, type.toString(), contents);
            } catch (Exception e) {
                log.error("Error creating {}", path, e);
                throw e;
            }
        }
    }

    /**
     * @return
     */
    private LockRegistry pgconfigLockRegistry() {
        return new JdbcLockRegistry(pgconfigLockRepository());
    }

    LockRepository pgconfigLockRepository() {
        DataSource dataSource = container.getDataSource();
        DefaultLockRepository lockRepository =
                new DefaultLockRepository(dataSource, "test-instance");
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // time in ms to expire dead locks (10k is the default)
        lockRepository.setTimeToLive(300_000);
        return lockRepository;
    }

    @Override
    protected Resource getDirectory() {
        return Arrays.stream(testPaths())
                .filter(path -> Paths.name(path).contains("Dir"))
                .map(store::get)
                .map(PgconfigResource.class::cast)
                .filter(PgconfigResource::isDirectory)
                .findFirst()
                .orElseThrow();
    }

    @Override
    protected Resource getResource() {
        return Arrays.stream(testPaths())
                .filter(path -> Paths.name(path).contains("File"))
                .map(store::get)
                .map(PgconfigResource.class::cast)
                .filter(PgconfigResource::isFile)
                .findFirst()
                .orElseThrow();
    }

    @Override
    protected Resource getUndefined() {
        return Arrays.stream(testPaths())
                .filter(path -> Paths.name(path).contains("UndefF"))
                .map(store::get)
                .map(PgconfigResource.class::cast)
                .filter(PgconfigResource::isUndefined)
                .findFirst()
                .orElseThrow();
    }

    @Override
    protected Resource getResource(String path) {
        return store.get(path);
    }

    @Override
    @Ignore("This behaviour is specific to the file based implementation")
    public void theoryAlteringFileAltersResource(String path) throws Exception {
        // disabled
    }

    @Override
    @Ignore("This behaviour is specific to the file based implementation")
    public void theoryAddingFileToDirectoryAddsResource(String path) {
        // disabled
    }

    @Theory
    public void theoryRenamedDirectoryRenamesChildren(String path) {
        final Resource res = getResource(path);
        assumeThat(res, is(directory()));

        final String newpath = "new/path/to" + path;

        Set<String> childrenRecursive = getRecursiveChildren(res);
        Set<String> expected = replace(path, newpath, childrenRecursive);

        Resource target = getResource(newpath);
        assertThat(target, is(undefined()));
        mkdirs(target.parent());

        boolean renamed = res.renameTo(target);
        assertThat(renamed, is(true));
        assertThat(res, is(undefined()));
        assertThat(target, is(directory()));

        Set<String> childrenRecursiveAfter = getRecursiveChildren(target);
        assertThat(childrenRecursiveAfter, is(equalTo(expected)));

        for (String oldPath : childrenRecursive) {
            Resource old = store.get(oldPath);
            assertThat(old, is(undefined()));
        }
    }

    @Test
    public void testDefaultIgnoredDirs() {
        assertFileSystemDir("temp");
        assertFileSystemDir("tmp");
        assertFileSystemDir("legendsamples");
        assertFileSystemDir("data");
        assertFileSystemDir("logs");
    }

    @Test
    public void testRemoveIgnoredDirs() {
        testRemoveIgnoredDir("temp");
        testRemoveIgnoredDir("tmp");
        testRemoveIgnoredDir("legendsamples");
        testRemoveIgnoredDir("data");
        testRemoveIgnoredDir("logs");
    }

    private void testRemoveIgnoredDir(String ignoredDir) {
        assertFileSystemFile(ignoredDir + "/child1");
        assertFileSystemFile(ignoredDir + "/child2");
        store.remove(ignoredDir);
        assertThat(store.get(ignoredDir), is(undefined()));
        assertThat(store.get(ignoredDir + "/child1"), is(undefined()));
        assertThat(store.get(ignoredDir + "/child2"), is(undefined()));
    }

    @Test
    public void testRemoveIgnoredFiles() {
        assertFileSystemFile("temp/sample.png");
        assertFileSystemFile("tmp/sample.png");
        assertFileSystemFile("legendsamples/sample.png");
        assertFileSystemFile("data/sample.png");
        assertFileSystemFile("logs/sample.png");
    }

    private void assertFileSystemFile(String path) {
        Resource resource = getResource(path);
        assertFileSystemDir(resource.parent().path());
        assertFalse(resource instanceof PgconfigResource);
        assertTrue(resource instanceof PgconfigResourceStore.FileSystemResourceAdaptor);
        assertTrue(
                resource.file()
                        .getAbsolutePath()
                        .startsWith(this.cacheDirectory.getAbsolutePath()));
    }

    private void assertFileSystemDir(String path) {
        Resource resource = getResource(path);
        assertFalse(resource instanceof PgconfigResource);
        assertTrue(resource instanceof PgconfigResourceStore.FileSystemResourceAdaptor);
        assertTrue(
                resource.dir().getAbsolutePath().startsWith(this.cacheDirectory.getAbsolutePath()));
    }

    @Test
    public void testRemoveFileSystemOnlyResource() {
        testRemoveFilesystemOnlyFile("temp/sample.png");
        testRemoveFilesystemOnlyFile("tmp/sample.png");
        testRemoveFilesystemOnlyFile("legendsamples/sample.png");
        testRemoveFilesystemOnlyFile("data/sample.png");
        testRemoveFilesystemOnlyFile("logs/sample.png");
    }

    @Test
    public void testRootResourceReturnsFilesystemResourcesForIgnoredPatterns() {
        testRootResourceFilesystem("temp");
        testRootResourceFilesystem("tmp");
        testRootResourceFilesystem("legendsamples");
        testRootResourceFilesystem("data");
        testRootResourceFilesystem("logs");
    }

    private void testRootResourceFilesystem(String dirname) {
        Resource root = store.get("");
        Resource dir = root.get(dirname);
        Resource file = root.get(dirname + "/sample.png");

        assertEquals(UNDEFINED, dir.getType());
        assertEquals(UNDEFINED, file.getType());
        assertTrue(root instanceof PgconfigResource);
        assertFalse(dir instanceof PgconfigResource);
        assertFalse(file instanceof PgconfigResource);
        assertFalse(dir.get(file.name()) instanceof PgconfigResource);

        Resource parent = file.parent();
        assertEquals(dir, parent);
        assertEquals(root, dir.parent());

        assertTrue(dir.parent().get("workspaces") instanceof PgconfigResource);
    }

    private void testRemoveFilesystemOnlyFile(String path) {
        Resource resource = getResource(path);
        assertFileSystemDir(resource.parent().path());
        resource.file();
        assertEquals(RESOURCE, store.get(path).getType());
        store.remove(path);
        assertEquals(UNDEFINED, store.get(path).getType());
    }

    @Test
    public void trestRemovePathInDatabase() {
        store.get("workspaces").dir();
        store.get("workspaces/ws1").dir();
        store.get("workspaces/ws1/workspace.xml").file();

        assertTrue(store.get("workspaces/ws1/workspace.xml") instanceof PgconfigResource);
        assertEquals(RESOURCE, store.get("workspaces/ws1/workspace.xml").getType());

        assertTrue(store.remove("workspaces/ws1/workspace.xml"));
        assertFalse(store.remove("workspaces/ws1/workspace.xml"));
    }

    @Test
    public void trestMovePathInDatabase() {
        store.get("workspaces").dir();
        store.get("workspaces/ws1").dir();
        store.move("workspaces/ws1", "workspaces/ws2");
        assertEquals(DIRECTORY, store.get("workspaces/ws2").getType());
        assertEquals(UNDEFINED, store.get("workspaces/ws1").getType());
        assertTrue(store.get("workspaces/ws2") instanceof PgconfigResource);
    }

    /**
     * Verify resiliency when moving a resource to the same path. Something that happens for example
     * when NamespaceWorkspaceConsistencyListener blindly updtes a workspace upon a namespace change
     * even if the workspace name is already equal to the namespace prefix
     */
    @Test
    public void trestMoveSameTarget() {
        store.get("workspaces").dir();
        store.get("workspaces/ws1").dir();
        store.get("workspaces/ws1/workspace.xml").file();

        assertTrue(store.move("workspaces/ws1", "workspaces/ws1"));

        assertEquals(DIRECTORY, store.get("workspaces/ws1").getType());
        assertEquals(RESOURCE, store.get("workspaces/ws1/workspace.xml").getType());
        assertTrue(store.get("workspaces/ws2") instanceof PgconfigResource);
    }

    @Test
    public void trestMovePathFilesystemOnly() {
        store.get("legendsamples").dir();
        store.get("legendsamples/sample.png").file();

        assertEquals(RESOURCE, store.get("legendsamples/sample.png").getType());
        store.move("legendsamples/sample.png", "legendsamples/sample2.png");
        assertEquals(UNDEFINED, store.get("legendsamples/sample.png").getType());
        assertEquals(RESOURCE, store.get("legendsamples/sample2.png").getType());
        assertFalse(store.get("legendsamples/sample2.png") instanceof PgconfigResource);
    }

    @Test
    public void trestMovePathFilesystemOnlyToDatabaseIsUnsupported() {
        store.get("legendsamples").dir();
        store.get("legendsamples/sample.png").file();

        store.get("workspaces").dir();
        store.get("workspaces/ws1").dir();
        assertThrows(
                UnsupportedOperationException.class,
                () -> store.move("legendsamples/sample.png", "workspaces/sample2.png"));

        assertThrows(
                UnsupportedOperationException.class,
                () -> store.move("workspaces/ws1", "temp/ws1"));
    }

    @Test
    public void testIgnoresFileSystemOnlyResourcesInDb() throws SQLException {
        // for pre 1.8.1 backwards compatibility, ignore fs-only resources already in the db
        DataSource ds = container.getDataSource();
        String sql =
                """
				INSERT INTO resourcestore (parentid, "type", path, content)
				VALUES (?, ?, ?, ?);
				""";
        try (var c = ds.getConnection();
                var st = c.prepareStatement(sql)) {
            st.setLong(1, 0);
            st.setString(2, Resource.Type.DIRECTORY.name());
            st.setString(3, "temp");
            st.setObject(4, null);
            st.executeUpdate();
        }
        PgconfigResource root = (PgconfigResource) store.get("");
        List<Resource> children = store.list(root);
        assertThat(
                children.stream()
                        .map(Resource::path)
                        .filter(path -> path.contains("temp"))
                        .toList(),
                empty());
    }

    private Set<String> replace(String oldParent, String newParent, Set<String> children) {
        return children.stream()
                .map(p -> newParent + p.substring(oldParent.length()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void mkdirs(Resource dir) {
        final List<String> names = Paths.names(dir.path());
        for (int i = 0; i < names.size(); i++) {
            String[] subpath = names.subList(0, i).toArray(String[]::new);
            String path = Paths.path(subpath);
            store.get(path).dir();
        }
    }

    private Set<String> getRecursiveChildren(Resource res) {
        Set<String> all = new TreeSet<>();
        List<Resource> direct = res.list();
        for (Resource c : direct) {
            all.add(c.path());
            if (c.getType() == Type.DIRECTORY) {
                all.addAll(getRecursiveChildren(c));
            }
        }
        return all;
    }
}
