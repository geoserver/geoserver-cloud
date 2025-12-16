# GeoServer Cloud: Spring Boot Architecture Migration Plan

## Why This Migration is Necessary

### Current Architectural Problems

#### 1. **Violation of Spring Boot Patterns**
Our current architecture violates fundamental Spring Boot principles:

```java
// ❌ WRONG: Mixed responsibilities in AutoConfiguration
@AutoConfiguration
@TranspileXmlConfig(...)  // XML processing logic
@Import(Generated.class)
public class WmsApplicationAutoConfiguration {
    
    @Bean  // Explicit configuration - shouldn't be in AutoConfiguration
    LegendSample legendSample() { ... }
    
    @Bean  // Override logic - shouldn't be in AutoConfiguration
    WMSServiceExceptionHandler wmsExceptionHandler() { ... }
}

// ❌ WRONG: Code in starters
// File: src/starters/observability/.../LoggingMDCServletAutoConfiguration.java
@AutoConfiguration  // This should NOT be in a starter module!
public class LoggingMDCServletAutoConfiguration {
    @Bean
    MDCCleaningFilter mdcCleaningServletFilter() { ... }  // Code in starter!
}
```

#### 2. **Scattered Configuration Properties**
Configuration properties are scattered across 42 different modules, making it impossible to:
- Understand the complete configuration structure
- Validate cross-module configuration
- Generate unified documentation
- Provide IDE autocomplete for the full config hierarchy

#### 3. **No Clear Separation of Concerns**
Currently, a single class handles:
- Conditional activation logic
- XML-to-Java transpilation 
- Explicit bean configuration
- Property binding
- Override customizations

This makes code hard to:
- Test (can't test conditions separate from setup)
- Maintain (changes affect multiple concerns)
- Extend (adding conditions affects setup logic)
- Debug (unclear where problems originate)

## The Spring Boot Model

### How Spring Boot Organizes Things

Spring Boot follows a **clear layered architecture** with distinct responsibilities:

#### **1. Starters: Dependency Aggregation Only**
```xml
<!-- spring-boot-starter-web/pom.xml -->
<dependencies>
    <!-- NO Java source code - only dependency declarations -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-core</artifactId>
    </dependency>
    <!-- etc... -->
</dependencies>
```

**Purpose**: Make it easy for users to pull in related functionality with a single dependency.

#### **2. AutoConfiguration: Conditional Logic Only**
```java
// spring-boot-autoconfigure/src/.../WebMvcAutoConfiguration.java
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
@Import({ ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
public class WebMvcAutoConfiguration {
    // Conditional logic only - delegates actual setup to @Configuration classes
}
```

**Purpose**: Decide WHEN to activate functionality based on classpath, properties, and other conditions.

#### **3. Configuration Classes: Explicit Setup**
```java
// spring-webmvc/src/.../WebMvcConfigurationSupport.java
@Configuration
public class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {
    
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        // Explicit bean creation logic
    }
    
    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        // Explicit bean creation logic
    }
    
    // All the actual setup work
}
```

**Purpose**: HOW to set up the functionality once conditions are met.

#### **4. Configuration Properties: Contract Definition**
```java
// spring-boot-autoconfigure/src/.../WebMvcProperties.java
@ConfigurationProperties("spring.mvc")
public class WebMvcProperties {
    private String staticPathPattern = "/**";
    private final Async async = new Async();
    private final Servlet servlet = new Servlet();
    
    // Defines the configuration contract
}
```

**Purpose**: Define WHAT can be configured and establish the configuration contract.

#### **5. Centralized Configuration Properties Module**

Spring Boot keeps **all** `@ConfigurationProperties` classes in the **autoconfigure module**, making them:
- **Discoverable** - one place to find all configuration options
- **Reusable** - shared between autoconfigure and implementation modules  
- **Documentable** - can generate complete configuration reference
- **Validatable** - can validate cross-property constraints

```
spring-boot-autoconfigure/
├── web/WebMvcProperties.java              # Web configuration
├── data/DataSourceProperties.java         # Data configuration  
├── security/SecurityProperties.java       # Security configuration
└── actuator/ActuatorProperties.java       # Actuator configuration
```

**Key Benefits:**
- **Single source of truth** for all configuration options
- **IDE autocomplete** works across entire configuration hierarchy
- **Cross-module validation** can ensure consistent configuration
- **Documentation generation** can create complete reference

#### **5. Module Architecture**
```
spring-boot-starter-web/           # Starter (dependencies only)
├── pom.xml                       # Just pulls in related modules
└── README.md                     # Documentation

spring-boot-autoconfigure/         # AutoConfiguration logic
├── WebMvcAutoConfiguration.java  # Conditional activation
├── WebMvcProperties.java         # Configuration contract
└── META-INF/spring/              # Auto-discovery
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports

spring-webmvc/                    # Implementation
├── DispatcherServlet.java        # Business logic
├── config/WebMvcConfigurer.java  # Explicit configuration
└── (no autoconfiguration classes)
```

### Why This Model Works

#### **1. Single Responsibility Principle**
- **Starters**: Only dependency management
- **AutoConfiguration**: Only conditional logic  
- **Configuration**: Only explicit setup
- **Properties**: Only configuration binding

#### **2. Testability**
```java
// Test conditional logic separately
@AutoConfigurationTest(WebMvcAutoConfiguration.class)
class WebMvcAutoConfigurationTest {
    // Test conditions without setup complexity
}

// Test setup logic separately  
@ConfigurationTest(WebMvcConfigurationSupport.class)
class WebMvcConfigurationTest {
    // Test setup without conditional complexity
}
```

#### **3. Extensibility**
```java
// Users can override any part independently
@Configuration
public class MyCustomWebConfig {
    
    @Bean
    @Primary  // Override autoconfigured bean
    public RequestMappingHandlerMapping customMapping() {
        return new CustomRequestMappingHandlerMapping();
    }
}
```

#### **4. Discoverability**
- All configuration properties in central location
- All conditional logic in central location
- Clear documentation of what can be configured
- IDE support for configuration hierarchy

## Our Target Architecture

### **Vision: Spring Boot Compliant Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│ STAGE 2: Module Separation (Future)                        │
├─────────────────────────────────────────────────────────────┤
│ Applications (Minimal POMs)                                │
│ ├── gs-cloud-wms-app         (uses gs-cloud-starter-wms)   │
│ ├── gs-cloud-wfs-app         (uses gs-cloud-starter-wfs)   │
│ └── gs-cloud-webui-app       (uses gs-cloud-starter-webui) │
│                                                             │
│ Starters (Dependencies Only - Like Spring Boot)           │
│ ├── gs-cloud-starter-wms     (pom.xml only)               │
│ ├── gs-cloud-starter-wfs     (pom.xml only)               │
│ └── gs-cloud-starter-webui   (pom.xml only)               │
│                                                             │
│ AutoConfiguration (Central - Like spring-boot-autoconfigure) │
│ └── gs-cloud-autoconfigure   (all @AutoConfiguration classes) │
│                                                             │
│ Configuration Contracts (Central)                          │
│ └── gs-cloud-config-properties (all @ConfigurationProperties) │
│                                                             │
│ Implementation Modules (Business Logic)                    │
│ ├── catalog/backends/common     (services, no @AutoConfiguration) │
│ ├── extensions/inspire          (services, no @AutoConfiguration) │
│ └── other modules...            (services, no @AutoConfiguration) │
└─────────────────────────────────────────────────────────────┘
```

### **Configuration Structure: From Scattered to Hierarchical**

#### **Current (Scattered)**
```
# 42 different @ConfigurationProperties classes in 42 different modules
/src/extensions/security/ldap/.../LDAPConfigProperties.java
/src/apps/geoserver/webui/.../WebUIConfigurationProperties.java  
/src/gwc/core/.../GeoWebCacheConfigurationProperties.java
# etc... impossible to understand complete structure
```

#### **Target (Hierarchical)**
```yaml
# Single, discoverable configuration hierarchy
geoserver:
  catalog:
    backend:
      data-directory:
        enabled: true
        location: /data
      jdbcconfig:
        enabled: false
      pgconfig: 
        enabled: false
    cache:
      enabled: true
  extension:
    security:
      ldap:
        enabled: true
        server-url: ldap://localhost
      jdbc:
        enabled: false
    output-formats:
      vector-tiles:
        enabled: true
      flatgeobuf:
        enabled: false
  services:
    wms:
      enabled: true
      reflector:
        enabled: true
    wfs:
      enabled: true
    rest:
      enabled: true
  web-ui:
    enabled: false
    demos:
      enabled: false
```

## Migration Plan

### **STAGE 1: Clean Up Current Architecture**

**Goal**: Separate responsibilities WITHIN current module structure, preparing for Stage 2.

#### **Problem → Solution Pattern**

Every mixed AutoConfiguration gets split into **4 clean classes**:

##### **1. Pure AutoConfiguration (Conditional Only)**
```java
// BEFORE: Mixed responsibilities
@AutoConfiguration
@TranspileXmlConfig(...)  // ❌ Should be separate
@Import(Generated.class)
public class WmsApplicationAutoConfiguration {
    @Bean                 // ❌ Should be separate
    LegendSample legendSample() { ... }
}

// AFTER: Pure conditional logic
@AutoConfiguration(before = WMSIntegrationAutoConfiguration.class)
@ConditionalOnClass(org.geoserver.wms.WMS.class)
@ConditionalOnProperty(
    prefix = "geoserver.services.wms", 
    name = "enabled", 
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(WmsConfigProperties.class)
@Import({
    WmsTranspiledConfiguration.class,  // XML processing
    WmsModuleConfiguration.class       // Explicit setup
})
public class WmsAutoConfiguration {
    // NO @Bean methods - pure conditional logic only
}
```

##### **2. Configuration Properties (Binding Only)**
```java
@ConfigurationProperties("geoserver.services.wms")
@Data
public class WmsConfigProperties {
    private boolean enabled = true;
    
    @NestedConfigurationProperty
    private ReflectorConfig reflector = new ReflectorConfig();
    
    @Data
    public static class ReflectorConfig {
        private boolean enabled = true;
    }
}
```

##### **3. No More Transpiled Configuration in Modules**
```java
// STAGE 1: Remove XML transpilation from individual modules
// XML transpilation will be moved to gs-cloud-xml-configuration module in Stage 2

// Individual modules will only import from the XML configuration module:
@AutoConfiguration  
@ConditionalOnClass(org.geoserver.wms.WMS.class)
@ConditionalOnProperty("geoserver.services.wms.enabled")
@Import({
    // Will import from gs-cloud-xml-configuration module (Stage 2)
    // WmsXmlConfiguration.class,     // Vanilla GeoServer beans (from xml module)
    WmsModuleConfiguration.class      // Cloud-specific overrides only
})
public class WmsAutoConfiguration {
    // Pure conditional logic only
}
```

##### **4. Module Configuration (Explicit Setup Only)**
```java
@Configuration
public class WmsModuleConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    LegendSample legendSample(
            @Qualifier("rawCatalog") Catalog catalog, 
            GeoServerResourceLoader loader) {
        return new LegendSampleImpl(catalog, loader);
    }
    
    @Bean
    @ConditionalOnMissingBean
    WMSServiceExceptionHandler wmsExceptionHandler(
            @Qualifier("wms-1_1_1-ServiceDescriptor") Service wms11,
            @Qualifier("wms-1_3_0-ServiceDescriptor") Service wms13,
            GeoServer geoServer,
            WmsConfigProperties config) {
        return new StatusCodeWmsExceptionHandler(List.of(wms11, wms13), geoServer, config);
    }
    
    // All explicit @Bean methods, overrides, customizations
}
```

#### **Starter Cleanup Pattern**

##### **Current Problem: Code in Starters**
```
src/starters/observability/
├── src/main/java/                              # ❌ Code in starter!
│   └── org/geoserver/cloud/autoconfigure/
│       └── logging/
│           └── LoggingMDCServletAutoConfiguration.java
├── src/main/resources/META-INF/spring.factories # ❌ AutoConfiguration discovery
└── pom.xml                                     # ✅ Dependencies (correct)
```

##### **Target: True Starter Pattern**
```
src/starters/observability/                    # True starter (like Spring Boot)
├── pom.xml                                    # ✅ Only dependencies
└── README.md                                  # ✅ Documentation only

src/autoconfigure-observability/               # NEW: AutoConfiguration module  
├── src/main/java/org/geoserver/cloud/autoconfigure/
│   ├── logging/
│   │   ├── LoggingMDCAutoConfiguration.java   # Pure @AutoConfiguration
│   │   └── LoggingMDCConfiguration.java       # Explicit @Configuration
│   └── properties/
│       └── LoggingConfigProperties.java       # @ConfigurationProperties
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── pom.xml                                    # Implementation dependencies
```

### **STAGE 2: Module Separation**

**Goal**: Create centralized Spring Boot-compliant architecture.

#### **New Module Structure**

##### **XML Configuration Module: Pure Vanilla GeoServer Transpilation**

**Key Insight**: XML transpilation is only needed for **vanilla GeoServer JARs** (external dependencies), not GeoServer Cloud modules. This enables perfect separation:

```
gs-cloud-xml-configuration/           # NEW: Pure XML transpilation module
├── src/main/java/org/geoserver/cloud/xml/
│   ├── WmsXmlConfiguration.java     # @TranspileXmlConfig for gs-wms.jar
│   ├── WfsXmlConfiguration.java     # @TranspileXmlConfig for gs-wfs.jar
│   ├── WcsXmlConfiguration.java     # @TranspileXmlConfig for gs-wcs.jar
│   ├── InspireXmlConfiguration.java # @TranspileXmlConfig for gs-inspire.jar
│   ├── LdapSecurityXmlConfiguration.java # @TranspileXmlConfig for gs-security-ldap.jar
│   └── etc...                       # One @Configuration per vanilla GeoServer JAR
├── src/main/java/org/geoserver/cloud/xml/generated/
│   ├── wms/WmsTranspiledBeans.java      # Generated by annotation processor
│   ├── wfs/WfsTranspiledBeans.java      # Generated by annotation processor
│   ├── inspire/InspireTranspiledBeans.java # Generated by annotation processor
│   └── etc...                           # All transpiled classes
└── pom.xml                              # Optional deps on ALL vanilla GeoServer JARs
```

**Benefits:**
- **Single processor setup** - annotation processor configured in only one module
- **Pure separation** - XML transpilation completely independent of autoconfigure
- **Optional dependencies** - only transpile XML for JARs actually present
- **Reusable** - any project can use vanilla GeoServer beans via this module

**Example Configuration Classes:**
```java
// One @Configuration class per vanilla GeoServer JAR
@TranspileXmlConfig(
    locations = "jar:gs-wms-.*!/applicationContext.xml",
    targetPackage = "org.geoserver.cloud.xml.generated.wms",
    targetClass = "WmsTranspiledBeans",
    excludes = {"legendSample", "wmsExceptionHandler"}
)
@Import(org.geoserver.cloud.xml.generated.wms.WmsTranspiledBeans.class)
@Configuration
public class WmsXmlConfiguration {
    // Pure XML → Java transpilation for gs-wms.jar
}

@TranspileXmlConfig(
    locations = "jar:gs-inspire-.*!/applicationContext.xml",
    targetPackage = "org.geoserver.cloud.xml.generated.inspire",
    targetClass = "InspireTranspiledBeans",
    includes = {"inspireWmsExtendedCapsProvider", "languageCallback", "inspireDirManager"}
)
@Import(org.geoserver.cloud.xml.generated.inspire.InspireTranspiledBeans.class)
@Configuration
public class InspireXmlConfiguration {
    // Pure XML → Java transpilation for gs-inspire.jar
}
```

##### **Configuration Properties Module: The Missing Piece**
```
gs-cloud-config-properties/           # Configuration contracts (lightweight, no dependencies)
├── src/main/java/org/geoserver/cloud/config/
│   ├── GeoServerCloudProperties.java          # Master configuration class
│   ├── catalog/
│   │   ├── CatalogProperties.java             # "geoserver.catalog"
│   │   ├── backend/
│   │   │   ├── data_directory/
│   │   │   │   └── DataDirectoryProperties.java    # "geoserver.catalog.backend.data-directory"
│   │   │   ├── jdbcconfig/
│   │   │   │   └── JdbcConfigProperties.java       # "geoserver.catalog.backend.jdbcconfig"
│   │   │   └── pgconfig/
│   │   │       └── PgconfigProperties.java         # "geoserver.catalog.backend.pgconfig"
│   │   └── cache/
│   │       └── CacheProperties.java            # "geoserver.catalog.cache"
│   ├── extension/
│   │   ├── security/
│   │   │   ├── ldap/
│   │   │   │   └── LDAPConfigProperties.java       # "geoserver.extension.security.ldap"
│   │   │   ├── jdbc/
│   │   │   │   └── JDBCConfigProperties.java       # "geoserver.extension.security.jdbc"
│   │   │   └── auth_key/
│   │   │       └── AuthKeyConfigProperties.java    # "geoserver.extension.security.auth-key"
│   │   ├── output_formats/
│   │   │   ├── vector_tiles/
│   │   │   │   └── VectorTilesConfigProperties.java # "geoserver.extension.output-formats.vector-tiles"
│   │   │   └── flatgeobuf/
│   │   │       └── FlatGeobufConfigProperties.java  # "geoserver.extension.output-formats.flatgeobuf"
│   │   └── inspire/
│   │       └── InspireConfigProperties.java         # "geoserver.extension.inspire"
│   ├── services/
│   │   ├── wms/
│   │   │   └── WmsConfigProperties.java             # "geoserver.services.wms"
│   │   ├── wfs/
│   │   │   └── WfsConfigProperties.java             # "geoserver.services.wfs"
│   │   └── wcs/
│   │       └── WcsConfigProperties.java             # "geoserver.services.wcs"
│   ├── web_ui/
│   │   └── WebUIConfigurationProperties.java       # "geoserver.web-ui"
│   └── gwc/
│       └── GeoWebCacheConfigProperties.java        # "geoserver.gwc"
└── pom.xml                                         # NO dependencies on implementation modules
```

**Master Configuration Class:**
```java
// org.geoserver.cloud.config.GeoServerCloudProperties
@ConfigurationProperties("geoserver")
@Data
public class GeoServerCloudProperties {
    
    @NestedConfigurationProperty
    private CatalogProperties catalog = new CatalogProperties();
    
    @NestedConfigurationProperty  
    private ExtensionProperties extension = new ExtensionProperties();
    
    @NestedConfigurationProperty
    private ServicesProperties services = new ServicesProperties();
    
    @NestedConfigurationProperty
    private WebUIProperties webUi = new WebUIProperties();
    
    @NestedConfigurationProperty
    private GwcProperties gwc = new GwcProperties();
    
    // Nested classes for hierarchical organization
    @Data
    public static class CatalogProperties {
        @NestedConfigurationProperty
        private BackendProperties backend = new BackendProperties();
        
        @NestedConfigurationProperty
        private CacheProperties cache = new CacheProperties();
        
        @Data
        public static class BackendProperties {
            @NestedConfigurationProperty
            private DataDirectoryProperties dataDirectory = new DataDirectoryProperties();
            
            @NestedConfigurationProperty
            private JdbcConfigProperties jdbcconfig = new JdbcConfigProperties();
            
            @NestedConfigurationProperty
            private PgconfigProperties pgconfig = new PgconfigProperties();
        }
    }
    
    @Data
    public static class ExtensionProperties {
        @NestedConfigurationProperty
        private SecurityProperties security = new SecurityProperties();
        
        @NestedConfigurationProperty
        private OutputFormatsProperties outputFormats = new OutputFormatsProperties();
        
        @NestedConfigurationProperty
        private InspireConfigProperties inspire = new InspireConfigProperties();
        
        @Data
        public static class SecurityProperties {
            @NestedConfigurationProperty
            private LDAPConfigProperties ldap = new LDAPConfigProperties();
            
            @NestedConfigurationProperty
            private JDBCConfigProperties jdbc = new JDBCConfigProperties();
            
            @NestedConfigurationProperty
            private AuthKeyConfigProperties authKey = new AuthKeyConfigProperties();
        }
    }
    
    @Data
    public static class ServicesProperties {
        @NestedConfigurationProperty
        private WmsConfigProperties wms = new WmsConfigProperties();
        
        @NestedConfigurationProperty
        private WfsConfigProperties wfs = new WfsConfigProperties();
        
        @NestedConfigurationProperty
        private WcsConfigProperties wcs = new WcsConfigProperties();
    }
}
```

**Key Benefits of Centralized Configuration Properties:**

1. **Single Source of Truth**
```java
// All configuration discoverable in one place
@Autowired
GeoServerCloudProperties config;

// Access any part of the hierarchy
boolean wmsEnabled = config.getServices().getWms().isEnabled();
String ldapServer = config.getExtension().getSecurity().getLdap().getServerUrl();
boolean dataDirEnabled = config.getCatalog().getBackend().getDataDirectory().isEnabled();
```

2. **IDE Autocomplete Support**
```java
// Full IDE support for entire configuration hierarchy
geoServerConfig.
    getCatalog().
        getBackend().
            getDataDirectory().  // IDE shows all available properties
                getLocation()
```

3. **Configuration Validation**
```java
@Component
public class ConfigurationValidator {
    
    @Autowired
    private GeoServerCloudProperties config;
    
    @PostConstruct
    public void validate() {
        var backends = config.getCatalog().getBackend();
        
        // Ensure only one backend is enabled
        int enabledBackends = 0;
        if (backends.getDataDirectory().isEnabled()) enabledBackends++;
        if (backends.getJdbcconfig().isEnabled()) enabledBackends++;
        if (backends.getPgconfig().isEnabled()) enabledBackends++;
        
        if (enabledBackends != 1) {
            throw new IllegalStateException("Exactly one catalog backend must be enabled");
        }
        
        // Ensure WebUI doesn't run without REST API
        if (config.getWebUi().isEnabled() && !config.getServices().getRest().isEnabled()) {
            throw new IllegalStateException("WebUI requires REST API to be enabled");
        }
    }
}
```

4. **Documentation Generation**
```java
// Can automatically generate configuration reference
@Component
public class ConfigurationDocumentationGenerator {
    
    public void generateConfigDocs() {
        // Use reflection on GeoServerCloudProperties to generate
        // complete configuration documentation with all nested properties
    }
}
```

gs-cloud-autoconfigure/               # AutoConfiguration logic (like spring-boot-autoconfigure)  
├── services/
│   ├── WmsAutoConfiguration.java            # Conditional logic only
│   ├── WmsConfiguration.java                # Explicit overrides + cloud-specific beans
│   ├── WfsAutoConfiguration.java            # Conditional logic only
│   └── WfsConfiguration.java                # Explicit overrides + cloud-specific beans
├── extensions/
│   ├── inspire/
│   │   ├── InspireAutoConfiguration.java    # Conditional logic only
│   │   └── InspireConfiguration.java        # Explicit overrides + cloud-specific beans
│   └── security/
│       ├── LdapAutoConfiguration.java       # Conditional logic only
│       └── LdapConfiguration.java           # Explicit overrides + cloud-specific beans
├── catalog/
│   ├── DataDirectoryAutoConfiguration.java # Conditional logic only
│   └── DataDirectoryConfiguration.java     # Explicit setup + cloud-specific beans
└── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

# How AutoConfiguration uses XML Configuration:
@AutoConfiguration
@ConditionalOnClass(org.geoserver.wms.WMS.class)
@ConditionalOnProperty("geoserver.services.wms.enabled")
@Import({
    WmsXmlConfiguration.class,     # Import vanilla GeoServer beans (from xml module)
    WmsConfiguration.class         # Import cloud-specific overrides + additions
})
public class WmsAutoConfiguration {
    // Pure conditional logic - delegates to XML + explicit config
}

##### **Complete Starter Ecosystem**

###### **Feature-Based Starters (Horizontal Composition)**
```
gs-cloud-starter-extensions/          # Aggregates ALL extensions
└── pom.xml                           # → input-formats + output-formats + security + features

gs-cloud-starter-input-formats/       # All input format extensions  
└── pom.xml                           # → vector-formats + raster-formats

gs-cloud-starter-output-formats/      # All output format extensions
└── pom.xml                           # → vector-tiles + flatgeobuf + dxf + etc.

gs-cloud-starter-security/            # All security extensions
└── pom.xml                           # → ldap + jdbc + auth-key + oauth2 + etc.
```

###### **Service-Based Starters (Vertical Composition)**
```
gs-cloud-starter-wms/                 # Minimal WMS microservice
└── pom.xml                           # → autoconfigure + gs-wms + web + output-formats

gs-cloud-starter-wfs/                 # Minimal WFS microservice  
└── pom.xml                           # → autoconfigure + gs-wfs + web + output-formats

gs-cloud-starter-webui/               # Full admin interface
└── pom.xml                           # → autoconfigure + ALL modules + wicket + extensions

gs-cloud-starter-all/                 # Monolith deployment
└── pom.xml                           # → autoconfigure + ALL services + ALL extensions
```

##### **Deployment Scenarios**

###### **1. Microservice Deployments**
```xml
<!-- WMS-only microservice -->
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-wms</artifactId>
</dependency>

<!-- Admin-only microservice -->  
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-webui</artifactId>
</dependency>
```

Configuration:
```yaml
# WMS microservice
geoserver:
  services:
    wms: true
    wfs: false
    wcs: false
  web-ui:
    enabled: false

# Admin microservice  
geoserver:
  services:
    wms: false
    wfs: false  
    wcs: false
    rest: true    # WebUI needs REST
  web-ui:
    enabled: true
```

###### **2. Monolith Deployment**
```xml
<!-- All services in one application -->
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-all</artifactId>
</dependency>
```

Configuration:
```yaml
# All services enabled
geoserver:
  services:
    wms: true
    wfs: true
    wcs: true
    rest: true
  web-ui:
    enabled: true
```

###### **3. Custom Deployments (Mix and Match)**
```xml
<!-- WMS with specific extensions -->
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-wms</artifactId>
</dependency>
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-security</artifactId>
</dependency>
<!-- Add specific extension -->
<dependency>
    <groupId>org.geoserver.cloud.extensions</groupId>
    <artifactId>gs-cloud-extension-inspire</artifactId>
</dependency>
```

Configuration:
```yaml
# Custom WMS with LDAP security and INSPIRE
geoserver:
  services:
    wms: true
    wfs: false
  extension:
    security:
      ldap: true
      jdbc: false
    inspire: true
```

gs-cloud-wms-app/                     # Application (like user's @SpringBootApplication)
├── WmsApplication.java               # @SpringBootApplication
└── pom.xml                           # Depends on gs-cloud-starter-wms
```

#### **Benefits of This Architecture**

##### **1. Spring Boot Compliance**
- Follows all official Spring Boot patterns
- Same experience as using spring-boot-starter-web
- Familiar to all Spring Boot developers

##### **2. Configuration Discoverability**
```java
// Single configuration contract - IDE can autocomplete entire hierarchy
@Autowired
GeoServerCloudProperties config;

// Or inject specific parts
@Autowired  
WmsConfigProperties wmsConfig;

@Autowired
LDAPConfigProperties ldapConfig;
```

##### **3. Flexible Deployment**
```yaml
# Monolith deployment
geoserver:
  services:
    wms: true
    wfs: true 
    wcs: true
  web-ui:
    enabled: true

# Microservice deployment  
geoserver:
  services:
    wms: true      # Only WMS enabled
    wfs: false
    wcs: false
  web-ui:
    enabled: false
```

##### **4. Easy Testing**
```java
// Test individual services
@SpringBootTest
@TestPropertySource(properties = {
    "geoserver.services.wms=true",
    "geoserver.services.wfs=false"
})
class WmsOnlyTest {
    // Only WMS beans are created
}

// Test configurations separately
@AutoConfigurationTest(WmsAutoConfiguration.class)  
class WmsConditionsTest {
    // Test conditional logic without setup complexity
}
```

##### **5. Flexible Starter Composition**

**Horizontal Composition (Features)**
```xml
<!-- Pick and choose feature sets -->
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-wms</artifactId>  <!-- Service -->
</dependency>
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-output-formats</artifactId>  <!-- Feature -->
</dependency>
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-security</artifactId>  <!-- Feature -->
</dependency>
```

**Vertical Composition (Services)**
```xml
<!-- Complete service stacks -->
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-all</artifactId>  <!-- Everything -->
</dependency>

<!-- OR minimal service -->
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-wms</artifactId>  <!-- Just WMS -->
</dependency>
```

**Configuration-Based Activation**
```yaml
# Same JARs, different runtime behavior
geoserver:
  services:
    wms: true      # Enable WMS (if JAR present)
    wfs: false     # Disable WFS (even if JAR present)  
  extension:
    security:
      ldap: true   # Enable LDAP (if JAR present)
      jdbc: false  # Disable JDBC (even if JAR present)
```

##### **5. Configuration Properties Usage**

**Independent Injection (Module-Specific)**
```java
// Implementation modules can inject specific properties directly
@Service
public class DataDirectoryService {
    
    @Autowired
    private DataDirectoryProperties config;  // Direct injection of specific properties
    
    public void initialize() {
        if (config.isEnabled()) {
            String location = config.getLocation();
            // Use configuration...
        }
    }
}
```

**Hierarchical Access (Cross-Module)**
```java
// Tools and validation can access full hierarchy
@Component
public class ConfigurationValidator {
    
    @Autowired
    private GeoServerCloudProperties allConfig;  // Full hierarchy access
    
    public void validateConfiguration() {
        // Access any part of the configuration tree
        boolean wmsEnabled = allConfig.getServices().getWms().isEnabled();
        boolean dataDirEnabled = allConfig.getCatalog().getBackend().getDataDirectory().isEnabled();
        
        // Cross-module validation logic
        if (wmsEnabled && !dataDirEnabled) {
            throw new IllegalStateException("WMS requires a catalog backend");
        }
    }
}
```

**Dependency Flow (Clean Separation)**
```
┌─────────────────────────────────────────────────────────────┐
│ Applications                                                │
│ ├── gs-cloud-wms-app       (depends on starter)           │
│ └── gs-cloud-webui-app     (depends on starter)           │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│ Starters                                                    │
│ ├── gs-cloud-starter-wms   (depends on autoconfigure)     │
│ └── gs-cloud-starter-webui (depends on autoconfigure)     │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│ gs-cloud-autoconfigure     (conditional logic + overrides) │
│ ├── @AutoConfiguration classes (conditional logic)         │
│ └── @Configuration classes (explicit overrides)            │
└─────────┬───────────────────────────┬─────────────────────────┘
          │                           │
┌─────────▼──────────────────────┐ ┌──▼─────────────────────────────┐
│ gs-cloud-config-properties     │ │ gs-cloud-xml-configuration     │
│ (configuration contracts)      │ │ (vanilla GeoServer transpilation)│
│ ├── All @ConfigurationProperties│ │ ├── @TranspileXmlConfig        │
│ └── Master GeoServerCloudProperties│ │ ├── Generated vanilla beans   │
│                                │ │ └── Single processor setup    │
└────────────────────────────────┘ └┬───────────────────────────────┘
                                    │
                             ┌──────▼──────────────────────────────┐
                             │ Vanilla GeoServer JARs              │
                             │ ├── gs-wms.jar (optional)           │
                             │ ├── gs-wfs.jar (optional)           │
                             │ ├── gs-inspire.jar (optional)       │
                             │ └── gs-security-ldap.jar (optional) │
                             └─────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Implementation Modules     (pure business logic)           │
│ ├── catalog/backends/common    (depends on config-properties)│
│ ├── extensions/inspire         (depends on config-properties)│
│ ├── extensions/security/*      (depends on config-properties)│
│ └── other business logic modules                           │
└─────────────────────────────────────────────────────────────┘
```

##### **6. Single Docker Image, Multiple Deployments**
```dockerfile
# Same image for all deployments
FROM openjdk:17-jre-slim
COPY gs-cloud-all-services.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

```bash
# Deploy as WMS microservice
docker run -e "SPRING_PROFILES_ACTIVE=wms" geoserver-cloud

# Deploy as monolith
docker run -e "SPRING_PROFILES_ACTIVE=monolith" geoserver-cloud

# Deploy as WebUI only
docker run -e "SPRING_PROFILES_ACTIVE=webui" geoserver-cloud
```

## Implementation Timeline

### **Phase 1: Stage 1 - Core Services (Week 1-2)**
- [ ] **WmsApplicationAutoConfiguration** → 4-class split
- [ ] **WfsApplicationAutoConfiguration** → 4-class split  
- [ ] **WcsApplicationConfiguration** → 4-class split
- [ ] **WebUIAutoConfiguration** → 4-class split

### **Phase 2: Stage 1 - Extensions (Week 3)**
- [ ] **InspireConfigurationWms/Wfs/Wcs** → Split pattern
- [ ] **VectorTilesAutoConfiguration** → Split pattern
- [ ] **SecurityAutoConfigurations** → Split pattern
- [ ] **GWC AutoConfigurations** → Split pattern

### **Phase 3: Stage 1 - Starter Cleanup (Week 4)**

#### **Fix Problematic Starters (Remove Code)**
- [ ] **observability starter** → Move `LoggingMDCServletAutoConfiguration` to `gs-cloud-autoconfigure`
- [ ] **spring-boot starter** → Move `StartupLoggerAutoConfiguration` to `gs-cloud-autoconfigure`  
- [ ] **webmvc starter** → Move `GeoServerMainModuleAutoConfiguration` to `gs-cloud-autoconfigure`

**Result**: These become true starters (dependencies only, no Java source code)

#### **Keep Working Starters (No Changes)**
- ✅ **extensions starter** → Already correct (aggregates all extensions)
- ✅ **input-formats starter** → Already correct (aggregates input format extensions)
- ✅ **output-formats starter** → Already correct (aggregates output format extensions)  
- ✅ **security starter** → Already correct (aggregates security extensions)

**These already follow Spring Boot patterns correctly**

### **Phase 4: Stage 2 - Module Separation (Future)**

#### **Step 1: Create XML Configuration Module**
- [ ] Create `gs-cloud-xml-configuration` module (independent of all GeoServer Cloud modules)
- [ ] Add all vanilla GeoServer JARs as optional dependencies
- [ ] Create one `@Configuration` class per vanilla GeoServer JAR with `@TranspileXmlConfig`
- [ ] Configure annotation processor in this module only
- [ ] Generate all vanilla GeoServer beans as Java `@Configuration` classes

#### **Step 2: Create Configuration Properties Module**
- [ ] Create `gs-cloud-config-properties` module (lightweight, no dependencies)
- [ ] Move all 42 `@ConfigurationProperties` classes to hierarchical package structure
- [ ] Create master `GeoServerCloudProperties` class with full hierarchy
- [ ] Update all existing modules to depend on config-properties instead of local properties

#### **Step 3: Create AutoConfiguration Module**  
- [ ] Create `gs-cloud-autoconfigure` module
- [ ] Move all `@AutoConfiguration` classes from Stage 1
- [ ] Move all `@Configuration` classes from Stage 1  
- [ ] Remove all `@TranspileXmlConfig` classes (now in xml-configuration module)
- [ ] Update AutoConfigurations to import from xml-configuration module
- [ ] Create `AutoConfiguration.imports` file

#### **Step 3: Create True Starters**

##### **Keep Existing Feature-Based Starters (Already Correct) ✅**
These current starters **already follow Spring Boot patterns correctly** and should remain unchanged:
- ✅ `gs-cloud-starter-extensions` (aggregates all extensions)
- ✅ `gs-cloud-starter-input-formats` (aggregates all input format extensions)  
- ✅ `gs-cloud-starter-output-formats` (aggregates all output format extensions)
- ✅ `gs-cloud-starter-security` (aggregates all security extensions)

**Role**: **Horizontal composition** - users can mix these to build custom deployments

##### **Fix Problematic Starters (Remove Code) 🔧**
These starters violate Spring Boot patterns by containing code - move code to autoconfigure:
- 🔧 `gs-cloud-starter-observability` → Move AutoConfiguration classes to `gs-cloud-autoconfigure`
- 🔧 `gs-cloud-starter-spring-boot` → Move AutoConfiguration classes to `gs-cloud-autoconfigure`
- 🔧 `gs-cloud-starter-webmvc` → Move AutoConfiguration classes to `gs-cloud-autoconfigure`

**After cleanup**: Only dependencies remain, no Java source code

##### **Create New Service-Based Starters ⭐**
Add service-specific starters for different deployment scenarios:
- ⭐ `gs-cloud-starter-wms` (minimal WMS microservice)
- ⭐ `gs-cloud-starter-wfs` (minimal WFS microservice)
- ⭐ `gs-cloud-starter-wcs` (minimal WCS microservice)  
- ⭐ `gs-cloud-starter-webui` (full admin interface)
- ⭐ `gs-cloud-starter-all` (monolith - all services)

#### **Step 4: Migrate Applications**
- [ ] Update application modules to use starters instead of direct dependencies
- [ ] Remove all autoconfiguration code from application modules
- [ ] Test monolith and microservice deployment modes

## Success Criteria

### **Technical Compliance**
- [ ] ✅ Zero AutoConfiguration classes with @Bean methods
- [ ] ✅ Zero starters with Java source code  
- [ ] ✅ All @ConfigurationProperties in dedicated classes
- [ ] ✅ All XML transpilation in dedicated classes
- [ ] ✅ Follows Spring Boot module patterns exactly

### **Functional Preservation**
- [ ] ✅ Same application behavior before/after
- [ ] ✅ Same configuration properties supported
- [ ] ✅ Same conditional behavior
- [ ] ✅ No breaking changes for users

### **Developer Experience**
- [ ] ✅ Configuration is discoverable (IDE autocomplete)
- [ ] ✅ Easy to find where functionality is configured
- [ ] ✅ Easy to override specific beans
- [ ] ✅ Easy to add new services/extensions
- [ ] ✅ Clear separation of concerns

## Why This Matters

### **For Users**
- **Familiar patterns** - same experience as any Spring Boot application
- **Better documentation** - centralized configuration reference
- **Flexible deployment** - monolith or microservices with same codebase
- **IDE support** - full autocomplete for configuration

### **For Developers**
- **Easier maintenance** - clear separation of concerns
- **Better testing** - can test parts independently
- **Faster development** - know exactly where to make changes
- **Reduced bugs** - single responsibility reduces complexity

### **For the Project**
- **Industry standard** - follows Spring Boot best practices
- **Better adoption** - familiar to Spring Boot developers
- **Easier contributions** - clear patterns for new features
- **Future-proof** - ready for new Spring Boot versions

This migration transforms GeoServer Cloud from a **custom Spring configuration system** into a **true Spring Boot ecosystem** that follows all official patterns and provides the same developer experience as any other Spring Boot project.