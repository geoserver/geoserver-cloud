/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.services;

import java.io.InputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController
@RequestMapping(path = "${gwc.context.suffix:}/rest")
public class SeedControllerOverride extends org.geowebcache.rest.controller.SeedController {

    /**
     *
     * {@inheritDoc}
     * <p>
     * Override for the {@code path} pattern to exclude {@code .xml} and
     * {@code .json} suffixes to avoid an exception like the following with
     * {@link #seedOrTruncateWithJsonPayload} and
     * {@link #seedOrTruncateWithXmlPayload}:
     * <pre>
     * <code>
     * java.lang.IllegalStateException: Ambiguous handler methods mapped for '.../rest/seed/workspace:layer.xml': {
     *   public org.springframework.http.ResponseEntity org.geoserver.cloud.gwc.config.services.SeedController.seedOrTruncateWithXmlPayload(...),
     *   public org.springframework.http.ResponseEntity org.geoserver.cloud.gwc.config.services.SeedController.doPost(...)
     *  }
     * </code>
     * </pre>
     */
    @Override
    @PostMapping("/seed/{layer:^(?!.*\\.(?:xml|json)$).+}")
    public ResponseEntity<?> doPost(
            HttpServletRequest request,
            InputStream inputStream,
            @PathVariable String layer,
            @RequestParam Map<String, String> params) {

        return super.doPost(request, inputStream, layer, params);
    }
}
