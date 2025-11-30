/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.flow.ControlFlowConfigurator;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.config.Intervals;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.CookieKeyGenerator;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.HttpHeaderPriorityProvider;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.IpKeyGenerator;
import org.geoserver.flow.controller.KeyGenerator;
import org.geoserver.flow.controller.OWSRequestMatcher;
import org.geoserver.flow.controller.PriorityProvider;
import org.geoserver.flow.controller.PriorityThreadBlocker;
import org.geoserver.flow.controller.RateFlowController;
import org.geoserver.flow.controller.SimpleThreadBlocker;
import org.geoserver.flow.controller.SingleIpFlowController;
import org.geoserver.flow.controller.ThreadBlocker;
import org.geoserver.flow.controller.UserConcurrentFlowController;
import org.geotools.util.logging.Logging;

/**
 * Based on {@link org.geoserver.flow.config.DefaultControlFlowConfigurator}, would require a bit of refactoring to allow extending it
 * @author Andrea Aime - OpenGeo
 * @author Juan Marin, OpenGeo
 */
class PropertiesControlFlowConfigurator implements ControlFlowConfigurator {

    static final Pattern RATE_PATTERN = Pattern.compile("(\\d+)/([smhd])(;(\\d+)s)?");

    static final Logger LOGGER = Logging.getLogger(PropertiesControlFlowConfigurator.class);

    private Properties properties;

    private boolean stale = true;

    public PropertiesControlFlowConfigurator(Properties properties) {
        this.properties = properties;
    }

    private Properties getProperties() {
        return properties;
    }

    @Override
    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean b) {
        this.stale = b;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    /**
     * Factors out the code to build a rate flow controller
     *
     * @author Andrea Aime - GeoSolutions
     */
    abstract static class RateControllerBuilder {
        public FlowController build(String[] keys, String value) {
            Matcher matcher = RATE_PATTERN.matcher(value);
            if (!matcher.matches()) {
                LOGGER.severe("Rate limiting rule values should be expressed as <rate</<unit>[;<delay>s], "
                        + "where unit can be s, m, h or d. This one is invalid: "
                        + value);
                return null;
            }
            int rate = Integer.parseInt(matcher.group(1));
            long interval = Intervals.valueOf(matcher.group(2)).getDuration();
            int delay = 0;
            String userDelay = matcher.group(4);
            if (userDelay != null) {
                delay = Integer.parseInt(userDelay) * 1000;
            }

            String service = keys.length >= 3 ? keys[2] : null;
            String request = keys.length >= 4 ? keys[3] : null;
            String format = keys.length >= 5 ? keys[4] : null;
            OWSRequestMatcher requestMatcher = new OWSRequestMatcher(service, request, format);
            KeyGenerator keyGenerator = buildKeyGenerator(keys, value);
            return new RateFlowController(requestMatcher, rate, interval, delay, keyGenerator);
        }

        protected abstract KeyGenerator buildKeyGenerator(String[] keys, String value);
    }

    long timeout = -1;

    @Override
    public List<FlowController> buildFlowControllers() throws Exception {
        timeout = -1;

        Properties p = getProperties();
        List<FlowController> newControllers = new ArrayList<>();
        PriorityProvider priorityProvider = getPriorityProvider(p);

        for (Object okey : p.keySet()) {
            String key = ((String) okey).trim();
            String value = (String) p.get(okey);

            String[] keys = key.split("\\s*\\.\\s*");

            int queueSize = 0;
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            try {
                // some properties are not integers
                if ("ip.blacklist".equals(key) || "ip.whitelist".equals(key) || "ows.priority.http".equals(key)) {
                    continue;
                } else {
                    if (!key.startsWith("user.ows") && !key.startsWith("ip.ows")) {
                        if (tokenizer.countTokens() == 1) {
                            queueSize = Integer.parseInt(value);
                        } else {
                            queueSize = Integer.parseInt(tokenizer.nextToken());
                        }
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.severe(
                        "Rules should be assigned just a queue size, instead " + key + " is associated to " + value);
                continue;
            }

            FlowController controller = null;
            if ("timeout".equalsIgnoreCase(key)) {
                timeout = queueSize * 1000;
                continue;
            }
            if ("ows.global".equalsIgnoreCase(key)) {
                controller = new GlobalFlowController(queueSize, buildBlocker(queueSize, priorityProvider));
            } else if ("ows".equals(keys[0])) {
                // todo: check, if possible, if the service, method and output format actually exist
                ThreadBlocker threadBlocker = buildBlocker(queueSize, priorityProvider);
                if (keys.length >= 4) {
                    controller = new BasicOWSController(keys[1], keys[2], keys[3], queueSize, threadBlocker);
                } else if (keys.length == 3) {
                    controller = new BasicOWSController(keys[1], keys[2], queueSize, threadBlocker);
                } else if (keys.length == 2) {
                    controller = new BasicOWSController(keys[1], queueSize, threadBlocker);
                }
            } else if ("user".equals(keys[0])) {
                if (keys.length == 1) {
                    controller = new UserConcurrentFlowController(queueSize);
                } else if ("ows".equals(keys[1])) {
                    controller = new RateControllerBuilder() {

                        @Override
                        protected KeyGenerator buildKeyGenerator(String[] keys, String value) {
                            return new CookieKeyGenerator();
                        }
                    }.build(keys, value);
                }
            } else if ("ip".equals(keys[0])) {
                if (keys.length == 1) {
                    controller = new IpFlowController(queueSize);
                } else if (keys.length > 1 && "ows".equals(keys[1])) {
                    controller = new RateControllerBuilder() {

                        @Override
                        protected KeyGenerator buildKeyGenerator(String[] keys, String value) {
                            return new IpKeyGenerator();
                        }
                    }.build(keys, value);
                } else if (keys.length > 1) {
                    if (!"blacklist".equals(keys[1]) && !"whitelist".equals(keys[1])) {
                        String ip = key.substring("ip.".length());
                        controller = new SingleIpFlowController(queueSize, ip);
                    }
                }
            }

            if (controller == null) {
                LOGGER.severe("Could not parse control-flow rule: '" + okey + "=" + value);
            } else {
                LOGGER.info("Loaded control-flow rule: " + key + "=" + value);
                newControllers.add(controller);
            }
        }

        return newControllers;
    }

    /**
     * Parses the configuration for priority providers
     *
     * @param p the configuration properties
     * @return A {@link PriorityProvider} or null if no (valid) configuration was found
     */
    private PriorityProvider getPriorityProvider(Properties p) {
        for (Object okey : p.keySet()) {
            String key = ((String) okey).trim();
            String value = (String) p.get(okey);

            // is it a priority specification?
            if ("ows.priority.http".equals(key)) {
                String error = "";
                try {
                    String[] splitValue = value.trim().split("\\s*,\\s*");
                    if (splitValue.length == 2 && splitValue[0].length() > 0) {
                        String httpHeaderName = splitValue[0];
                        int defaultPriority = Integer.parseInt(splitValue[1]);

                        LOGGER.info("Found OWS priority specification " + key + "=" + value);
                        return new HttpHeaderPriorityProvider(httpHeaderName, defaultPriority);
                    }
                } catch (NumberFormatException e) {
                    error = " " + e.getMessage();
                }

                LOGGER.severe("Unexpected priority specification found '"
                        + value
                        + "', "
                        + "the expected format is headerName,defaultPriorityValue."
                        + error);
            }
        }
        return null;
    }

    /**
     * Builds a {@link ThreadBlocker} based on a queue size and a prority provider
     *
     * @param queueSize The count of concurrent requests allowed to run
     * @param priorityProvider The priority provider (if not null, a
     *     {@link org.geoserver.flow.controller.PriorityThreadBlocker} will be built
     * @return a {@link ThreadBlocker}
     */
    private ThreadBlocker buildBlocker(int queueSize, PriorityProvider priorityProvider) {
        if (priorityProvider != null) {
            return new PriorityThreadBlocker(queueSize, priorityProvider);
        } else {
            return new SimpleThreadBlocker(queueSize);
        }
    }
}
