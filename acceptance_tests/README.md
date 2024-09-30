# GeoServer Cloud acceptance tests

## Requirements

[Poetry](https://python-poetry.org/docs/#installing-with-the-official-installer)

## Installation

```shell
poetry install
```

# Run the tests
First start the docker composition then run:

```shell
GEOSERVER_URL=http://localhost:9090/geoserver/cloud poetry run pytest -vvv .
```