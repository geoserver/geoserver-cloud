#!/bin/bash

# Default GeoServer URL
GEOSERVER_URL=${GEOSERVER_URL:-"http://localhost:9090/geoserver/cloud"}

# Default database connection for local testing (requires geodatabase port exposed on 5433)
PGHOST=${PGHOST:-"localhost"}
PGPORT=${PGPORT:-"5433"}
PGDATABASE=${PGDATABASE:-"geodata"}
PGUSER=${PGUSER:-"geodata"}
PGPASSWORD=${PGPASSWORD:-"geodata"}
PGSCHEMA=${PGSCHEMA:-"test1"}

# Note: This script runs tests from the host machine and requires the geodatabase 
# port to be exposed (5433). Start services with: ./acceptance_datadir up -d

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
    echo "    $0 tests/test_cog.py"
    echo "    $0 tests/test_cog_imagemosaic.py::test_create_imagemosaic_cogs_http"
    echo "    $0 tests/test_workspace.py"
    echo ""
    echo "Environment variables:"
    echo "  GEOSERVER_URL  GeoServer URL (default: http://localhost:9090/geoserver/cloud)"
    echo "  PGHOST         Database host (default: localhost)"
    echo "  PGPORT         Database port (default: 5433)"
    echo ""
    echo "Examples:"
    echo "  $0                           # Run all tests"
    echo "  $0 tests/test_cog.py        # Run COG tests only"
    echo "  $0 -v                       # Run all tests with verbose output"
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
    TARGET="$TEST_PATH"
else
    TARGET="."
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

# Check if we're using Poetry or virtual environment
if command -v poetry &> /dev/null && [[ -f "pyproject.toml" ]]; then
    echo "Using Poetry to run tests..."
    echo "Installing dependencies if needed..."
    poetry install
    export GEOSERVER_URL PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD PGSCHEMA
    poetry run pytest $VERBOSE_FLAG --color=yes $TARGET
elif [[ -n "$VIRTUAL_ENV" ]]; then
    echo "Using activated virtual environment..."
    # Check if pytest is available
    if ! command -v pytest &> /dev/null; then
        echo "Error: pytest not found in virtual environment"
        echo "Please install dependencies: pip install -e ."
        exit 1
    fi
    export GEOSERVER_URL PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD PGSCHEMA
    pytest $VERBOSE_FLAG --color=yes $TARGET
else
    echo "Error: Please either:"
    echo "  1. Install Poetry (https://python-poetry.org/docs/#installing-with-the-official-installer) and run this script again, or"
    echo "  2. Create and activate a virtual environment:"
    echo "     python -m venv .venv"
    echo "     source .venv/bin/activate  # Linux/macOS"
    echo "     pip install -e ."
    echo "     ./run_tests_locally.sh"
    echo ""
    exit 1
fi
