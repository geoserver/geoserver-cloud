/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wms;

import static org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE;
import static org.geoserver.platform.ServiceException.MAX_MEMORY_EXCEEDED;
import static org.geoserver.platform.ServiceException.MISSING_PARAMETER_VALUE;
import static org.geoserver.platform.ServiceException.SERVICE_UNAVAILABLE;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSServiceExceptionHandler;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpStatus;

/**
 * WMS exception handler that will check the {@code W3CSTATUS=true} query parameter, or
 * the {@code geoserver.wms.exceptions.w3cstatus=true} Spring configuration property, to assign an HTTP Status code to the response,
 * bypassing the OWS protocol, which always returns a {@code 200 OK} status code, even when errors occurred.
 * <p>
 * This is of especial interest for testing the service (e.g. for performance/scalability testing), where tools would expect errors to be reported as HTTP status codes.
 * <p>
 * The following well-known {@link ServiceException#getCode() service exception codes} are mapped:
 * <ul>
 * <li> MISSING_PARAMETER_VALUE, INVALID_PARAMETER_VALUE, "InvalidCRS" -> {@code 400 BAD_REQUEST}
 * <li> SERVICE_UNAVAILABLE, MAX_MEMORY_EXCEEDED (and rendering timeout, though it doesn't have its own code) -> {@code 503 SERVICE_UNAVAILABLE}
 * <li> Default -> {@code 500 INTERNAL_SERVER_ERROR}
 * </ul>
 *
 * @since 2.27
 */
public class StatusCodeWmsExceptionHandler extends WMSServiceExceptionHandler {

    private static final String ENABLE_PARAM = "W3CSTATUS";
    private static final String ENABLED_PROPERTY = "geoserver.wms.exceptions.w3cstatus";
    private PropertyResolver propertyResolver;

    /**
     *
     * @param services         the {@link WMSInfo}s this handler writes exceptions
     *                         for
     * @param geoServer        needed to know whether to write detailed exception
     *                         reports or not (as per
     *                         {@code GeoServer.getGlobal().isVerbose()})
     * @param propertyResolver Spring property resolver to check if
     *                         {@code geoserver.wms.exceptions.w3cstatus=true} is
     *                         set through externalized configuration
     */
    public StatusCodeWmsExceptionHandler(
            List<Service> services, GeoServer geoServer, PropertyResolver propertyResolver) {
        super(services, geoServer);
        this.propertyResolver = propertyResolver;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        setStatusCode(exception, request);
        super.handleServiceException(exception, request);
    }

    protected void setStatusCode(ServiceException exception, Request request) {
        if (shallSetStatus(request)) {
            HttpStatus status = determineStatusCode(exception);
            HttpServletResponse response = request.getHttpResponse();
            response.setStatus(status.value());
        }
    }

    private boolean shallSetStatus(Request request) {
        Object queryParam = request.getKvp().get(ENABLE_PARAM);
        if (queryParam != null) {
            // respect the query param always, it may be overriding the defaults
            return Boolean.valueOf(String.valueOf(queryParam));
        }
        return propertyResolver.getProperty(ENABLED_PROPERTY, Boolean.class, Boolean.FALSE);
    }

    private HttpStatus determineStatusCode(ServiceException exception) {
        final String code = exception.getCode();
        final String message = exception.getMessage();
        if (code == null && message != null) {
            // Some ServiceException do not have code, so check the message instead
            if (message.startsWith("This request used more time than allowed")) {
                /*
                 * RenderedImageMapOutputFormat (rendering timeout)
                 * The 503 (Service Unavailable) status code indicates that the server is
                 * currently unable to handle the request due to a temporary overload or
                 * scheduled maintenance, which will likely be alleviated after some delay.
                 */
                return HttpStatus.SERVICE_UNAVAILABLE;
            } else if (message.contains("Content has been requested in one of the following languages:")) {
                // Capabilities_1_3_0_Response (the language requested with the query param ACCEPTLANGUAGES is not
                // supported)
                return HttpStatus.BAD_REQUEST;
            }
        } else if (code != null) {
            return switch (code) {
                case MISSING_PARAMETER_VALUE, INVALID_PARAMETER_VALUE, "InvalidCRS" -> HttpStatus.BAD_REQUEST;
                case SERVICE_UNAVAILABLE, MAX_MEMORY_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
