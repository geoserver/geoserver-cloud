#!/bin/bash
set -e

# Clean build script for GeoServer Cloud Documentation
# This script removes all generated files and build artifacts

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🧹 Cleaning GeoServer Cloud Documentation"
echo "========================================="

# Remove built site
if [ -d "site" ]; then
    echo "🗑️  Removing built site directory..."
    rm -rf site/
    echo "✅ Site directory removed"
else
    echo "ℹ️  No site directory to remove"
fi

# Remove generated diagrams (preserving .gitkeep)
if [ -d "structurizr/exports" ]; then
    echo "🗑️  Removing generated diagram exports..."
    find structurizr/exports/ -name "*.puml" -delete 2>/dev/null || true
    echo "✅ Diagram exports removed"
else
    echo "ℹ️  No diagram exports to remove"
fi

# Remove SVG files from src/assets
if [ -d "src/assets/images/structurizr" ]; then
    echo "🗑️  Removing SVG diagrams from src/assets..."
    rm -f src/assets/images/structurizr/*.svg
    echo "✅ Src assets SVG files removed"
else
    echo "ℹ️  No src assets SVG files to remove"
fi

echo ""
echo "✅ Cleanup completed!"
echo ""
echo "💡 To rebuild everything: ./build.sh"
echo "🚀 To start development server: ./serve.sh"
