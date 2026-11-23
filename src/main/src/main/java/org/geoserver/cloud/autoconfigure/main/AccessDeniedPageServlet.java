/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;

/**
 * Writes the access denied page from the classpath, whatever the method and path of the request.
 *
 * <p>The status of the response stays as it is: the 403 set by the exception translation filter before the forward, or
 * the default 200 for a request sent to the page itself.
 *
 * <p>The page links to the web UI with a relative URL. The browser resolves it against the URL of the denied request,
 * kept by the forward. A base element pointing at the root of the service makes the link work from any path.
 */
class AccessDeniedPageServlet extends HttpServlet {

    private static final String PAGE_RESOURCE =
            "META-INF/resources" + AccessDeniedPageAutoConfiguration.ACCESS_DENIED_PAGE;

    private static final String HEAD = "<head>";

    private final String page = readPage();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] body = pageFor(request).getBytes(StandardCharsets.ISO_8859_1);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    private String pageFor(HttpServletRequest request) {
        int levels = levelsBelowServiceRoot(request);
        if (levels == 0) {
            return page;
        }
        String base = "<base href=\"" + "../".repeat(levels) + "\" />";
        return page.replace(HEAD, HEAD + "\n" + base);
    }

    /**
     * How many directories the URL of the denied request lies below the root of the service, as seen by the browser.
     * Zero for a request sent to the page itself.
     */
    private static int levelsBelowServiceRoot(HttpServletRequest request) {
        String deniedUri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        String contextPath = (String) request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH);
        if (deniedUri == null || contextPath == null || !deniedUri.startsWith(contextPath)) {
            return 0;
        }
        String pathInService = deniedUri.substring(contextPath.length());
        long slashes = pathInService.chars().filter(c -> c == '/').count();
        return (int) Math.max(0, slashes - 1);
    }

    private static String readPage() {
        try (InputStream in = new ClassPathResource(PAGE_RESOURCE).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
