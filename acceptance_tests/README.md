# GeoServer Cloud acceptance tests

## Requirements

- Python 3.8+

## Installation

```shell
# Create virtual environment
python -m venv .venv

# Activate virtual environment
# On Linux/macOS:
source .venv/bin/activate
# On Windows:
.venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

## Running the tests

### Option 1: Using make (runs full docker composition)

```shell
# Run tests with datadir backend
make acceptance-tests-datadir

# Run tests with pgconfig backend  
make acceptance-tests-pgconfig
```

### Option 2: Manual execution

#### Run tests inside Docker container (recommended for all tests)

The configuration for the tests will be read from the file `config.yaml`.

```shell
# Start GeoServer services  
cd ../compose
./acceptance_datadir up -d  # or ./acceptance_pgconfig up -d
./acceptance_datadir scale acceptance=1

# Optional: Start webui service if needed (not started by default in acceptance composition)
./acceptance_datadir scale webui=1

# Run all tests inside the container
./acceptance_datadir exec acceptance pytest --pyargs geoserver_acceptance_tests.tests -v --color=yes

# Run specific tests inside the container
./acceptance_datadir exec acceptance pytest --pyargs geoserver_acceptance_tests.tests.test_cog -v --color=yes
```

#### Run tests from host machine (full functionality)

The configuration for the tests will be read from the file `config.yaml` with some values being overridden with environment variables in the script `run_tests_locally.sh`.

**Note:** This requires the geodatabase port to be exposed (port 6432). The acceptance composition now exposes this port automatically.

```shell
# Start GeoServer services (geodatabase port 6432 will be exposed)
cd ../compose  
./acceptance_datadir up -d  # or ./acceptance_pgconfig up -d

# Optional: Start webui service if needed
./acceptance_datadir scale webui=1

# From acceptance_tests directory, run tests from host
cd ../acceptance_tests
./run_tests_locally.sh test_cog             # Run COG tests
./run_tests_locally.sh test_imagemosaic_cog # Run ImageMosaic tests
./run_tests_locally.sh test_workspace       # Run workspace tests
./run_tests_locally.sh                      # Run all tests

# Run specific test functions
./run_tests_locally.sh test_imagemosaic_cog::test_create_imagemosaic_local_files
./run_tests_locally.sh test_cog::test_create_cog_coverage
```

### Run specific tests with make

```shell
# To run specific tests with make, you can modify the Makefile or use the manual Docker approach above
```

## Debugging

If you need to debug the GeoServer services, you can run the acceptance test composition with local ports exposed:

```shell
cd ../compose

# Start the acceptance test compose with local ports
./acceptance_datadir -f localports.yml up -d

# Enable the webui service if needed
./acceptance_datadir -f localports.yml scale webui=1

# Shut down the rest service if you're going to launch it from your IDE
./acceptance_datadir -f localports.yml down rest

# Now you can run from the IDE with the `local` spring profile enabled 
# and the required catalog backend profile (datadir/pgconfig)
```

If you need to debug the Python tests, make a clone of the repository `github.com:camptocamp/python-geoservercloud`
and change the dependency location in the file `requirements.txt`.

### Accessing Sample Data

When debugging, you may need to access the sample data that's available in the containers. The sample data is extracted to `/opt/geoserver_data/sampledata` inside the containers. To access it from your local development environment:

```shell
# Check what sample data is available
./acceptance_datadir exec wms find /opt/geoserver_data/sampledata

# Copy sample data to your local machine for testing
docker cp $(./acceptance_datadir ps -q wms | head -1):/opt/geoserver_data/sampledata ./local_sampledata

# Or mount the geoserver_data volume directly to a local directory
# Add this to your docker-compose override file:
# volumes:
#   geoserver_data:
#     driver: local
#     driver_opts:
#       type: none
#       o: bind
#       device: /path/to/local/sampledata
```

## Testing Different GeoServer Cloud Versions

You can test different versions of GeoServer Cloud without modifying the `.env` file by setting the TAG environment variable.

### Option 1: Using Make Commands (Recommended)

```shell
# Test with GeoServer Cloud 2.27.2-SNAPSHOT (datadir backend)
TAG=2.27.2-SNAPSHOT make start-acceptance-tests-datadir
cd acceptance_tests && ./run_tests_locally.sh tests/test_imagemosaic.py
TAG=2.27.2-SNAPSHOT make clean-acceptance-tests-datadir

# Test with GeoServer Cloud 2.26.2.0 (pgconfig backend)
TAG=2.26.2.0 make start-acceptance-tests-pgconfig
cd acceptance_tests && ./run_tests_locally.sh tests/test_imagemosaic.py
TAG=2.26.2.0 make clean-acceptance-tests-pgconfig

# Test with default version (from Maven project.version)
make start-acceptance-tests-datadir
cd acceptance_tests && ./run_tests_locally.sh
make clean-acceptance-tests-datadir
```

### Option 2: Manual Docker Compose Commands

```shell
# Test with GeoServer Cloud 2.27.1.0
cd ../compose
TAG=2.27.1.0 ./acceptance_datadir up -d

# Run your tests
cd ../acceptance_tests
./run_tests_locally.sh

# Test with GeoServer Cloud 2.26.2.0
cd ../compose
./acceptance_datadir down
TAG=2.26.2.0 ./acceptance_datadir up -d

# Run your tests again
cd ../acceptance_tests
./run_tests_locally.sh

# Return to default version (check .env file for current default)
cd ../compose
./acceptance_datadir down
./acceptance_datadir up -d
```

## Cleanup

```shell
# Stop and remove containers
cd ../compose
./acceptance_datadir down -v  # or ./acceptance_pgconfig down -v
```
