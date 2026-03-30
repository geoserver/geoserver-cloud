/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.support;

import org.geoserver.cloud.autoconfigure.jndi.SimpleJNDIStaticContextInitializer;
import org.geoserver.cloud.config.jndi.JNDIDataSourceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A {@link Testcontainers test container} based on {@link PostgreSQLContainer} using PostgreSQL 15.
 *
 * <p>Provides connection properties to Spring {@link AbstractApplicationContextRunner} tests and
 * {@code @DynamicPropertySource} tests. For DataSource, JdbcTemplate, and Flyway migration management, use
 * {@link PgconfigTestDatabaseSupport} as a {@code @RegisterExtension}.
 *
 * @since 1.6
 * @see PgconfigTestDatabaseSupport
 */
@SuppressWarnings("java:S119")
public class PgConfigTestContainer extends PostgreSQLContainer {

    /** System property to override the PostgreSQL docker image (e.g. {@code -Dpgconfig.test.image=postgres:17}). */
    public static final String IMAGE_PROPERTY = "pgconfig.test.image";

    private static final String DEFAULT_IMAGE = "postgres:15";

    public PgConfigTestContainer() {
        super(System.getProperty(IMAGE_PROPERTY, DEFAULT_IMAGE));
    }

    @SuppressWarnings("unchecked")
    public <R extends AbstractApplicationContextRunner<?, ?, ?>> R withJdbcUrlConfig(R runner) {
        String url = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        return (R) runner.withPropertyValues( //
                "geoserver.backend.pgconfig.enabled=true", //
                "geoserver.backend.pgconfig.datasource.url=" + url, //
                "geoserver.backend.pgconfig.datasource.username=" + username, //
                "geoserver.backend.pgconfig.datasource.password=" + password //
                );
    }

    @SuppressWarnings("unchecked")
    public <R extends AbstractApplicationContextRunner<?, ?, ?>> R withJndiConfig(R runner) {
        String url = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        return (R) runner
                // enable simplejndi
                .withInitializer(new SimpleJNDIStaticContextInitializer())
                .withConfiguration(AutoConfigurations.of(JNDIDataSourceConfiguration.class))
                .withPropertyValues(
                        "geoserver.backend.pgconfig.enabled: true", //
                        // java:comp/env/jdbc/testdb config properties
                        "jndi.datasources.testdb.enabled: true", //
                        "jndi.datasources.testdb.url: " + url,
                        "jndi.datasources.testdb.username: " + username, //
                        "jndi.datasources.testdb.password: " + password, //
                        // pgconfig backend datasource config using jndi
                        "geoserver.backend.pgconfig.datasource.jndi-name: java:comp/env/jdbc/testdb");
    }

    /**
     * Contribute the following properties defined in the {@literal pgconfigjndi} spring profile
     *
     * <ul>
     *   <li>pgconfig.host
     *   <li>pgconfig.port
     *   <li>pgconfig.database
     *   <li>pgconfig.schema
     *   <li>pgconfig.username
     *   <li>pgconfig.password
     * </ul>
     */
    public void setupDynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("pgconfig.host", this::getHost);
        registry.add("pgconfig.port", () -> this.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registry.add("pgconfig.database", this::getDatabaseName);
        registry.add("pgconfig.schema", () -> "pgconfigtestschema");
        registry.add("pgconfig.username", this::getUsername);
        registry.add("pgconfig.password", this::getPassword);
    }
}
