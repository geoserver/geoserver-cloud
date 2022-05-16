/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import org.gwc.tiling.model.MetaTileIdentifier;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

/**
 * @since 1.0
 */
public interface MetaTileLockManager {

    Optional<Lock> tryLock(MetaTileIdentifier metaTileId);
}
