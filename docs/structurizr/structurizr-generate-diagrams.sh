#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPORTS_DIR="${SCRIPT_DIR}/exports"

# Create the export directory if it doesn't exist
mkdir -p "$EXPORTS_DIR"

echo "Step 1: Pulling the latest Structurizr CLI Docker image"
docker pull structurizr/cli:latest

echo "Step 2: Generating PlantUML files from Structurizr DSL"

# Track if any exports succeeded
EXPORTS_SUCCEEDED=false

# Export workspace.dsl to PlantUML (required)
if [ -f "${SCRIPT_DIR}/workspace.dsl" ]; then
    echo "Exporting workspace.dsl to PlantUML..."
    if docker run --rm -v "${SCRIPT_DIR}:/usr/local/structurizr" structurizr/cli:latest export \
      -workspace /usr/local/structurizr/workspace.dsl \
      -format plantuml/c4plantuml \
      -output /usr/local/structurizr/exports; then
        echo "✅ Workspace views exported successfully"
        EXPORTS_SUCCEEDED=true
    else
        echo "❌ Workspace export failed"
        exit 1
    fi
else
    echo "❌ ERROR: workspace.dsl not found - this is required!"
    exit 1
fi

# Export dynamic-views.dsl to PlantUML (must be valid if present)
if [ -f "${SCRIPT_DIR}/dynamic-views.dsl" ]; then
    echo "Exporting dynamic-views.dsl to PlantUML..."
    if docker run --rm -v "${SCRIPT_DIR}:/usr/local/structurizr" structurizr/cli:latest export \
      -workspace /usr/local/structurizr/dynamic-views.dsl \
      -format plantuml/c4plantuml \
      -output /usr/local/structurizr/exports; then
        echo "✅ Dynamic views exported successfully"
    else
        echo "❌ Dynamic views export failed"
        exit 1
    fi
else
    echo "ℹ️  dynamic-views.dsl not found (skipping dynamic views)"
fi

# Verify we have at least some PlantUML files
if ! ls "${EXPORTS_DIR}"/*.puml 1> /dev/null 2>&1; then
    echo "❌ ERROR: No PlantUML files were generated!"
    exit 1
fi

echo "Step 3: Converting PlantUML to SVG"
./plantuml-generate-svg.sh

# Verify SVG files were created
SVG_OUTPUT_DIR="${SCRIPT_DIR}/../src/assets/images/structurizr"
if ! ls "${SVG_OUTPUT_DIR}"/*.svg 1> /dev/null 2>&1; then
    echo "❌ ERROR: No SVG files were generated!"
    exit 1
fi

echo "✅ Diagram generation completed successfully"

# List the generated files
echo "Generated files:"
echo "PlantUML files:"
find "$EXPORTS_DIR" -name "*.puml" | sort | while read -r file; do
  echo " - $(basename "$file")"
done
echo "SVG files:"
find "$SVG_OUTPUT_DIR" -name "*.svg" | sort | while read -r file; do
  file_size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null)
  echo " - $(basename "$file") (Size: $file_size bytes)"
done
