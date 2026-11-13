/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Change;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Notifies this pod's listeners and publishes the event to the others once a change is committed.
 *
 * <p>A listener notified before the commit could read the previous state from another connection. A rolled back change
 * is never announced. A failure of a listener or of the publication is logged and does not fail the write.
 */
@Slf4j
public class ResourceChangePublisher {

    private static final Runnable NO_LOCAL_CHANGE = () -> {};

    private final ResourceNotificationDispatcher dispatcher;
    private final Consumer<GeoServerEvent> eventPublisher;

    public ResourceChangePublisher(
            @NonNull ResourceNotificationDispatcher dispatcher, @NonNull Consumer<GeoServerEvent> eventPublisher) {
        this.dispatcher = dispatcher;
        this.eventPublisher = eventPublisher;
    }

    /** Runs after the current transaction commits, or at once when no transaction is active. */
    public void afterCommit(@NonNull ResourceChanged change) {
        afterCommit(change, NO_LOCAL_CHANGE);
    }

    /**
     * Like {@link #afterCommit(ResourceChanged)}, applying a change to the local copies of the resources first. The
     * listeners notified next find the local copies up to date.
     */
    public void afterCommit(@NonNull ResourceChanged change, @NonNull Runnable localChange) {
        Runnable announcement = () -> announce(change, localChange);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(runAfterCommit(announcement));
        } else {
            announcement.run();
        }
    }

    public ResourceNotificationDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * Tells the listeners of this pod about a change found in the database, without publishing it: the other pods find
     * it the same way. A failure of a listener is logged.
     */
    public void notifyLocally(@NonNull ResourceChanged change) {
        runGuarded("notify the listeners", change, () -> dispatcher.changed(toNotification(change)));
    }

    private void announce(ResourceChanged change, Runnable localChange) {
        runGuarded("update the local copy", change, localChange);
        runGuarded("notify the listeners", change, () -> dispatcher.changed(toNotification(change)));
        runGuarded("publish the event", change, () -> eventPublisher.accept(change));
    }

    // The transaction committed: a failed step must not fail the write, nor skip the steps and the changes after it
    private static void runGuarded(String step, ResourceChanged change, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Could not {} of a change to {}", step, change.getPath(), e);
        }
    }

    private static TransactionSynchronization runAfterCommit(Runnable announcement) {
        return new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                announcement.run();
            }
        };
    }

    /**
     * The notification of a change, for the listeners of this pod. Its events start with the main path, followed by the
     * other paths of the change, like the notifications of the upstream file system resource store.
     */
    static ResourceNotification toNotification(ResourceChanged change) {
        List<Event> events = new ArrayList<>();
        events.add(new Event(change.getPath(), notificationKind(change.getKind())));
        for (Change other : change.getChanges()) {
            events.add(new Event(other.getPath(), notificationKind(other.getKind())));
        }
        Kind kind = notificationKind(change.getKind());
        return new ResourceNotification(change.getPath(), kind, change.getMtime(), events);
    }

    private static Kind notificationKind(ResourceChanged.Kind kind) {
        return switch (kind) {
            case CREATED -> Kind.ENTRY_CREATE;
            case MODIFIED -> Kind.ENTRY_MODIFY;
            case DELETED -> Kind.ENTRY_DELETE;
        };
    }
}
