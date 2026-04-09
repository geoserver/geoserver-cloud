/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.components;

import org.springframework.web.bind.annotation.ControllerAdvice;

/** @ControllerAdvice-annotated class for testing GENERATE mode picks up non-stereotype @Component meta-annotations. */
@ControllerAdvice
public class ControllerAdviceComponent {}
