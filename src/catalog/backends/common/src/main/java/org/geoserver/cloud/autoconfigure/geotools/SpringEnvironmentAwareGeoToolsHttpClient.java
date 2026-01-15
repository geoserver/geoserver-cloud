/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.geotools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.geoserver.cloud.autoconfigure.geotools.GeoToolsHttpClientProxyConfigurationProperties.ProxyHostConfig;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPConnectionPooling;
import org.geotools.http.HTTPProxy;
import org.geotools.http.HTTPResponse;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.springframework.core.env.PropertyResolver;

/**
 * Copy of GeoTools' {@link org.geotools.http.commons.MultithreadedHttpClient} due to its lack of
 * extensibility, adding the ability to set up the Apache HTTPClient's proxy configuration from
 * Spring {@link PropertyResolver}'s properties.
 */
@Slf4j
class SpringEnvironmentAwareGeoToolsHttpClient extends org.geotools.http.AbstractHttpClient
        implements HTTPClient, HTTPConnectionPooling, HTTPProxy {

    private static final Logger LOGGER = Logging.getLogger(SpringEnvironmentAwareGeoToolsHttpClient.class);

    private final GeoToolsHttpClientProxyConfigurationProperties proxyConfig;

    private final PoolingHttpClientConnectionManager connectionManager;

    private HttpClient client;

    private ConnectionConfig connectionConfig;

    private RequestConfig requestConfig;

    private AuthScope authScope;

    public SpringEnvironmentAwareGeoToolsHttpClient(
            @NonNull GeoToolsHttpClientProxyConfigurationProperties proxyConfig) {
        this.proxyConfig = proxyConfig;
        connectionConfig = ConnectionConfig.custom()
                .setSocketTimeout(Timeout.ofSeconds(30))
                .setConnectTimeout(Timeout.ofSeconds(30))
                .build();
        connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();
        connectionManager.setMaxTotal(6);
        connectionManager.setDefaultMaxPerRoute(6);
        requestConfig = RequestConfig.custom()
                .setCookieSpec(StandardCookieSpec.RELAXED)
                .setExpectContinueEnabled(true)
                .build();

        resetCredentials();
    }

    private BasicCredentialsProvider credsProvider = null;

    private HttpClientBuilder builder() {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setUserAgent("GeoTools/%s (%s)"
                        .formatted(GeoTools.getVersion(), this.getClass().getSimpleName()))
                .useSystemProperties()
                .setConnectionManager(connectionManager);
        if (credsProvider != null) {
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        return builder;
    }

    @Override
    public HttpMethodResponse post(final URL url, final InputStream postContent, final String postContentType)
            throws IOException {
        return post(url, postContent, postContentType, null);
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public HttpMethodResponse post(
            URL url, InputStream postContent, String postContentType, Map<String, String> headers) throws IOException {

        if (headers == null) {
            headers = new HashMap<>();
        } else {
            headers = new HashMap<>(headers); // avoid parameter modification
        }
        HttpPost postMethod = new HttpPost(url.toExternalForm());
        postMethod.setConfig(requestConfig(url));
        HttpEntity requestEntity;
        if (credsProvider != null) {
            // we can't read the input stream twice as would be needed if the server asks us to
            // authenticate
            String input = new BufferedReader(new InputStreamReader(postContent, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            requestEntity = new StringEntity(input);
        } else {
            requestEntity = new InputStreamEntity(postContent, ContentType.create(postContentType));
        }
        if (tryGzip) {
            headers.put("Accept-Encoding", "gzip");
        }
        if (postContentType != null) {
            headers.put("Content-type", postContentType);
        }

        setHeadersOn(headers, postMethod);

        postMethod.setEntity(requestEntity);

        HttpMethodResponse response = null;
        try {
            response = executeMethod(postMethod);
        } catch (HttpException | URISyntaxException e) {
            throw new IOException(e);
        }
        if (200 != response.getStatusCode()) {
            throw new IOException(
                    "Server returned HTTP error code " + response.getStatusCode() + " for URL " + url.toExternalForm());
        }

        return response;
    }

    /** @return the http status code of the execution */
    private HttpMethodResponse executeMethod(org.apache.hc.client5.http.classic.methods.HttpUriRequestBase method)
            throws IOException, HttpException, URISyntaxException {

        HttpClientContext localContext = HttpClientContext.create();
        ClassicHttpResponse resp;
        AuthScope scope = authScope;
        if (credsProvider != null && scope != null) {
            localContext.setCredentialsProvider(credsProvider);
            // see https://stackoverflow.com/a/21592593
            AuthCache authCache = new BasicAuthCache();
            URI target = method.getUri();
            BasicScheme basicScheme = new BasicScheme();
            Credentials credentials = credsProvider.getCredentials(scope, localContext);
            basicScheme.initPreemptive(credentials);
            authCache.put(new HttpHost(target.getScheme(), target.getHost(), target.getPort()), basicScheme);
            localContext.setAuthCache(authCache);
        }
        resp = client.executeOpen(RoutingSupport.determineHost(method), method, localContext);

        return new HttpMethodResponse(resp);
    }

    @Override
    public HTTPResponse get(final URL url) throws IOException {
        return this.get(url, null);
    }

    @Override
    public HTTPResponse get(URL url, Map<String, String> headers) throws IOException {

        if (isFile(url)) {
            return createFileResponse(url);
        }
        if (headers == null) {
            headers = new HashMap<>();
        } else {
            headers = new HashMap<>(headers); // avoid parameter modification
        }

        Map<String, String> extraParams = getExtraParams();
        if (!extraParams.isEmpty()) {
            url = appendURL(url, extraParams);
        }

        HttpGet getMethod = new HttpGet(url.toExternalForm());
        getMethod.setConfig(requestConfig(url));

        if (tryGzip) {
            headers.put("Accept-Encoding", "gzip");
        }

        setHeadersOn(headers, getMethod);

        HttpMethodResponse response = null;
        try {
            response = executeMethod(getMethod);
        } catch (HttpException e) {
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (200 != response.getStatusCode()) {
            throw new IOException(
                    "Server returned HTTP error code " + response.getStatusCode() + " for URL " + url.toExternalForm());
        }
        return response;
    }

    private void setHeadersOn(
            Map<String, String> headers, org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Setting header " + header.getKey() + " = " + header.getValue());
            }
            request.setHeader(header.getKey(), header.getValue());
        }
    }

    @Override
    public void setUser(String user) {
        super.setUser(user);
        resetCredentials();
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(password);
        resetCredentials();
    }

    private void resetCredentials() {
        // overrides defaulting to SystemDefaultCredentialsProvider
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        if (user != null && password != null) {
            setTargetCredentials(provider, user, password);
        }
        setProxyCredentials(provider, this.proxyConfig.getHttp());
        setProxyCredentials(provider, this.proxyConfig.getHttps());
        this.credsProvider = provider;
        client = builder().build();
    }

    private void setTargetCredentials(BasicCredentialsProvider provider, String userName, String pwd) {
        authScope = new AuthScope(null, -1);
        Credentials credentials = new UsernamePasswordCredentials(userName, pwd.toCharArray());
        provider.setCredentials(authScope, credentials);
    }

    private void setProxyCredentials(BasicCredentialsProvider provider, ProxyHostConfig proxy) {
        if (proxy.isSecured()) {
            AuthScope scope = new AuthScope(toHttpHost(proxy), null, null);
            String proxyUser = proxy.getUser();
            String proxyPassword = proxy.getPassword();
            Credentials credentials = new UsernamePasswordCredentials(proxyUser, proxyPassword.toCharArray());
            provider.setCredentials(scope, credentials);
        }
    }

    private Optional<HttpHost> proxy(URL url) {
        final GeoToolsHttpClientProxyConfigurationProperties config = this.proxyConfig;
        final String host = url.getHost();
        return config.ofProtocol(url.getProtocol()).forHost(host).map(this::toHttpHost);
    }

    private HttpHost toHttpHost(ProxyHostConfig conf) {
        String host = conf.getHost();
        int port = conf.port();
        return new HttpHost(host, port);
    }

    private RequestConfig requestConfig(URL url) {
        RequestConfig reqConf = this.requestConfig;
        Optional<HttpHost> proxy = proxy(url);
        if (proxy.isPresent()) {
            HttpHost proxyHost = proxy.get();
            reqConf = RequestConfig.copy(reqConf).setProxy(proxyHost).build();
        }
        return reqConf;
    }

    @Override
    public int getConnectTimeout() {
        return (int) connectionConfig.getConnectTimeout().toSeconds();
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        connectionConfig = ConnectionConfig.copy(connectionConfig)
                .setConnectTimeout(Timeout.ofSeconds(connectTimeout))
                .build();
    }

    @Override
    public int getReadTimeout() {
        return (int) requestConfig.getResponseTimeout().toSeconds();
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        requestConfig = RequestConfig.copy(requestConfig)
                .setResponseTimeout(Timeout.ofSeconds(readTimeout))
                .build();
    }

    @Override
    public int getMaxConnections() {
        return connectionManager.getDefaultMaxPerRoute();
    }

    @Override
    public void setMaxConnections(final int maxConnections) {
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxTotal(maxConnections);
    }

    @Override
    public void close() {
        this.connectionManager.close();
    }

    static class HttpMethodResponse implements HTTPResponse {

        private ClassicHttpResponse methodResponse;

        private InputStream responseBodyAsStream;

        public HttpMethodResponse(final ClassicHttpResponse methodResponse) {
            this.methodResponse = methodResponse;
        }

        /** @return */
        public int getStatusCode() {
            if (methodResponse != null) {
                return methodResponse.getCode();
            } else {
                return -1;
            }
        }

        @Override
        public void dispose() {
            if (responseBodyAsStream != null) {
                try {
                    responseBodyAsStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            if (methodResponse != null) {
                try {
                    methodResponse.close();
                } catch (IOException e) {
                    // ignore
                }
                methodResponse = null;
            }
        }

        @Override
        public String getContentType() {
            return getResponseHeader("Content-Type");
        }

        @Override
        public String getResponseHeader(final String headerName) {
            Header responseHeader = methodResponse.getFirstHeader(headerName);
            return responseHeader == null ? null : responseHeader.getValue();
        }

        @Override
        public InputStream getResponseStream() throws IOException {
            if (responseBodyAsStream == null) {
                responseBodyAsStream = methodResponse.getEntity().getContent();
                // commons httpclient does not handle gzip encoding automatically, we have to check
                // ourselves: https://issues.apache.org/jira/browse/HTTPCLIENT-816
                Header header = methodResponse.getFirstHeader("Content-Encoding");
                if (header != null && "gzip".equals(header.getValue())) {
                    responseBodyAsStream = new GZIPInputStream(responseBodyAsStream);
                }
            }
            return responseBodyAsStream;
        }

        /** @see org.geotools.data.ows.HTTPResponse#getResponseCharset() */
        @Override
        public String getResponseCharset() {
            final Header encoding = new BasicHeader(
                    HttpHeaders.CONTENT_ENCODING, methodResponse.getEntity().getContentEncoding());
            return encoding.getValue();
        }
    }
}
