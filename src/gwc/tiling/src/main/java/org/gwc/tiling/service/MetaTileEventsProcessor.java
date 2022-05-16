/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @since 1.0
 */
public class MetaTileEventsProcessor {

    private MetaTileLockManager locks;
    private WorkQueue queue;

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    public void process(MetaTileRequest request) {
        queue.add(request);
    }

    static class WorkQueue {

        /**
         * @param request
         */
        public void add(MetaTileRequest request) {
            // TODO Auto-generated method stub
        }
    }
}
