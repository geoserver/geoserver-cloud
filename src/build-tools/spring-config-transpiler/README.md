# Spring XML Configuration Transpiler

A compile-time annotation processor that reads Spring XML bean definitions and generates
equivalent `@Configuration` classes with `@Bean` methods. The generated code eliminates
runtime XML parsing entirely — beans are wired through plain Java method calls.

## Why

GeoServer's upstream codebase defines hundreds of beans across dozens of `applicationContext.xml`
files bundled inside JARs. Parsing and processing all that XML at startup is slow. This processor
moves that work to compile time: it reads the XML once during the build, writes out Java source
files, and the application boots with no XML overhead at all.

## Usage

### 1. Add the dependency (compile-only)

```xml
<dependency>
    <groupId>org.geoserver.cloud.build</groupId>
    <artifactId>gs-spring-config-transpiler</artifactId>
    <version>${revision}</version>
    <optional>true</optional>
</dependency>
```

### 2. Configure the compiler plugin

The processor needs access to the classes referenced in the XML at compile time.
Add them to `annotationProcessorPaths`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.geoserver.cloud.build</groupId>
                <artifactId>gs-spring-config-transpiler</artifactId>
                <version>${revision}</version>
            </path>
            <path>
                <groupId>org.geoserver</groupId>
                <artifactId>gs-main</artifactId>
                <version>${gs.version}</version>
            </path>
            <!-- any other JARs whose classes appear in the XML -->
        </annotationProcessorPaths>
        <proc>full</proc>
    </configuration>
</plugin>
```

### 3. Annotate a configuration class

```java
@Configuration
@TranspileXmlConfig("classpath:applicationContext.xml")
@Import(MyConfiguration_Generated.class)
public class MyConfiguration {
}
```

The processor generates `MyConfiguration_Generated` at compile time. The `@Import`
brings its beans into the application context.

## `@TranspileXmlConfig` reference

| Attribute            | Type       | Default                   | Description |
|----------------------|------------|---------------------------|-------------|
| `value` / `locations`| `String[]` | —                         | XML resource locations (`classpath:`, `jar:gs-main-.*!/...`, `file:`) |
| `targetPackage`      | `String`   | annotated class's package | Package for the generated class |
| `targetClass`        | `String`   | `<ClassName>_Generated`   | Name of the generated class |
| `includes`           | `String[]` | `{".*"}`                  | Regex patterns — only matching bean names are transpiled |
| `excludes`           | `String[]` | `{}`                      | Regex patterns — matching beans are skipped (takes precedence over includes) |
| `publicAccess`       | `boolean`  | `false`                   | Generate `public` class and methods (needed for cross-package `@Import`) |
| `proxyBeanMethods`   | `boolean`  | `false`                   | Controls `@Configuration(proxyBeanMethods=...)` on the generated class |
| `componentScanStrategy`| `ComponentScanStrategy` | `INCLUDE`          | How to handle `<context:component-scan>` elements: `INCLUDE` (generate `@ComponentScan` annotations), `IGNORE` (skip entirely), or `GENERATE` (build-time classpath scan producing `@Bean` methods) |

The annotation is `@Repeatable` — you can apply it more than once on the same class
to process multiple XML files into separate generated classes.

## What gets generated

### Bean definitions

The processor handles the bean patterns that appear in GeoServer's XML configs:

- Simple beans (with or without an explicit `id`)
- Constructor injection (`<constructor-arg>` — indexed, by value, by ref, with lists/arrays)
- Property injection (`<property>` — values, refs, nested `<bean>`, `<props>`, `<map>`)
- Static factory methods (`factory-method` on the bean class)
- Instance factory methods (`factory-bean` + `factory-method`) — stubbed, not yet implemented
- Abstract parent beans (`abstract="true"` / `parent="..."`) — the child bean's generated method
  merges constructor args and properties from the parent
- `ProxyFactoryBean` — translated to a `@Bean` method that builds the proxy
- `MethodInvokingFactoryBean` — translated as-is (the factory bean itself becomes a `@Bean`)
- Bean scopes, `lazy-init`, `depends-on`
- `@SuppressWarnings` added automatically when managed collections require unchecked casts
- Non-public constructors handled via reflection-based instantiation

### Type resolution

Parameter and return types are resolved through a combination of:

- The annotation processing API (`Elements` / `TypeMirror`) when running inside `javac`
- Runtime reflection (`Class.forName`) as a fallback — works in both the processor and unit tests
- Constructor/setter signature inspection to infer dependency types

Inner class return types (e.g. `ArcGridPPIOFactory.ArcGridPPIO`) are handled correctly via
reflection probing with `$` separators.

### Component scanning

`<context:component-scan>` elements are handled according to the `componentScanStrategy` setting:

- **`INCLUDE`** (default): Generates `@ComponentScan` annotations on the generated class,
  preserving `base-package`, `use-default-filters`, and `resource-pattern`.
- **`IGNORE`**: Skips component-scan elements entirely — no annotations or beans are generated.
- **`GENERATE`**: Performs classpath scanning at build time using Spring's
  `ClassPathScanningCandidateComponentProvider` and generates `@Bean` methods for each
  discovered component in a static inner `@Configuration` class named `ComponentScannedBeans`.
  Abstract classes and interfaces are skipped. The `excludes` patterns apply to component-scanned
  beans, matching against both the fully qualified class name and the default bean name.

### Bean filtering and aliases

Both bean names and aliases are checked against include/exclude patterns. Excluding a bean also
excludes its aliases.

### SpEL expressions

Expressions like `#{systemProperties['PROP'] ?: default}` are turned into `@Value`-annotated
method parameters. The generated method receives the resolved value from Spring and passes it
to the constructor.

### Javadoc

Each generated `@Bean` method includes a Javadoc comment with the original XML snippet, so you
can trace the generated code back to its source.

## Bean method generator pipeline

The processor selects a generator for each bean definition based on priority (lowest wins):

| Priority | Generator                               | Handles |
|----------|-----------------------------------------|---------|
| 40       | `ProxyFactoryBeanMethodGenerator`       | Beans whose class is `ProxyFactoryBean` |
| 50       | `AbstractBeanInheritanceMethodGenerator`| Beans with a `parent` attribute |
| 50       | `ConstructorBasedBeanMethodGenerator`   | Beans with constructor args (but no `factory-method`) |
| 60       | `FactoryMethodBeanMethodGenerator`      | Beans with a `factory-method` attribute |
| 200      | `SimpleBeanMethodGenerator`             | Everything else (no-arg constructor + property setters) |

`ConfigurationClassGenerator` orchestrates the overall class generation: it loads the XML,
runs filtering, delegates each bean to the matching generator, and writes the `.java` file.

## Limitations

**Not yet implemented:**
- Instance factory methods (`factory-bean` + `factory-method`) — the generator exists but throws
  `UnsupportedOperationException`
- `ProxyFactoryBean` target bean injection (target is set inline, not as a method parameter)
- Custom namespace handlers beyond `context:component-scan`
- Custom `PropertyEditor` implementations

**By design:**
- Runtime-created bean definitions can't be transpiled
- `BeanPostProcessor` side effects are not replicated
- Complex nested collection structures may not round-trip perfectly

## Tests

83 tests across two test classes:

- `TranspileXmlConfigAnnotationProcessorTest` (24 tests) — end-to-end compilation tests:
  resource resolution, filtering, component scan strategies (INCLUDE, IGNORE, GENERATE),
  multiple annotations, public access, error handling.
- `TranspileXmlConfigAnnotationProcessorMethodGenerationTest` (59 tests) — per-bean-pattern
  tests using AST-based structural comparison. Each test specifies source XML and the expected
  generated Java.

Main test categories:
- Basic bean patterns (simple beans, inner classes, auto-generated names)
- Constructor injection (values, references, indexed arguments, lists, arrays)
- Property injection (values, references, nested beans, managed collections)
- Bean annotations (lazy, scope, depends-on, suppress warnings)
- Dependency resolution (qualifiers, type inference, implicit autowiring)
- Static factory methods (no-arg and with constructor-arg references)
- Abstract bean inheritance (parent merging, child-only class, non-indexed args)
- ProxyFactoryBean (proxy interfaces, interceptor names)
- MethodInvokingFactoryBean (static and instance methods)
- SpEL expressions (system properties, elvis operator)
- Component scan strategies (INCLUDE, IGNORE, GENERATE with excludes)
- Method name collision prevention (unique suffixes for auto-generated beans)

Run them with:

```bash
mvn clean verify -f src/config/spring-config-transpiler/
```

## Troubleshooting

**"Resource not found"** — the XML file must be on the annotation processor classpath
(i.e. in `annotationProcessorPaths`), not just in regular `dependencies`.

**Type resolution failures** — the processor uses reflection, so the classes referenced in the
XML need to be loadable at compile time. Add the relevant JARs to `annotationProcessorPaths`.

**Verbose output** — add `-Xlint:processing` to `<compilerArgs>` to see diagnostic messages
about resource resolution, type inference, and bean filtering decisions.
