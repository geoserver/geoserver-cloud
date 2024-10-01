#!/usr/bin/env python3
import time
import os
import requests
import sys

# Set variables
start_time = time.time()

# Set a maximum timeout from the environment or use 60 seconds as default
max_time = int(os.getenv('MAX_TIMEOUT', 60))

# Set the GEOSERVER_URL from the environment or use the default value
GEOSERVER_URL = os.getenv('GEOSERVER_URL', 'http://gateway:8080/geoserver/cloud')
GEOSERVER_USERNAME = os.getenv('GEOSERVER_USERNAME', 'admin')
GEOSERVER_PASSWORD = os.getenv('GEOSERVER_PASSWORD', 'geoserver')

# is we want to start directly with the passed command
IGNORE_HEALTH_CHECK = os.getenv('IGNORE_HEALTH_CHECK', False)

# Timeout function
def timeout():
    current_time = time.time()
    if current_time - start_time > max_time:
        return True
    return False

# Array of endpoints to check
endpoints = [
    f"{GEOSERVER_URL}/wms?SERVICE=WMS&REQUEST=GetCapabilities",
    f"{GEOSERVER_URL}/wfs?SERVICE=WFS&REQUEST=GetCapabilities",
    f"{GEOSERVER_URL}/wps?SERVICE=WPS&REQUEST=GetCapabilities",
    f"{GEOSERVER_URL}/wcs?SERVICE=WCS&REQUEST=GetCapabilities",
    f"{GEOSERVER_URL}/ows?SERVICE=WMS&REQUEST=GetCapabilities",
    f"{GEOSERVER_URL}/gwc",
    f"{GEOSERVER_URL}/rest",
]

if not IGNORE_HEALTH_CHECK:
    # Loop through each endpoint and check if it's available
    for endpoint in endpoints:
        print(f"Waiting for {endpoint} to be available...")
        if timeout():
            print("Timeout")
            break

        while True:
            try:
                # Make a request to the endpoint
                response = requests.get(endpoint, auth=(GEOSERVER_USERNAME, GEOSERVER_PASSWORD))
                if response.status_code == 200:
                    print(f"{endpoint} is up")
                    break
                else:
                    print(f"{endpoint} returned status code {response.status_code}")
            except requests.exceptions.RequestException as e:
                print(f"{endpoint} is not available - retrying...")

            if timeout():
                print("Timeout reached")
                break

            time.sleep(1)

# create /tmp/healthcheck file to signal that the healthcheck is done
with open("/tmp/healthcheck", "w") as f:
    f.write("done")
# Execute the command passed to the script anyway, this is useful for
# running the tests and see what breaks
if len(sys.argv) > 1:
    command = sys.argv[1:]
    os.execvp(command[0], command)
