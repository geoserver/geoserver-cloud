# GeoServer Cloud Build Tools

This module contains centralized build configurations for the GeoServer Cloud project.

## Checkstyle

The build-tools module contains Checkstyle configuration that enforces consistent code style across the project. This approach follows the pattern used by many major Java projects including Apache Commons, Spring Framework, and Hibernate.

### Key Features

- **Centralized Configuration**: All code style rules are defined in a single place
- **Enforced File Headers**: Ensures all Java files have the proper license header
- **Import Control**: Forbids wildcard imports (e.g., `import java.util.*`) to improve code readability
- **Consistent Code Style**: Enforces naming conventions, whitespace rules, and other code style guidelines

### Configuration Files

- `checkstyle.xml`: Main Checkstyle configuration file
- `suppressions.xml`: Contains rules to suppress certain Checkstyle checks for specific files or patterns

## Usage

The build-tools module is referenced by the Maven Checkstyle plugin in the parent POM. The validation can be run with:

```bash
# Run checkstyle as part of the QA process
mvn validate -Dqa

# Run only checkstyle
mvn validate -Dqa -Dspotless.skip=true -Dsortpom.skip=true
```

## Benefits

1. **Consistency**: Ensures consistent code style across the entire project
2. **Quality**: Helps catch common programming issues early
3. **License Compliance**: Enforces proper license headers
4. **Maintainability**: By prohibiting wildcard imports, it makes dependencies clearer