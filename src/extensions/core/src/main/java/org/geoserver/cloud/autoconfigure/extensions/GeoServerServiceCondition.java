/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

/**
 * {@link org.springframework.context.annotation.Condition} implementation backing
 * {@link ConditionalOnGeoServerService}.
 *
 * <p>Reads every occurrence of the {@link ConditionalOnGeoServerService} annotation attributes present on the annotated
 * element and ANDs their outcomes, following the same pattern as Spring Boot's {@code OnPropertyCondition}. This
 * matters because Spring deduplicates condition classes when two {@code @ConditionalOnGeoServerService}
 * meta-annotations meet on the same element. All requested service names must therefore be resolved together in a
 * single {@link #getMatchOutcome} call.
 */
public class GeoServerServiceCondition extends SpringBootCondition {

    static final String SERVICES_PROPERTY = "geoserver.services";
    static final String ALL = "all";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<String> services = serviceNames(metadata);
        Environment env = context.getEnvironment();
        for (String service : services) {
            ConditionOutcome outcome = evaluate(service, env);
            if (!outcome.isMatch()) {
                return outcome;
            }
        }
        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnGeoServerService.class)
                .because("enabled: %s".formatted(services)));
    }

    private ConditionOutcome evaluate(String service, Environment env) {
        ConditionMessage.Builder message =
                ConditionMessage.forCondition(ConditionalOnGeoServerService.class, "(%s)".formatted(service));
        String flagProperty = "geoserver.service.%s.enabled".formatted(service);
        String flag = env.getProperty(flagProperty);
        if (flag != null) {
            boolean enabled = Boolean.parseBoolean(flag);
            return new ConditionOutcome(enabled, message.because("%s=%s".formatted(flagProperty, flag)));
        }
        String list = env.getProperty(SERVICES_PROPERTY);
        if (list == null || list.isBlank()) {
            return ConditionOutcome.noMatch(
                    message.didNotFind(SERVICES_PROPERTY).atAll());
        }
        Set<String> enabledServices = parse(list);
        boolean enabled = enabledServices.contains(ALL) || enabledServices.contains(service);
        return new ConditionOutcome(enabled, message.because("%s=%s".formatted(SERVICES_PROPERTY, list)));
    }

    private List<String> serviceNames(AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> attributes =
                metadata.getAllAnnotationAttributes(ConditionalOnGeoServerService.class.getName());
        List<Object> values = attributes == null ? List.of() : attributes.getOrDefault("value", List.of());
        return values.stream()
                .map(String.class::cast)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
    }

    private Set<String> parse(String commaSeparated) {
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
