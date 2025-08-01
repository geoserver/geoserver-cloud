Centralized transpiled configuration for GeoServer modules and plugings

This module acts as a centralized dependency containing transpiled GeoServer `applicationContext.xml` Spring XML
configurations to Spring `@Configuration` classes.

All GeoServer dependencies are optional and used to generate their configuration classes, in order
to apply the transpiling of XML to Java configuration classes in one place.

This module contains pure Spring configuration classes that can then be imported by
Spring Boot `@AutoConfiguration` classes in specific modules.

For example:

```java
@AutoConfiguration
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWebUI
@EnableConfigurationProperties(OgcApiFeatureConfigProperties.class)
@Import(OgcApiFeaturesWebUIConfiguration.class)
class OgcApiFeaturesWebUIAutoConfiguration {}
```

Defines the conditionals to enable OGC API Features WebUI and imports `OgcApiFeaturesWebUIConfiguration.java` from this module.