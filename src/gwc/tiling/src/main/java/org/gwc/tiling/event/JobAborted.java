/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.event;

import lombok.NonNull;
import lombok.Value;

import org.gwc.tiling.model.CacheJobInfo;

/**
 * @since 1.0
 */
public @Value class JobAborted {

    private @NonNull CacheJobInfo job;
}
