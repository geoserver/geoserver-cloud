package org.geoserver.cloud.logging.mdc.config;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Data
public class SpringEnvironmentMdcConfigProperties {

    private boolean name = true;
    private boolean version = false;
    private boolean instanceId = false;

    /**
     * Application environment property names where to extract the instance-id from. Defaults to
     * [info.instance-id, spring.application.instance_id]
     */
    private List<String> instanceIdProperties = List.of("info.instance-id", "spring.application.instance_id");

    private boolean activeProfiles = false;

    public void addEnvironmentProperties(Environment env, Optional<BuildProperties> buildProperties) {
        if (isName()) MDC.put("application.name", env.getProperty("spring.application.name"));

        putVersion(buildProperties);
        putInstanceId(env);

        if (isActiveProfiles())
            MDC.put("spring.profiles.active", Stream.of(env.getActiveProfiles()).collect(Collectors.joining(",")));
    }

    private void putVersion(Optional<BuildProperties> buildProperties) {
        if (isVersion()) {
            buildProperties.map(BuildProperties::getVersion).ifPresent(v -> MDC.put("application.version", v));
        }
    }

    private void putInstanceId(Environment env) {
        if (!isInstanceId() || null == getInstanceIdProperties()) return;

        for (String prop : getInstanceIdProperties()) {
            String value = env.getProperty(prop);
            if (StringUtils.hasText(value)) {
                MDC.put("application.instance.id", value);
                return;
            }
        }
    }
}
