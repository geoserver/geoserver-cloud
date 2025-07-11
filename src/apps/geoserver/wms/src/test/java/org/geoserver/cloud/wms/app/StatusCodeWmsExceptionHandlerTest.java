/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wms.app;

import static org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE;
import static org.geoserver.platform.ServiceException.MAX_MEMORY_EXCEEDED;
import static org.geoserver.platform.ServiceException.MISSING_PARAMETER_VALUE;
import static org.geoserver.platform.ServiceException.SERVICE_UNAVAILABLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpStatus;

public class StatusCodeWmsExceptionHandlerTest {
    private StatusCodeWmsExceptionHandler handler;
    private PropertyResolver propertyResolver = mock(PropertyResolver.class);
    private GeoServer geoServer = mock(GeoServer.class);

    @BeforeEach
    void setUp() {
        when(propertyResolver.getProperty("geoserver.wms.exceptions.w3cstatus", Boolean.class, Boolean.FALSE))
                .thenReturn(true);
        handler = new StatusCodeWmsExceptionHandler(List.of(), geoServer, propertyResolver);
    }

    @ParameterizedTest
    @MethodSource("exceptionCodeToHttpStatus")
    void setStatusCodeWithExceptionCodeTest(String exceptionCode, HttpStatus expectedStatus) {
        ServiceException serviceException = new ServiceException("Some message", exceptionCode);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHttpResponse()).thenReturn(response);

        handler.setStatusCode(serviceException, request);

        verify(response).setStatus(expectedStatus.value());
    }

    private static Stream<Arguments> exceptionCodeToHttpStatus() {
        return Stream.of(
                Arguments.of(MISSING_PARAMETER_VALUE, HttpStatus.BAD_REQUEST),
                Arguments.of(INVALID_PARAMETER_VALUE, HttpStatus.BAD_REQUEST),
                Arguments.of("InvalidCRS", HttpStatus.BAD_REQUEST),
                Arguments.of(SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
                Arguments.of(MAX_MEMORY_EXCEEDED, HttpStatus.SERVICE_UNAVAILABLE),
                Arguments.of("SomeUnknownCode", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("exceptionMessageToHttpStatus")
    void setStatusCodeWithExceptionMessageTest(String message, HttpStatus expectedStatus) {
        ServiceException exception = new ServiceException(message);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHttpResponse()).thenReturn(response);

        handler.setStatusCode(exception, request);

        verify(response).setStatus(expectedStatus.value());
    }

    private static Stream<Arguments> exceptionMessageToHttpStatus() {
        return Stream.of(
                Arguments.of("This request used more time than allowed", HttpStatus.SERVICE_UNAVAILABLE),
                Arguments.of("Content has been requested in one of the following languages:", HttpStatus.BAD_REQUEST),
                Arguments.of("Unknown message", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
