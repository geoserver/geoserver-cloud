# GeoServer Cloud Documentation

This directory contains the complete documentation for the GeoServer Cloud project, built with MkDocs Material theme and featuring C4 model architectural diagrams.

## Quick Start

### Build Documentation

```bash
# One-time setup and build
./build.sh
```

### Development Server

```bash
# Start development server with auto-reload
./serve.sh
```

### Clean Build

```bash
# Remove all generated files and rebuild
./clean.sh && ./build.sh
```

## Documentation Structure

The documentation is organized into three main sections within the `src` directory:

```
docs/
├── build.sh              # Main build script (sets up venv, generates diagrams, builds docs)
├── serve.sh              # Development server script
├── clean.sh              # Clean build artifacts script
├── requirements.txt      # Python dependencies for MkDocs
├── mkdocs.yml            # MkDocs configuration
│
├── src/                  # Documentation source files (Markdown)
│   ├── index.md          # Homepage
│   ├── deploy/           # Deployment guides (Docker Compose, Helm)
│   ├── configuration/    # Configuration reference and guides
│   ├── developer-guide/  # Developer documentation and architecture
│   └── assets/           # Static assets (images, CSS)
│
├── structurizr/          # C4 model architectural diagrams definitions
│   ├── workspace.dsl     # Main C4 model definition
│   ├── dynamic-views.dsl # Dynamic view definitions
│   └── ...               # Diagram generation scripts
│
└── site/                 # Generated static documentation (after build)
```

## Build Process

The `build.sh` script handles the complete build process:

1. **Environment Setup**: Creates Python virtual environment and installs dependencies.
2. **Diagram Generation**: 
   - Runs Structurizr CLI to generate PlantUML from DSL files.
   - Converts PlantUML to SVG using Docker.
   - Copies SVGs to `src/assets/images/structurizr/` directory.
3. **Documentation Build**: Uses MkDocs to build the complete static site.
4. **Validation**: Ensures all links and references are valid.

## Requirements

- **Python 3.8+**: For MkDocs and dependencies.
- **Docker**: For C4 diagram generation (Structurizr CLI and PlantUML).
- **Internet connection**: For pulling Docker images during diagram generation.

## Contributing

When contributing to documentation:

1. Follow the existing structure and style.
2. Update both content and navigation in `mkdocs.yml` if adding new pages.
3. Test with `./serve.sh` locally before submitting.
4. Ensure all links work by running `./build.sh`.
5. If modifying architecture, update `structurizr/workspace.dsl` and/or `dynamic-views.dsl`.