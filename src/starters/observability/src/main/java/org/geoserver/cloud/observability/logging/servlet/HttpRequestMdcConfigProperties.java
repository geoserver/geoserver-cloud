/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.observability.logging.servlet;

import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "logging.mdc.include.http")
public class HttpRequestMdcConfigProperties {

    private boolean id = true;

    /**
     * The Internet Protocol (IP) address of the client or last proxy that sent the request. For
     * HTTP servlets, same as the value of the CGI variable REMOTE_ADDR.
     */
    private boolean remoteAddr = true;

    /**
     * The fully qualified name of the client or the last proxy that sent the request. If the engine
     * cannot or chooses not to resolve the hostname (to improve performance), this method returns
     * the dotted-string form of the IP address. For HTTP servlets, same as the value of the CGI
     * variable REMOTE_HOST. Defaults to false to avoid the possible overhead in reverse DNS
     * lookups. remoteAddress should be enough in most cases.
     */
    private boolean remoteHost = true;

    private boolean method = true;
    private boolean url = true;
    private boolean parameters = true;
    private boolean queryString = true;
    private boolean sessionId = true;

    private boolean cookies = true;
    private boolean headers = true;
    private Pattern headersPattern = Pattern.compile(".*");
}
