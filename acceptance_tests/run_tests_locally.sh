#!/bin/bash

virtualenv -q .venv/
source .venv/bin/activate
pip install -q -r requirements.txt

# Default GeoServer URL
GEOSERVER_URL=${GEOSERVER_URL:-"http://localhost:9090/geoserver/cloud"}

# Default database connection for local testing (requires geodatabase port exposed on 6432)
GEOSERVER_PG_HOST_DOCKER=${GEOSERVER_PG_HOST_DOCKER:-"geodatabase"}
GEOSERVER_PG_PORT_DOCKER=${GEOSERVER_PG_PORT_DOCKER:-"5432"}
GEOSERVER_PG_HOST_LOCAL=${GEOSERVER_PG_HOST_LOCAL:-"localhost"}
GEOSERVER_PG_PORT_LOCAL=${GEOSERVER_PG_PORT_LOCAL:-"6432"}

# Acceptance test configuration
GEOSERVER_ACCEPTANCE_CONFIG=${GEOSERVER_ACCEPTANCE_CONFIG:-"config.yaml"}
# Flags to activate geoserver-cloud specific tests
GEOSERVER_ACCEPTANCE_RUN_DB_TESTS=${GEOSERVER_ACCEPTANCE_RUN_DB_TESTS:-"true"}
GEOSERVER_ACCEPTANCE_RUN_COG_TESTS=${GEOSERVER_ACCEPTANCE_RUN_COG_TESTS:-"true"}
GEOSERVER_ACCEPTANCE_RUN_JNDI_TESTS=${GEOSERVER_ACCEPTANCE_RUN_JNDI_TESTS:-"true"}
GEOSERVER_ACCEPTANCE_RUN_SLOW_TESTS=${GEOSERVER_ACCEPTANCE_RUN_SLOW_TESTS:-"false"}
GEOSERVER_ACCEPTANCE_RUN_AWS_S3_TESTS=${GEOSERVER_ACCEPTANCE_RUN_AWS_S3_TESTS:-"true"}

# Help function
show_help() {
    echo "Usage: $0 [OPTIONS] [TEST_PATH]"
    echo ""
    echo "Run GeoServer Cloud acceptance tests locally"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help     Show this help message"
    echo "  -v, --verbose  Run with verbose output (-vvv)"
    echo "  -q, --quiet    Run with minimal output (-q)"
    echo ""
    echo "TEST_PATH:"
    echo "  Optional path to specific test file or test function"
    echo "  Examples:"
    echo "    $0 test_cog"
    echo "    $0 test_cog_imagemosaic::test_create_imagemosaic_cogs_http"
    echo "    $0 test_workspace"
    echo ""
    echo "Environment variables (see script for full list):"
    echo "  GEOSERVER_URL                   GeoServer URL (default: http://localhost:9090/geoserver/cloud)"
    echo "  GEOSERVER_PG_HOST_LOCAL         Database host (default: localhost)"
    echo "  GEOSERVER_PG_PORT_LOCAL         Database port (default: 6432)"
    echo ""
    echo "Examples:"
    echo "  $0                           # Run all tests"
    echo "  $0 test_cog                  # Run COG tests only"
    echo "  $0 -v                        # Run all tests with verbose output"
    echo "  GEOSERVER_URL=http://localhost:8080/geoserver $0  # Use different URL"
}

# Parse command line arguments
VERBOSE_FLAG="-v"
TEST_PATH=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE_FLAG="-vvv"
            shift
            ;;
        -q|--quiet)
            VERBOSE_FLAG="-q"
            shift
            ;;
        -*)
            echo "Unknown option: $1" >&2
            show_help >&2
            exit 1
            ;;
        *)
            TEST_PATH="$1"
            shift
            ;;
    esac
done

# Set the target (all tests or specific test)
if [[ -n "$TEST_PATH" ]]; then
    TARGET="geoserver_acceptance_tests.tests.$TEST_PATH"
else
    TARGET="geoserver_acceptance_tests.tests"
fi

echo "Running GeoServer Cloud acceptance tests..."
echo "GeoServer URL: $GEOSERVER_URL"
echo "Test target: $TARGET"
echo "Verbosity: $VERBOSE_FLAG"
echo ""

# Wait for GeoServer to be available
echo "Waiting for GeoServer to be available at $GEOSERVER_URL/rest/workspaces..."
max_attempts=60
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s -u admin:geoserver --fail "$GEOSERVER_URL/rest/workspaces" > /dev/null 2>&1; then
        echo "✓ GeoServer is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo "  Attempt $attempt/$max_attempts - waiting 5 seconds..."
    sleep 5
done

if [ $attempt -eq $max_attempts ]; then
    echo "✗ Timeout: GeoServer did not become available at $GEOSERVER_URL/rest/workspaces"
    echo "  Please ensure the services are running with: cd ../compose && ./acceptance_datadir up -d"
    exit 1
fi

echo ""

# Check if virtual environment is activated
if [[ -n "$VIRTUAL_ENV" ]]; then
    echo "Using activated virtual environment..."
    # Check if pytest is available
    if ! command -v pytest &> /dev/null; then
        echo "Error: pytest not found in virtual environment"
        echo "Please install dependencies: pip install -e ."
        exit 1
    fi
    export GEOSERVER_URL GEOSERVER_PG_HOST_DOCKER GEOSERVER_PG_PORT_DOCKER GEOSERVER_PG_HOST_LOCAL GEOSERVER_PG_PORT_LOCAL GEOSERVER_ACCEPTANCE_CONFIG GEOSERVER_ACCEPTANCE_RUN_COG_TESTS GEOSERVER_ACCEPTANCE_RUN_DB_TESTS GEOSERVER_ACCEPTANCE_RUN_JNDI_TESTS GEOSERVER_ACCEPTANCE_RUN_SLOW_TESTS GEOSERVER_ACCEPTANCE_RUN_AWS_S3_TESTS
    pytest $VERBOSE_FLAG --color=yes --pyargs $TARGET
else
    echo "Error: Please create and activate a virtual environment:"
    echo "     python -m venv .venv"
    echo "     source .venv/bin/activate  # Linux/macOS"
    echo "     pip install -e ."
    echo "     ./run_tests_locally.sh"
    echo ""
    exit 1
fi
