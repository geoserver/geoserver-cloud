/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.jndi;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

/**
 * A <a href="https://www.testcontainers.org">Testcontainers</a> container to run
 * georchestra's LDAP server based on {@code georchestra/ldap:latest}.
 * <p>
 * Use with JUnit 5's {@code @Container} annotation.
 * <p>
 * Get the host mapped port for {@code 389} with {@link #getMappedPort(int)
 * getMappedPort(389)}, or directly with {@link #getMappedLdapPort()}
 */
class GeorchestraLdapContainer extends GenericContainer<GeorchestraLdapContainer> {

    public GeorchestraLdapContainer() {
        this(DockerImageName.parse("georchestra/ldap:latest"));
    }

    GeorchestraLdapContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        withExposedPorts(389);
        addEnv("SLAPD_ORGANISATION", "georchestra");
        addEnv("SLAPD_DOMAIN", "georchestra.org");
        addEnv("SLAPD_PASSWORD", "secret");
        addEnv("SLAPD_LOG_LEVEL", "32768");

        withCreateContainerCmdModifier(it -> it.withName("testcontainers-georchestra-ldap-" + Base58.randomString(8)));

        // this is faster than Wait.forHealthcheck() which is set every 30secs in
        // georchestra/ldap's Dockerfile
        waitingFor(Wait.forLogMessage(".*slapd starting.*\\n", 1));
    }

    public int getMappedLdapPort() {
        return getMappedPort(389);
    }
}
