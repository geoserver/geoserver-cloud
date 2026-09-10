/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.server.mvc.filter.FormFilter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** @since 3.1.0 */
class FormPostCharacterEncodingFilterTest {

    private final FormPostCharacterEncodingFilter filter = new FormPostCharacterEncodingFilter();

    @Test
    void runsBeforeFormFilter() {
        FormFilter formFilter = new FormFilter();

        assertThat(filter.getOrder()).isLessThan(formFilter.getOrder());
    }

    @Test
    void formPostWithoutCharset_declaresUtf8() throws ServletException, IOException {
        MockHttpServletRequest request = request("POST", "application/x-www-form-urlencoded");
        assertThat(request.getCharacterEncoding()).isNull();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
    }

    @Test
    void formPostWithCharset_keepsDeclaredCharset() throws ServletException, IOException {
        MockHttpServletRequest request = request("POST", "application/x-www-form-urlencoded; charset=ISO-8859-1");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getCharacterEncoding()).isEqualTo("ISO-8859-1");
    }

    @Test
    void nonFormPost_leavesEncodingUndeclared() throws ServletException, IOException {
        MockHttpServletRequest request = request("POST", "application/zip");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getCharacterEncoding()).isNull();
    }

    @Test
    void formContentTypeOnGet_leavesEncodingUndeclared() throws ServletException, IOException {
        MockHttpServletRequest request = request("GET", "application/x-www-form-urlencoded");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getCharacterEncoding()).isNull();
    }

    private static MockHttpServletRequest request(String method, String contentType) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/web/wicket/page");
        request.setContentType(contentType);
        return request;
    }
}
