/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.context;

import lombok.Value;
import org.w3c.dom.Element;

/**
 * Information about a component-scan configuration from XML. Based on the ComponentScanInfo from the old
 * spring-factory-processor.
 *
 * @since 3.0.0
 */
@Value
public class ComponentScanInfo {

    /** Comma/semicolon/whitespace-separated list of packages to scan, from the {@code base-package} XML attribute. */
    private final String basePackage;

    /** Optional custom scope-resolver class name, from the {@code scope-resolver} XML attribute. Empty if not set. */
    private final String scopeResolver;

    /**
     * Whether the default stereotype filters ({@code @Component}, {@code @Service}, etc.) are active. Defaults to
     * {@code true}; only {@code false} when the XML explicitly sets {@code use-default-filters="false"}.
     */
    private final boolean useDefaultFilters;

    /**
     * Optional classpath resource pattern restricting which classes are considered, from the {@code resource-pattern}
     * XML attribute (e.g. {@code "**&#47;*.class"}). Empty if not set.
     */
    private final String resourcePattern;

    /** The original DOM element for the {@code <context:component-scan>} declaration. */
    private final Element originalElement;

    public ComponentScanInfo(Element element) {
        this.originalElement = element;
        this.basePackage = element.getAttribute("base-package");
        this.scopeResolver = element.getAttribute("scope-resolver");
        this.useDefaultFilters = !"false".equals(element.getAttribute("use-default-filters"));
        this.resourcePattern = element.getAttribute("resource-pattern");
    }

    /** Split base-package attribute into individual packages. */
    public String[] getBasePackages() {
        if (basePackage == null || basePackage.trim().isEmpty()) {
            return new String[0];
        }
        return basePackage.split("[,;\\s]+");
    }
}
