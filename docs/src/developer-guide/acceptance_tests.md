## Acceptance tests

### Overview

The acceptance tests are an important part of GeoServer Cloud's quality assurance process. They serve two primary purposes:

- **Prevent regressions**: Ensure that new changes don't break existing functionality
- **Maintain upstream compatibility**: Verify that GeoServer Cloud maintains consistent behavior with upstream GeoServer

These tests validate the full system behavior through end-to-end scenarios, testing the actual REST API, OGC services and catalog operations against running GeoServer Cloud instances.

### Test suite architecture

The acceptance tests use a remote test suite maintained in the [camptocamp/python-geoservercloud](https://github.com/camptocamp/python-geoservercloud) repository. This approach allows:

- **Shared test suite**: The same tests can be used across different GeoServer implementations
- **Centralized maintenance**: Test improvements benefit all projects using the suite
- **Version-controlled testing**: Tests evolve alongside the Python client library

The test suite is written in Python using [Pytest](https://pytest.org/) and runs against dedicated Docker Compose environments configured with different catalog backends:

- **datadir backend**: Traditional file-based catalog storage
- **pgconfig backend**: PostgreSQL-based catalog storage

### Running acceptance tests locally

#### Quick start with make

The simplest way to run the acceptance tests is using the provided `make` targets:

```bash
# Run tests with datadir backend
make acceptance-tests-datadir

# Run tests with pgconfig backend  
make acceptance-tests-pgconfig
```

These commands will:
1. Build the acceptance test Docker image
2. Start the required GeoServer Cloud services and a local DB
3. Execute the full test suite
4. Report the results

#### Manual execution

For more control over test execution, you can run tests manually. This approach is useful when developing or debugging specific tests.

##### Running tests in Docker container

This is the recommended approach for running the full test suite:

```bash
# Start GeoServer services  
cd compose
./acceptance_datadir up -d  # or ./acceptance_pgconfig up -d

# Run all tests inside the container
./acceptance_datadir exec acceptance pytest --pyargs geoserver_acceptance_tests.tests -v --color=yes

# Run specific test modules
./acceptance_datadir exec acceptance pytest --pyargs geoserver_acceptance_tests.tests.test_cog -v --color=yes
./acceptance_datadir exec acceptance pytest --pyargs geoserver_acceptance_tests.tests.test_workspace -v --color=yes
```

##### Running tests from host machine

In you need to debug the tests, you can run tests directly from your host machine. First, ensure you have Python 3.8+ installed and the acceptance test dependencies:

```bash
cd acceptance_tests

# Create and activate virtual environment
python -m venv .venv
source .venv/bin/activate  # On Linux/macOS
# .venv\Scripts\activate   # On Windows

# Install dependencies
pip install -r requirements.txt
```

Start the Docker composition and run the tests with the provided bash scripts:

```bash
# Start GeoServer services (geodatabase port 6432 will be exposed)
cd compose  
./acceptance_datadir up -d  # or ./acceptance_pgconfig up -d

# Run tests from acceptance_tests directory
cd ../acceptance_tests
./run_tests_locally.sh                      # Run all tests
./run_tests_locally.sh test_cog             # Run COG tests
./run_tests_locally.sh test_workspace       # Run workspace tests

# Run specific test functions
./run_tests_locally.sh test_cog::test_create_cog_coverage
```

The `run_tests_locally.sh` script handles:
- Virtual environment creation and activation
- Dependency installation
- Environment variable configuration (database ports, URLs, feature flags)
- Pytest invocation with appropriate parameters

See the [acceptance_tests/README.md](https://github.com/geoserver/geoserver-cloud/blob/main/acceptance_tests/README.md) for more detailed options and configuration.

### Using a local version of the test suite

During development, you may need to modify the test suite itself to add new tests or fix existing ones. Here's how to work with a local version:

#### Setup local development environment

1. Clone the test suite repository alongside GeoServer Cloud:

```bash
cd ..
git clone git@github.com:camptocamp/python-geoservercloud.git
```

2. Modify the `acceptance_tests/requirements.txt` to use your local version:

```python
# Comment out the published version
# geoservercloud==0.8.4.dev17

# Add the local editable installation
-e file:../../python-geoservercloud
```

3. Reinstall dependencies to use your local version:

```bash
cd acceptance_tests
source .venv/bin/activate
pip install -r requirements.txt
```

#### Implementing or fixing tests

When working on the test suite:

1. **Make changes** in your local `python-geoservercloud` repository
2. **Test locally** using the steps above - changes are immediately reflected due to the editable installation
3. **Commit and push** your changes to a feature branch in the python-geoservercloud repository
4. **Create a Pull Request** in the python-geoservercloud repository
5. **Wait for merge** - once merged, a new development version (e.g., `0.8.4.dev18`) is automatically published to [PyPI](https://pypi.org/) - check the latest version on the [project page](https://pypi.org/project/geoservercloud/#history)
6. **Update requirements.txt** in geoserver-cloud to use the new version:

```python
geoservercloud==0.8.4.dev18  # Update to the new dev version
```

To ensure changes and new tests are compatible with upstream GeoServer, the acceptance test suite is run against a nightly docker image of GeoServer as part of the PR checks in python-geoservercloud.

#### Development workflow best practices

- Always contribute test improvements back to the upstream repository
- Test against both `datadir` and `pgconfig` backends when possible
- Follow the existing test patterns and naming conventions in the suite
- Use environment variables and Pytest marks for GeoServer Cloud-specific tests (see the [README for python-geoservercloud acceptance tests](https://github.com/camptocamp/python-geoservercloud/blob/master/geoserver_acceptance_tests/README.md#enabling-and-disabling-tests))
- Update the development version number in GeoServer Cloud's `requirements.txt` as soon as the PR is merged
- When adding new acceptance tests, in order to make sure they are run in the CI/CD of [upstream GeoServer](https://github.com/geoserver/geoserver/tree/main/build/acceptance), upgrade the test suite version there as well.

### Configuration

The acceptance tests are configured through:

- **config.yaml**: Main configuration file in `acceptance_tests/` directory
- **Environment variables**: Override configuration values for local testing (see `run_tests_locally.sh`)
- **Docker Compose**: Service configuration in `compose/acceptance.yml`

Key configuration options include:

- GeoServer URL and credentials
- Database connection parameters
- Feature flags for enabling/disabling specific test categories (COG, JNDI, AWS S3, etc.)
- Test markers for slow or resource-intensive tests

### Troubleshooting

If tests fail or services don't start properly:

1. **Check service logs**:
```bash
cd compose
./acceptance_datadir logs -f
```

2. **Verify services are healthy**:
```bash
./acceptance_datadir ps
```

3. **Clean and restart**:
```bash
./acceptance_datadir down -v --remove-orphans
./acceptance_datadir up -d
```

4. **Enable debug ports** for step-through debugging:
```bash
./acceptance_datadir -f localports.yml up -d
```

### Continuous integration

The acceptance tests run automatically in the CI/CD pipeline on pull requests to the `main` or `release/**` branches.

Both `datadir` and `pgconfig` backends are tested to ensure compatibility across all catalog storage implementations.
