#!/bin/bash
set -e

# Documentation build script for GeoServer Cloud
# Usage: ./build.sh [site_url] [banner_message]

SITE_URL="$1"
BANNER_MESSAGE="$2"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${SCRIPT_DIR}/.venv"

echo "🏗️  Building GeoServer Cloud Documentation"
echo "=========================================="

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check required dependencies
echo "🔍 Checking dependencies..."

if ! command_exists python3; then
    echo "❌ Python 3 is required but not installed"
    exit 1
fi

if ! command_exists docker; then
    echo "❌ Docker is required for diagram generation but not installed"
    exit 1
fi

echo "✅ All required dependencies found"

# Create and activate virtual environment
echo ""
echo "🐍 Setting up Python virtual environment..."

if [ ! -d "$VENV_DIR" ]; then
    echo "Creating new virtual environment..."
    python3 -m venv "$VENV_DIR"
else
    echo "Using existing virtual environment..."
fi

# Activate virtual environment
echo "Activating virtual environment: $VENV_DIR"
source "$VENV_DIR/bin/activate"

# Verify we're in the virtual environment
if [[ "$VIRTUAL_ENV" != "$VENV_DIR" ]]; then
    echo "❌ Failed to activate virtual environment"
    exit 1
fi

echo "✅ Virtual environment activated: $VIRTUAL_ENV"

# Upgrade pip and install dependencies
echo "📦 Installing Python dependencies..."
python -m pip install --upgrade pip
pip install -r requirements.txt

echo "✅ Python environment ready"

# Generate C4 model diagrams
echo ""
echo "📊 Generating C4 model diagrams..."

cd structurizr

# Generate diagrams
echo "🔄 Running Structurizr diagram generation..."
./structurizr-generate-diagrams.sh

echo "✅ Diagram generation completed"

# Return to docs directory
cd "$SCRIPT_DIR"

# Determine config file
CONFIG_FILE="mkdocs.yml"

if [ -n "$SITE_URL" ]; then
    echo ""
    echo "🔧 Configuring for preview..."
    echo "   URL: $SITE_URL"
    if [ -n "$BANNER_MESSAGE" ]; then
        echo "   Banner: $BANNER_MESSAGE"
    fi
    
    # Export variables for mkdocs-env-config-plugin
    export SITE_URL
    export BANNER_MESSAGE
fi

# Validate MkDocs configuration
echo ""
echo "🔧 Validating MkDocs configuration..."
mkdocs --version

# Build documentation
echo ""
echo "📚 Building documentation..."
mkdocs build --verbose --strict

# Disable Jekyll processing on GitHub Pages
echo ""
echo "🚫 Disabling Jekyll processing..."
touch site/.nojekyll
echo "✅ Created .nojekyll file"

# Add preview metadata if preview build
if [ -n "$BANNER_MESSAGE" ]; then
    echo "<!-- Preview build -->" >> site/index.html
    echo "<!-- Generated: $(date) -->" >> site/index.html
fi

echo ""
echo "✅ Documentation build completed successfully!"
echo ""
echo "📁 Built site location: ${SCRIPT_DIR}/site/"
echo "🌐 To serve locally: mkdocs serve"
echo "🔄 To clean build: rm -rf site/ && ./build.sh"
echo ""
echo "💡 Remember to activate the virtual environment for future runs:"
echo "   source .venv/bin/activate"
