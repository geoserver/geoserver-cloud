/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

/**
 * Thrown when generated code could not instantiate a bean without reflection, because its class or its constructor is
 * not public. The message tells an upstream contributor what to change.
 *
 * @see BeanAccessibility
 */
public class InaccessibleBeanException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    InaccessibleBeanException(String message) {
        super(message);
    }
}
