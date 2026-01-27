/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.event;

import jakarta.annotation.security.RolesAllowed;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @since 1.0
 */
@RestController
@RequestMapping("${geoserver.base-path:}")
@RequiredArgsConstructor
public class UpdateSequenceController {

    private final @NonNull UpdateSequence updateSequence;
    private final @NonNull ApplicationEventPublisher eventPublisher;
    private final @NonNull GeoServer geoServer;

    public static record UpdateSeq(long observed, long real) {}

    @GetMapping(path = "/admin/updatesequence", produces = "application/json")
    public UpdateSeq currVal() {
        long observed = observed();
        long real = updateSequence.currValue();
        return new UpdateSeq(observed, real);
    }

    @RolesAllowed({"ADMIN", "ROLE_ADMINISTRATOR"})
    @PostMapping(path = "/admin/updatesequence", produces = "application/json")
    public UpdateSeq nextVal() {
        long observed = observed();
        long nextValue = updateSequence.nextValue();
        eventPublisher.publishEvent(UpdateSequenceEvent.createLocal(nextValue));
        return new UpdateSeq(observed, nextValue);
    }

    protected long observed() {
        return geoServer.getGlobal().getUpdateSequence();
    }
}
