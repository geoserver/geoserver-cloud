/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;

/**
 * Secure headers configuration properties mimicking the WebFlux gateway's {@code SecureHeadersProperties}.
 *
 * <p>SCG Server MVC does not include the WebFlux {@code filter.secure-headers} support. This class bridges the gap by
 * binding the same YAML structure under {@code spring.cloud.gateway.server.webmvc.filter.secure-headers} and providing
 * it to a servlet filter registered in {@link GatewayApplicationAutoconfiguration}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * spring.cloud.gateway.server.webmvc:
 *   filter:
 *     secure-headers:
 *       enabled: true
 *       disable:
 *         - content-security-policy
 *       frame-options: SAMEORIGIN
 *       xss-protection-header: "0"
 *       referrer-policy: no-referrer
 * }</pre>
 *
 * @see "org.springframework.cloud.gateway.filter.factory.SecureHeadersProperties"
 * @since 3.0.0
 */
@ConfigurationProperties(GatewayMvcProperties.PREFIX + ".filter.secure-headers")
public class SecureHeadersProperties {

    public static final String X_XSS_PROTECTION_HEADER = "X-Xss-Protection";
    public static final String X_XSS_PROTECTION_HEADER_DEFAULT = "1 ; mode=block";

    public static final String STRICT_TRANSPORT_SECURITY_HEADER = "Strict-Transport-Security";
    public static final String STRICT_TRANSPORT_SECURITY_HEADER_DEFAULT = "max-age=631138519";

    public static final String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";
    public static final String X_FRAME_OPTIONS_HEADER_DEFAULT = "DENY";

    public static final String X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options";
    public static final String X_CONTENT_TYPE_OPTIONS_HEADER_DEFAULT = "nosniff";

    public static final String REFERRER_POLICY_HEADER = "Referrer-Policy";
    public static final String REFERRER_POLICY_HEADER_DEFAULT = "no-referrer";

    public static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";
    public static final String CONTENT_SECURITY_POLICY_HEADER_DEFAULT =
            "default-src 'self' https:; font-src 'self' https: data:; img-src 'self' https: data:; object-src 'none'; script-src https:; style-src 'self' https: 'unsafe-inline'";

    public static final String X_DOWNLOAD_OPTIONS_HEADER = "X-Download-Options";
    public static final String X_DOWNLOAD_OPTIONS_HEADER_DEFAULT = "noopen";

    public static final String X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER = "X-Permitted-Cross-Domain-Policies";
    public static final String X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER_DEFAULT = "none";

    public static final String PERMISSIONS_POLICY_HEADER = "Permissions-Policy";
    public static final String PERMISSIONS_POLICY_HEADER_OPT_IN_DEFAULT = "accelerometer=(), ambient-light-sensor=(), "
            + "autoplay=(), battery=(), camera=(), cross-origin-isolated=(), display-capture=(), document-domain=(), "
            + "encrypted-media=(), execution-while-not-rendered=(), execution-while-out-of-viewport=(), fullscreen=(), "
            + "geolocation=(), gyroscope=(), keyboard-map=(), magnetometer=(), microphone=(), midi=(), "
            + "navigation-override=(), payment=(), picture-in-picture=(), publickey-credentials-get=(), "
            + "screen-wake-lock=(), sync-xhr=(), usb=(), web-share=(), xr-spatial-tracking=()";

    /** All default (opt-out) header names, lowercased for case-insensitive comparison. */
    private static final Set<String> DEFAULT_HEADERS = Stream.of(
                    X_XSS_PROTECTION_HEADER,
                    STRICT_TRANSPORT_SECURITY_HEADER,
                    X_FRAME_OPTIONS_HEADER,
                    X_CONTENT_TYPE_OPTIONS_HEADER,
                    REFERRER_POLICY_HEADER,
                    CONTENT_SECURITY_POLICY_HEADER,
                    X_DOWNLOAD_OPTIONS_HEADER,
                    X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private boolean enabled = true;

    private String xssProtectionHeader = X_XSS_PROTECTION_HEADER_DEFAULT;
    private String strictTransportSecurity = STRICT_TRANSPORT_SECURITY_HEADER_DEFAULT;
    private String frameOptions = X_FRAME_OPTIONS_HEADER_DEFAULT;
    private String contentTypeOptions = X_CONTENT_TYPE_OPTIONS_HEADER_DEFAULT;
    private String referrerPolicy = REFERRER_POLICY_HEADER_DEFAULT;
    private String contentSecurityPolicy = CONTENT_SECURITY_POLICY_HEADER_DEFAULT;
    private String downloadOptions = X_DOWNLOAD_OPTIONS_HEADER_DEFAULT;
    private String permittedCrossDomainPolicies = X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER_DEFAULT;
    private String permissionsPolicy = PERMISSIONS_POLICY_HEADER_OPT_IN_DEFAULT;

    private Set<String> enabledHeaders = new HashSet<>();
    private Set<String> disabledHeaders = new HashSet<>();

    /**
     * Returns the map of header name to value for all headers that should be applied: default headers + enabled headers
     * - disabled headers.
     */
    public Map<String, String> resolveHeaders() {
        Set<String> active = new HashSet<>(DEFAULT_HEADERS);
        active.addAll(enabledHeaders);
        active.removeAll(disabledHeaders);

        Map<String, String> allHeaders = Map.of(
                X_XSS_PROTECTION_HEADER, xssProtectionHeader,
                STRICT_TRANSPORT_SECURITY_HEADER, strictTransportSecurity,
                X_FRAME_OPTIONS_HEADER, frameOptions,
                X_CONTENT_TYPE_OPTIONS_HEADER, contentTypeOptions,
                REFERRER_POLICY_HEADER, referrerPolicy,
                CONTENT_SECURITY_POLICY_HEADER, contentSecurityPolicy,
                X_DOWNLOAD_OPTIONS_HEADER, downloadOptions,
                X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, permittedCrossDomainPolicies,
                PERMISSIONS_POLICY_HEADER, permissionsPolicy);

        return allHeaders.entrySet().stream()
                .filter(e -> active.contains(e.getKey().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getXssProtectionHeader() {
        return xssProtectionHeader;
    }

    public void setXssProtectionHeader(String xssProtectionHeader) {
        this.xssProtectionHeader = xssProtectionHeader;
    }

    public String getStrictTransportSecurity() {
        return strictTransportSecurity;
    }

    public void setStrictTransportSecurity(String strictTransportSecurity) {
        this.strictTransportSecurity = strictTransportSecurity;
    }

    public String getFrameOptions() {
        return frameOptions;
    }

    public void setFrameOptions(String frameOptions) {
        this.frameOptions = frameOptions;
    }

    public String getContentTypeOptions() {
        return contentTypeOptions;
    }

    public void setContentTypeOptions(String contentTypeOptions) {
        this.contentTypeOptions = contentTypeOptions;
    }

    public String getReferrerPolicy() {
        return referrerPolicy;
    }

    public void setReferrerPolicy(String referrerPolicy) {
        this.referrerPolicy = referrerPolicy;
    }

    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public void setContentSecurityPolicy(String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    public String getDownloadOptions() {
        return downloadOptions;
    }

    public void setDownloadOptions(String downloadOptions) {
        this.downloadOptions = downloadOptions;
    }

    public String getPermittedCrossDomainPolicies() {
        return permittedCrossDomainPolicies;
    }

    public void setPermittedCrossDomainPolicies(String permittedCrossDomainPolicies) {
        this.permittedCrossDomainPolicies = permittedCrossDomainPolicies;
    }

    public String getPermissionsPolicy() {
        return permissionsPolicy;
    }

    public void setPermissionsPolicy(String permissionsPolicy) {
        this.permissionsPolicy = permissionsPolicy;
    }

    public void setDisable(List<String> disable) {
        if (disable != null) {
            disabledHeaders =
                    disable.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        }
    }

    public void setEnable(List<String> enable) {
        if (enable != null) {
            enabledHeaders =
                    enable.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        }
    }
}
