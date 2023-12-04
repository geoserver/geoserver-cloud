/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.extension.importer;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(name = "org.geoserver.importer.web.ImporterConfigPage")
@ConditionalOnProperty( // enabled by default
        prefix = ImporterAutoConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import(ImporterConfiguration.class)
public class ImporterAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.web-ui.extensions.importer";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
