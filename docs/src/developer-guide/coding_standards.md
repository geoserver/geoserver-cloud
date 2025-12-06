# Coding Standards and Style Guidelines

GeoServer Cloud follows a set of coding standards to ensure code consistency and quality across the project. This document outlines the key standards and how they are enforced.

## Introduction

Code style consistency is enforced using automated tools during the build process. The project uses a build-tools module approach, similar to other major Java projects like Apache Commons, Spring Framework, and Hibernate, to centralize style configuration.

## Checkstyle Configuration

Checkstyle is used to enforce consistent code style and formatting standards. The configuration is located in the `build-tools` module.

### Key Style Rules

- **File Headers**: All Java files must include the proper license header
- **Import Control**: Wildcard imports (e.g., `import java.util.*`) are forbidden to improve code readability
- **Naming Conventions**: Standard Java naming conventions are enforced
- **Whitespace**: Consistent tab and space usage is required
- **Line Length**: Lines should not exceed 120 characters
- **Coding Practices**: Various best practices are enforced, such as:
  - Proper bracing
  - Avoiding empty statements
  - Implementing both `equals()` and `hashCode()`
  - Simplifying boolean expressions

### Configuration Files

The Checkstyle configuration is stored in the following files:

- `build-tools/src/main/resources/checkstyle/checkstyle.xml`: Main configuration
- `build-tools/src/main/resources/checkstyle/suppressions.xml`: Rules for suppressing certain checks

## Code Formatting

In addition to Checkstyle, the project uses:

- **Spotless**: For consistent Java code formatting using Palantir Java Format
- **SortPOM**: For consistent XML formatting in pom.xml files

## Running Style Checks

Style checks are run as part of the build process. You can trigger them manually with:

```bash
# Run all checks
make lint
```

or using maven directly:

```bash
# Run all checks
mvn validate -Dqa -fae -ntp -T1C

# Run only Java formatting checks
mvn validate -Dqa -fae -Dsortpom.skip=true -ntp -T1C

# Run only POM checks
mvn validate -Dqa -fae -Dspotless.skip=true -Dcheckstyle.skip=true -ntp -T1C
```

## Fixing Style Issues

To automatically fix style issues:

```bash
# Format all files
make format

# Format only Java files
make format-java

# Format only POM files
make format-pom
```

## Integration with IDE

Most common IDEs can be configured to follow these style guidelines:

### IntelliJ IDEA

- Install the Checkstyle-IDEA plugin
- Configure it to use the project's Checkstyle configuration

### Eclipse

- Install the Checkstyle plugin
- Configure it to use the project's Checkstyle configuration

### VS Code

- Install the Checkstyle for Java extension
- Configure it to use the project's Checkstyle configuration