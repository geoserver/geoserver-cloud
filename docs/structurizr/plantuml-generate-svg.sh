#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPORT_DIR="${SCRIPT_DIR}/exports"
OUTPUT_DIR="${SCRIPT_DIR}/../src/assets/images/structurizr"

# Create the output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Pull PlantUML Docker image
echo "Pulling PlantUML Docker image..."
docker pull plantuml/plantuml:latest

# Loop through all PlantUML files and convert them to SVG
for puml_file in "$EXPORT_DIR"/*.puml; do
  if [ -f "$puml_file" ]; then
    base_name=$(basename "$puml_file" .puml)
    
    echo "Converting $(basename "$puml_file") to SVG..."
    
    # Use PlantUML Docker image to convert PUML to SVG with current user permissions
    docker run --rm --user "$(id -u):$(id -g)" -v "${EXPORT_DIR}:/work" plantuml/plantuml -tsvg "/work/$(basename "$puml_file")"
    
    # Move the generated SVG to the output directory
    if [ -f "${EXPORT_DIR}/${base_name}.svg" ]; then
      # Remove existing file first to avoid permission issues
      rm -f "${OUTPUT_DIR}/${base_name}.svg"
      # Move the file (now has correct permissions)
      mv "${EXPORT_DIR}/${base_name}.svg" "${OUTPUT_DIR}/${base_name}.svg"
      echo "✓ Generated ${base_name}.svg"
    else
      echo "⚠ Failed to generate ${base_name}.svg"
    fi
  fi
done

echo "Conversion complete. SVG files are in: $OUTPUT_DIR"

# List the generated files
echo "Generated files:"
ls -la "$OUTPUT_DIR"/*.svg 2>/dev/null || echo "No SVG files found"
