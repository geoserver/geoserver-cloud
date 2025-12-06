#!/bin/bash
set -e

# Development server script for GeoServer Cloud Documentation
# Usage: ./serve.sh [banner_message]
# Example: ./serve.sh "🔍 Development Preview"

BANNER_MESSAGE="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${SCRIPT_DIR}/.venv"

echo "🌐 Starting GeoServer Cloud Documentation Server"
echo "================================================"

# Check if virtual environment exists
if [ ! -d "$VENV_DIR" ]; then
    echo "❌ Virtual environment not found. Please run ./build.sh first."
    exit 1
fi

# Activate virtual environment
echo "🐍 Activating virtual environment..."
source "$VENV_DIR/bin/activate"

# Check if MkDocs is available
if ! command -v mkdocs >/dev/null 2>&1; then
    echo "❌ MkDocs not found. Please run ./build.sh first."
    exit 1
fi

echo "✅ Environment ready"
echo ""

# Export banner message if provided
if [ -n "$BANNER_MESSAGE" ]; then
    echo "🏷️  Banner message: $BANNER_MESSAGE"
    export BANNER_MESSAGE
fi

echo "🚀 Starting development server..."
echo "📍 Local URL: http://127.0.0.1:8000"
echo "🌐 Network URL: http://$(hostname):8000"
echo "🔄 Auto-reload enabled for content changes"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start the development server (bind to all interfaces for network access)
mkdocs serve --dev-addr 0.0.0.0:8000
