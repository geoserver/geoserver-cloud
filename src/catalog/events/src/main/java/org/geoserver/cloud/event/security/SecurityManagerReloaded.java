/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.security;

import java.io.Serial;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired after a {@link GeoServerSecurityManager} has successfully completed its reload
 * process.
 *
 * <p>This event indicates that the security manager has fully initialized or reloaded its
 * configuration, and it's safe to make further security configuration changes.
 *
 * @since 1.9
 */
@ToString(callSuper = true)
public class SecurityManagerReloaded extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The update sequence at the time this event was created. */
    private final @Getter long updateSequence;

    /**
     * Creates a new {@code SecurityManagerReloaded} event.
     *
     * @param source the security manager that fired this event
     * @param updateSequence the update sequence at the time this event was created
     */
    public SecurityManagerReloaded(@NonNull Object source, long updateSequence) {
        super(source);
        this.updateSequence = updateSequence;
    }
}
