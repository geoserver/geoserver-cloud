/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.SimpleThreadBlocker;
import org.junit.jupiter.api.Test;

class PropertiesControlFlowConfiguratorTest {

    @Test
    void priorityRulesUseMonitoredBlocker() throws Exception {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");
        rules.setProperty("ows.priority.http", "gs-priority,3");

        GlobalFlowController global = buildGlobalController(rules);

        assertThat(global.getBlocker()).isInstanceOf(MonitoredPriorityThreadBlocker.class);
    }

    @Test
    void nonPriorityRulesKeepSimpleBlocker() throws Exception {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");

        GlobalFlowController global = buildGlobalController(rules);

        assertThat(global.getBlocker()).isInstanceOf(SimpleThreadBlocker.class);
    }

    private GlobalFlowController buildGlobalController(Properties rules) throws Exception {
        List<FlowController> controllers = new PropertiesControlFlowConfigurator(rules).buildFlowControllers();
        return controllers.stream()
                .filter(GlobalFlowController.class::isInstance)
                .map(GlobalFlowController.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
