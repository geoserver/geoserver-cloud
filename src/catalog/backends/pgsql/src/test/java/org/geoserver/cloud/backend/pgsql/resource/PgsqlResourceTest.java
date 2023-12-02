/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.resource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDatabaseMigrations;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceTheoryTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;
import java.util.Objects;

import javax.sql.DataSource;

@RunWith(Theories.class)
class PgsqlResourceTest extends ResourceTheoryTest {

    public @ClassRule static PostgreSQLContainer<?> container =
            new PostgreSQLContainer<>("postgres:15");

    static final String schema = "testschema";
    static PgsqlDatabaseMigrations databaseMigrations;
    static DataSource dataSource;

    public @Rule TemporaryFolder cacheDir = new TemporaryFolder();
    private PgsqlResourceStore store;

    public static @BeforeClass void createDataSource() throws Exception {
        String url = container.getJdbcUrl();
        String username = container.getUsername();
        String password = container.getPassword();
        String driverClassName = container.getDriverClassName();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setPassword(password);
        hikariConfig.setUsername(username);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setSchema(schema);
        dataSource = new HikariDataSource(hikariConfig);
        databaseMigrations =
                new PgsqlDatabaseMigrations()
                        .setSchema(schema)
                        .setDataSource(dataSource)
                        .setCleanDisabled(false);
        databaseMigrations.migrate();
    }

    @DataPoints
    public static String[] testPaths() {
        return new String[] {
            "FileA",
            "FileB",
            "DirC",
            "DirC/FileD",
            "DirE",
            "UndefF",
            "DirC/UndefF",
            "DirE/UndefF",
            "DirE/UndefD/UndefF"
        };
    }

    @After
    public void cleanDb() throws Exception {
        new JdbcTemplate(dataSource).update("DELETE FROM resourcestore WHERE parentid IS NOT NULL");
    }

    @Before
    public void setUp() throws Exception {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        PgsqlLockProvider lockProvider = new PgsqlLockProvider(pgsqlLockRegistry());
        store = new PgsqlResourceStore(cacheDir.getRoot().toPath(), template, lockProvider);
        setupTestData(template);
    }

    private void setupTestData(JdbcTemplate template) throws Exception {
        for (String path : testPaths()) {
            boolean undef = Paths.name(path).contains("Undef");
            if (!undef) {
                boolean dir = Paths.name(path).contains("Dir");
                String parentPath = Paths.parent(path);
                Objects.requireNonNull(parentPath);
                long parentId =
                        template.queryForObject(
                                "SELECT id FROM resources WHERE path = ?", Long.class, parentPath);
                String name = Paths.name(path);
                Resource.Type type = dir ? Type.DIRECTORY : Type.RESOURCE;
                byte[] contents = dir ? null : path.getBytes("UTF-8");
                String sql =
                        """
                		INSERT INTO resourcestore (parentid, name, "type", content)
                		VALUES (?, ?, ?, ?)
                		""";
                template.update(sql, parentId, name, type.toString(), contents);
            }
        }
    }

    /**
     * @return
     */
    private LockRegistry pgsqlLockRegistry() {
        return new JdbcLockRegistry(pgsqlLockRepository());
    }

    @Bean
    LockRepository pgsqlLockRepository() {
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

    @Ignore
    @Override
    public void theoryAlteringFileAltersResource(String path) throws Exception {
        // disabled
    }

    @Ignore
    @Override
    public void theoryAddingFileToDirectoryAddsResource(String path) throws Exception {
        // disabled
    }
}
