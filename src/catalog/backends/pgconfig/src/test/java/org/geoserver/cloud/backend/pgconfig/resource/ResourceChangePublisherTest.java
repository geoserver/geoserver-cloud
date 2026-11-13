/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Change;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ResourceChangePublisherTest {

    @Mock
    private ResourceNotificationDispatcher dispatcher;

    @Mock
    private Consumer<GeoServerEvent> eventPublisher;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Runnable localChange;

    private ResourceChangePublisher publisher;

    private final ResourceChanged change =
            ResourceChanged.createLocal("styles/a.sld", Kind.MODIFIED, Type.RESOURCE, 1000L, List.of());

    @BeforeEach
    void setUp() {
        publisher = new ResourceChangePublisher(dispatcher, eventPublisher);
    }

    @Test
    void afterCommitOutsideTransactionRunsNow() {
        publisher.afterCommit(change, localChange);

        InOrder order = inOrder(localChange, dispatcher, eventPublisher);
        order.verify(localChange).run();
        order.verify(dispatcher).changed(any(ResourceNotification.class));
        order.verify(eventPublisher).accept(change);
    }

    @Test
    void afterCommitInsideTransactionWaits() throws SQLException {
        TransactionTemplate transaction = transactionOverMockedConnection();

        transaction.executeWithoutResult(status -> {
            publisher.afterCommit(change, localChange);
            verifyNoInteractions(localChange, dispatcher, eventPublisher);
        });

        verify(localChange).run();
        verify(dispatcher).changed(any(ResourceNotification.class));
        verify(eventPublisher).accept(change);
    }

    @Test
    void rollbackPublishesNothing() throws SQLException {
        TransactionTemplate transaction = transactionOverMockedConnection();

        transaction.executeWithoutResult(status -> {
            publisher.afterCommit(change, localChange);
            status.setRollbackOnly();
        });

        verifyNoInteractions(localChange, dispatcher, eventPublisher);
    }

    @Test
    void failingListenerDoesNotThrow() {
        doThrow(new IllegalStateException("listener failed")).when(dispatcher).changed(any(ResourceNotification.class));

        try (LogCapture log = LogCapture.of(ResourceChangePublisher.class.getName())) {
            publisher.afterCommit(change);
            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("styles/a.sld");
        }

        verify(eventPublisher).accept(change);
    }

    @Test
    void failingPublisherDoesNotThrow() {
        doThrow(new IllegalStateException("publication failed"))
                .when(eventPublisher)
                .accept(change);

        try (LogCapture log = LogCapture.of(ResourceChangePublisher.class.getName())) {
            publisher.afterCommit(change);
            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("styles/a.sld");
        }

        InOrder order = inOrder(dispatcher, eventPublisher);
        order.verify(dispatcher).changed(any(ResourceNotification.class));
        order.verify(eventPublisher).accept(change);
    }

    @Test
    void laterChangesStillAnnounced() throws SQLException {
        ResourceChanged second =
                ResourceChanged.createLocal("styles/b.sld", Kind.CREATED, Type.RESOURCE, 2000L, List.of());
        doThrow(new IllegalStateException("listener failed"))
                .doNothing()
                .when(dispatcher)
                .changed(any(ResourceNotification.class));
        TransactionTemplate transaction = transactionOverMockedConnection();

        try (LogCapture log = LogCapture.of(ResourceChangePublisher.class.getName())) {
            transaction.executeWithoutResult(status -> {
                publisher.afterCommit(change);
                publisher.afterCommit(second);
            });
        }

        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).accept(change);
        order.verify(eventPublisher).accept(second);
    }

    @Test
    void toNotificationMapsKinds() {
        assertThat(notificationOf(Kind.CREATED).getKind()).isEqualTo(ResourceNotification.Kind.ENTRY_CREATE);
        assertThat(notificationOf(Kind.MODIFIED).getKind()).isEqualTo(ResourceNotification.Kind.ENTRY_MODIFY);
        assertThat(notificationOf(Kind.DELETED).getKind()).isEqualTo(ResourceNotification.Kind.ENTRY_DELETE);

        ResourceNotification deleted = notificationOf(Kind.DELETED);
        assertThat(deleted.getPath()).isEqualTo("d");
        assertThat(deleted.getTimestamp()).isEqualTo(1000L);
        // the main path leads the delta, as in the notifications of the upstream file system store
        assertThat(deleted.events())
                .extracting(Event::getPath, Event::getKind)
                .containsExactly(
                        tuple("d", ResourceNotification.Kind.ENTRY_DELETE),
                        tuple("d/e", ResourceNotification.Kind.ENTRY_DELETE));
    }

    private static ResourceNotification notificationOf(Kind kind) {
        List<Change> changes = List.of(new Change("d/e", kind));
        ResourceChanged event = ResourceChanged.createLocal("d", kind, Type.DIRECTORY, 1000L, changes);
        return ResourceChangePublisher.toNotification(event);
    }

    private TransactionTemplate transactionOverMockedConnection() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }
}
