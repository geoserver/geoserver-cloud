/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.event;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

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

    @Accessors(chain = true)
    public static @Data class UpdateSeq {
        private long observed;
        private long real;
    }

    @GetMapping(path = "/admin/updatesequence", produces = "application/json")
    public UpdateSeq currVal() {
        long observed = observed();
        long real = updateSequence.currValue();
        return new UpdateSeq().setObserved(observed).setReal(real);
    }

    @RolesAllowed({"ADMIN", "ROLE_ADMINISTRATOR"})
    @PostMapping(path = "/admin/updatesequence", produces = "application/json")
    public UpdateSeq nextVal() {
        final long nextValue = updateSequence.nextValue();
        eventPublisher.publishEvent(UpdateSequenceEvent.createLocal(nextValue));
        return new UpdateSeq().setObserved(observed()).setReal(nextValue);
    }

    protected long observed() {
        return geoServer.getGlobal().getUpdateSequence();
    }
}
