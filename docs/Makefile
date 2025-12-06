.PHONY: help build serve serve-preview clean build-preview

# Show this help message
help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build           Build the documentation using mkdocs.yml"
	@echo "  serve           Start the development server"
	@echo "  serve-preview   Start the development server with a preview banner (BANNER_MESSAGE=...)"
	@echo "  clean           Remove build artifacts"
	@echo "  build-preview   Build with custom settings (SITE_URL=... BANNER_MESSAGE=...)"
	@echo "  help            Show this help message"

# Default build
build:
	./build.sh

# Start development server
serve:
	./serve.sh

# Start development server with preview banner
# Usage: make serve-preview BANNER_MESSAGE="Preview..."
serve-preview:
	./serve.sh "$(BANNER_MESSAGE)"

# Clean build artifacts
clean:
	./clean.sh

# Build preview with custom URL and banner
# Usage: make build-preview SITE_URL="https://..." BANNER_MESSAGE="Preview..."
build-preview:
	./build.sh "$(SITE_URL)" "$(BANNER_MESSAGE)"