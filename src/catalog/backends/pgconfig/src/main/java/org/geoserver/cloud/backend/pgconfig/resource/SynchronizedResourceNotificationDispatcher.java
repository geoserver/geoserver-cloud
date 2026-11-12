/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;

/**
 * Dispatches resource notifications while holding the lock of the dispatcher. Request threads announce their writes
 * while other threads add and remove listeners. The parent class takes the lock to add and remove listeners, and this
 * class takes the same lock to notify them.
 */
public class SynchronizedResourceNotificationDispatcher extends SimpleResourceNotificationDispatcher {

    @Override
    public synchronized void changed(ResourceNotification notification) {
        super.changed(notification);
    }
}
