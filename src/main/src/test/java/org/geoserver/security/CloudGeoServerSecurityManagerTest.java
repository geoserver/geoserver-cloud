/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cloud.autoconfigure.main.GeoServerMainSecurityAutoConfiguration;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.security.RolesChanged;
import org.geoserver.cloud.event.security.UsersAndGroupsChanged;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.FileSystemWatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.event.EventListener;

/**
 * Verifies {@link CloudGeoServerSecurityManager} broadcasts changes to the contents of user/group and role services to
 * the other service instances, and refreshes the named service when such a change arrives from another instance.
 */
class CloudGeoServerSecurityManagerTest {

    private static final String USER_GROUP_SERVICE = XMLUserGroupService.DEFAULT_NAME;
    private static final String ROLE_SERVICE = XMLRoleService.DEFAULT_NAME;

    @TempDir
    File dataDirectory;

    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = createContextRunner(dataDirectory);
    }

    @Test
    void addingAUserBroadcastsUsersAndGroupsChanged() {
        runInitialized(context -> {
            addUser(securityManager(context), "newuser");

            UsersAndGroupsChanged event = publishedEvents(context).expectOne(UsersAndGroupsChanged.class);
            assertThat(event.getServiceName()).isEqualTo(USER_GROUP_SERVICE);
            assertThat(event.isLocal()).isTrue();
        });
    }

    @Test
    void addingARoleBroadcastsRolesChanged() {
        runInitialized(context -> {
            addRole(securityManager(context), "ROLE_NEW");

            RolesChanged event = publishedEvents(context).expectOne(RolesChanged.class);
            assertThat(event.getServiceName()).isEqualTo(ROLE_SERVICE);
            assertThat(event.isLocal()).isTrue();
        });
    }

    @Test
    void storingWithoutChangesBroadcastsNothing() {
        runInitialized(context -> {
            CloudGeoServerSecurityManager manager = securityManager(context);
            manager.loadUserGroupService(USER_GROUP_SERVICE).createStore().store();
            manager.loadRoleService(ROLE_SERVICE).createStore().store();

            assertThat(publishedEvents(context).all()).isEmpty();
        });
    }

    @Test
    void remoteUsersAndGroupsChangedRefreshesTheNamedService() {
        runInitialized(context -> {
            CloudGeoServerSecurityManager manager = securityManager(context);
            assertThat(userNames(manager)).doesNotContain("newuser");

            addUserFromAnotherInstance(manager, "newuser");
            assertThat(userNames(manager))
                    .as("the cached service is stale until the event arrives")
                    .doesNotContain("newuser");

            context.publishEvent(remote(UsersAndGroupsChanged.createLocal(USER_GROUP_SERVICE)));

            assertThat(userNames(manager)).contains("newuser");
        });
    }

    @Test
    void remoteRolesChangedRefreshesTheNamedService() {
        runInitialized(context -> {
            CloudGeoServerSecurityManager manager = securityManager(context);
            assertThat(roleNames(manager)).doesNotContain("ROLE_NEW");

            addRoleFromAnotherInstance(manager, "ROLE_NEW");
            assertThat(roleNames(manager))
                    .as("the cached service is stale until the event arrives")
                    .doesNotContain("ROLE_NEW");

            context.publishEvent(remote(RolesChanged.createLocal(ROLE_SERVICE)));

            assertThat(roleNames(manager)).contains("ROLE_NEW");
        });
    }

    @Test
    void localEventsDoNotRefreshServices() {
        runInitialized(context -> {
            CloudGeoServerSecurityManager manager = securityManager(context);
            addUserFromAnotherInstance(manager, "newuser");
            addRoleFromAnotherInstance(manager, "ROLE_NEW");

            context.publishEvent(UsersAndGroupsChanged.createLocal(USER_GROUP_SERVICE));
            context.publishEvent(RolesChanged.createLocal(ROLE_SERVICE));

            assertThat(userNames(manager)).doesNotContain("newuser");
            assertThat(roleNames(manager)).doesNotContain("ROLE_NEW");
        });
    }

    @Test
    void remoteEventsForUnknownServicesAreIgnored() {
        runInitialized(context -> assertThatCode(() -> {
                    context.publishEvent(remote(UsersAndGroupsChanged.createLocal("no-such-service")));
                    context.publishEvent(remote(RolesChanged.createLocal("no-such-service")));
                })
                .doesNotThrowAnyException());
    }

    private void runInitialized(ContextConsumer<AssertableApplicationContext> test) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            securityManager(context).reload();
            publishedEvents(context).clear();
            test.accept(context);
        });
    }

    private CloudGeoServerSecurityManager securityManager(AssertableApplicationContext context) {
        return context.getBean(CloudGeoServerSecurityManager.class);
    }

    private PublishedEvents publishedEvents(AssertableApplicationContext context) {
        return context.getBean(PublishedEvents.class);
    }

    private void addUser(GeoServerSecurityManager manager, String username) throws Exception {
        addUser(manager.loadUserGroupService(USER_GROUP_SERVICE), username);
    }

    /**
     * Mimics another service instance writing to the shared storage:
     * {@link GeoServerSecurityManager#loadUserGroupServices()} creates fresh, uncached service instances over the same
     * files, and storing through them leaves the service cached by the manager untouched.
     */
    private void addUserFromAnotherInstance(GeoServerSecurityManager manager, String username) throws Exception {
        GeoServerUserGroupService detached = manager.loadUserGroupServices().stream()
                .filter(service -> USER_GROUP_SERVICE.equals(service.getName()))
                .findFirst()
                .orElseThrow();
        addUser(detached, username);
    }

    private void addUser(GeoServerUserGroupService service, String username) throws Exception {
        GeoServerUserGroupStore store = service.createStore();
        GeoServerUser user = store.createUserObject(username, "s3cret", true);
        store.addUser(user);
        store.store();
    }

    private void addRole(GeoServerSecurityManager manager, String roleName) throws IOException {
        addRole(manager.loadRoleService(ROLE_SERVICE), roleName);
    }

    /**
     * Mimics another service instance writing to the shared storage: the helper creates a fresh, uncached role service
     * over the same files, and storing through it leaves the service cached by the manager untouched.
     */
    private void addRoleFromAnotherInstance(GeoServerSecurityManager manager, String roleName) throws IOException {
        GeoServerRoleService detached = manager.roleServiceHelper.load(ROLE_SERVICE);
        addRole(detached, roleName);
    }

    private void addRole(GeoServerRoleService service, String roleName) throws IOException {
        GeoServerRoleStore store = service.createStore();
        store.addRole(store.createRoleObject(roleName));
        store.store();
    }

    private List<String> userNames(GeoServerSecurityManager manager) throws IOException {
        SortedSet<GeoServerUser> users =
                manager.loadUserGroupService(USER_GROUP_SERVICE).getUsers();
        return users.stream().map(GeoServerUser::getUsername).toList();
    }

    private List<String> roleNames(GeoServerSecurityManager manager) throws IOException {
        SortedSet<GeoServerRole> roles = manager.loadRoleService(ROLE_SERVICE).getRoles();
        return roles.stream().map(GeoServerRole::getAuthority).toList();
    }

    private <E extends GeoServerEvent> E remote(E event) {
        event.setRemote(true);
        event.setOrigin("another-instance");
        return event;
    }

    @SneakyThrows(Exception.class)
    private static ApplicationContextRunner createContextRunner(File dataDirectory) {
        Catalog rawCatalog = new CatalogImpl();
        SecureCatalogImpl secureCatalog = new SecureCatalogImpl(rawCatalog, new TestResourceAccessManager());
        GeoServer geoserver = mock(GeoServer.class);
        FileSystemResourceStore resourceStore = new FileSystemResourceStore(dataDirectory);
        disableFilePolling(resourceStore);
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(resourceLoader);
        UpdateSequence updateSequence = mock(UpdateSequence.class);

        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GeoServerMainSecurityAutoConfiguration.class))
                .withBean("extensions", GeoServerExtensions.class)
                .withBean(GeoServerConfigurationLock.class)
                .withBean(ResourceStore.class, () -> resourceStore)
                .withBean(GeoServerResourceLoader.class, () -> resourceLoader)
                .withBean("dataDirectory", GeoServerDataDirectory.class, () -> datadir)
                .withBean("rawCatalog", Catalog.class, () -> rawCatalog)
                .withBean("catalog", Catalog.class, () -> secureCatalog)
                .withBean("secureCatalog", Catalog.class, () -> secureCatalog)
                .withBean("geoServer", GeoServer.class, () -> geoserver)
                .withBean("updateSequence", UpdateSequence.class, () -> updateSequence)
                .withBean(PublishedEvents.class)
                .withPropertyValues("logging.level.org.geoserver.platform: off");
    }

    /**
     * The XML services poll their files for external changes. A long polling delay keeps that out of the way: the
     * refresh under test is the one triggered by the events.
     */
    private static void disableFilePolling(FileSystemResourceStore resourceStore) {
        FileSystemWatcher watcher = (FileSystemWatcher) resourceStore.getResourceNotificationDispatcher();
        watcher.schedule(1, TimeUnit.HOURS);
    }

    /** Collects the events published by the security manager into the application context */
    static class PublishedEvents {

        private final List<GeoServerEvent> events = new CopyOnWriteArrayList<>();

        @EventListener(GeoServerEvent.class)
        void onEvent(GeoServerEvent event) {
            events.add(event);
        }

        List<GeoServerEvent> all() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }

        <E extends GeoServerEvent> E expectOne(Class<E> type) {
            List<E> matches =
                    events.stream().filter(type::isInstance).map(type::cast).toList();
            assertThat(matches)
                    .as("expected one %s, got %s", type.getSimpleName(), events)
                    .hasSize(1);
            return matches.getFirst();
        }
    }
}
