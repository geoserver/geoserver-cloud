/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ControlFlowAppContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String cores = System.getProperty("cpu.cores");
        if (null == cores) {
            cores = "" + Runtime.getRuntime().availableProcessors();
            System.setProperty("cpu.cores", cores);
        }
    }
}
