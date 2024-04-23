/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.geoserver.platform.resource.ResourceMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

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
public class PgsqlResourceTest extends ResourceTheoryTest {

    public @ClassRule static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    public @Rule TemporaryFolder cacheDir = new TemporaryFolder();
    private PgsqlResourceStore store;

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
        PgsqlLockProvider lockProvider = new PgsqlLockProvider(pgsqlLockRegistry());
        File newFolder = cacheDir.newFolder();
        store = new PgsqlResourceStore(newFolder.toPath(), template, lockProvider);
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
    private LockRegistry pgsqlLockRegistry() {
        return new JdbcLockRegistry(pgsqlLockRepository());
    }

    LockRepository pgsqlLockRepository() {
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
                .map(PgsqlResource.class::cast)
                .filter(PgsqlResource::isDirectory)
                .findFirst()
                .orElseThrow();
    }

    @Override
    protected Resource getResource() {
        return Arrays.stream(testPaths())
                .filter(path -> Paths.name(path).contains("File"))
                .map(store::get)
                .map(PgsqlResource.class::cast)
                .filter(PgsqlResource::isFile)
                .findFirst()
                .orElseThrow();
    }

    @Override
    protected Resource getUndefined() {
        return Arrays.stream(testPaths())
                .filter(path -> Paths.name(path).contains("UndefF"))
                .map(store::get)
                .map(PgsqlResource.class::cast)
                .filter(PgsqlResource::isUndefined)
                .findFirst()
                .orElseThrow();
    }

    @Override
    protected Resource getResource(String path) throws Exception {
        return store.get(path);
    }

    @Override
    @Ignore("This behaviour is specific to the file based implementation")
    public void theoryAlteringFileAltersResource(String path) throws Exception {
        // disabled
    }

    @Override
    @Ignore("This behaviour is specific to the file based implementation")
    public void theoryAddingFileToDirectoryAddsResource(String path) throws Exception {
        // disabled
    }

    @Theory
    public void theoryRenamedDirectoryRenamesChildren(String path) throws Exception {
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

    private Set<String> replace(String oldParent, String newParent, Set<String> children) {
        return children.stream()
                .map(
                        p -> {
                            String newpath = newParent + p.substring(oldParent.length());
                            return newpath;
                        })
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
