/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Allows to pass a JVM argument to exit the application upon specific {@link
 * ApplicationContextEvent application events}, mostly useful to start up an application during the
 * Docker image build process to create the AppCDS archive.
 *
 * <p>Usage: run the application with {@code -Dspring.context.exit=<event>}, where {@code <event>}
 * is one of
 *
 * <ul>
 *   <li>{@link ExitOn#onPrepared onPrepared}
 *   <li>{@link ExitOn#onRefreshed onRefreshed}
 *   <li>{@link ExitOn#onStarted onStarted}
 *   <li>{@link ExitOn#onReady onReady}
 * </ul>
 *
 * <p>Note Spring Boot 3.2 supports {@code spring.context.exit=onRefresh} as of <a
 * href="https://github.com/spring-projects/spring-framework/commit/eb3982b6c25d6c3dd49f6c4cc000c40364916a83">this
 * commit</a>, and when we migrate from Spring Boot 2.7 to 3.2+ this will not be necessary most
 * probably, although we've added additional events because some applications may fail to start
 * without all the machinery in place at different stages. Nonetheless, the new {@code offline}
 * embedded spring profile should allow them all to start without spring cloud bus, ACL, etc.
 *
 * @since 1.9.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnProperty("spring.context.exit")
@Slf4j
public class ExitOnApplicationEventAutoConfiguration {

    public enum ExitOn {
        /**
         * The {@link SpringApplication} is starting up and the {@link ApplicationContext} is fully
         * prepared but not refreshed. The bean definitions will be loaded and the {@link
         * Environment} is ready for use at this stage.
         *
         * @see ApplicationPreparedEvent
         */
        onPrepared,
        /**
         * {@code ApplicationContext} gets initialized or refreshed
         *
         * @see ContextRefreshedEvent
         */
        onRefreshed,
        /**
         * {@code ApplicationContext} has been refreshed but before any {@link ApplicationRunner
         * application} and {@link CommandLineRunner command line} runners have been called.
         *
         * @see ApplicationStartedEvent
         */
        onStarted,
        /**
         * Published as late as conceivably possible to indicate that the application is ready to
         * service requests. The source of the event is the {@link SpringApplication} itself, but
         * beware all initialization steps will have been completed by then.
         *
         * @see ApplicationReadyEvent
         */
        onReady
    }

    @Autowired
    private ApplicationContext appContext;

    @Value("${spring.context.exit}")
    ExitOn exitOn;

    @EventListener(ApplicationPreparedEvent.class)
    void exitOnApplicationPreparedEvent(ApplicationPreparedEvent event) {
        exit(ExitOn.onStarted, event.getApplicationContext());
    }

    @EventListener(ContextRefreshedEvent.class)
    void exitOnContextRefreshedEvent(ContextRefreshedEvent event) {
        exit(ExitOn.onRefreshed, event.getApplicationContext());
    }

    @EventListener(ApplicationStartedEvent.class)
    void exitOnApplicationStartedEvent(ApplicationStartedEvent event) {
        exit(ExitOn.onStarted, event.getApplicationContext());
    }

    @EventListener(ApplicationReadyEvent.class)
    void exitOnApplicationReadyEvent(ApplicationReadyEvent event) {
        exit(ExitOn.onStarted, event.getApplicationContext());
    }

    private void exit(ExitOn ifGiven, ApplicationContext applicationContext) {
        if (this.exitOn == ifGiven && applicationContext == this.appContext) {
            log.warn("Exiting application, spring.context.exit={}", ifGiven);
            try {
                ((ConfigurableApplicationContext) applicationContext).close();
            } finally {
                System.exit(0);
            }
        }
    }
}
