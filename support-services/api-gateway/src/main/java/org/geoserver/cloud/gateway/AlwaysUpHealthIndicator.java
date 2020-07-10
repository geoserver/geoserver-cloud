package org.geoserver.cloud.gateway;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Having a {@link HealthIndicator} is the only way so far that found to get the Eureka health
 * indicator working
 */
@Component
public class AlwaysUpHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up().build();
    }
}
