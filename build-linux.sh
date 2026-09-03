#!/usr/bin/env bash
# PDFchemy Linux Package Build Script (DEB & RPM)
set -e

echo "==================================================="
echo "Building PDFchemy Linux Packages (.deb and .rpm)"
echo "==================================================="

./gradlew :desktop:packageDeb :desktop:packageRpm

echo ""
echo "==================================================="
echo "SUCCESS! Linux packages generated at:"
echo "  DEB: desktop/build/compose/binaries/main/deb/"
echo "  RPM: desktop/build/compose/binaries/main/rpm/"
echo "==================================================="
