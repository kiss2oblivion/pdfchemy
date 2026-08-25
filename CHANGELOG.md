# Changelog

All notable changes to the **PDFchemy Tools (Shrink PDF)** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-08-25

### Added
- **Interactive PDF Editor Engine (`PdfEditor.kt`)**:
  - 100% on-device high-resolution PDF rendering via Android `PdfRenderer`.
  - Multi-page document navigation with real-time vector and annotation compositing using `PDFBox-Android`.
  - **Freehand Drawing & Signature Tool**: Smooth Bezier stroke capture with customizable stroke thickness and full RGBA color palette.
  - **Translucent Highlighter**: Fluorescent alpha-blended highlighting for text emphasis and document markup.
  - **Draggable Text Box Annotations**: Movable text overlays with dynamic typography styling, custom fonts, and colored bounding backgrounds.
  - **Business Stamp Suite**: Predefined enterprise stamps (*APPROVED*, *CONFIDENTIAL*, *DRAFT*, *PAID*, *REJECTED*, *FINAL*, *URGENT*) with angled borders and high-contrast typography.
  - **Structural Page Operations**: Dynamic 90°/180°/270° page rotation, selective page deletion, and non-destructive vector preservation during export.
- **Interactive PDF Editor UI (`PdfEditorScreen.kt`)**:
  - Fullscreen interactive page canvas with gesture detection and live visual feedback.
  - Floating bottom tool shelf with quick tool switcher (View, Pen, Highlight, Text, Stamp, Color Picker).
  - Page navigation controls with page count indicator and one-tap Undo action.
  - Direct PDF export with native Android storage integration (`CreateDocument`) and instant share sheets.
- **Automated Unit Test Suite (`PdfEditorTest.kt`)**:
  - Automated tests verifying multi-page annotation overlays, signature compositing, stamp rendering, page rotations, and page deletions.
- **Full 9-Language Localization**:
  - Native translations added across English (`en`), German (`de`), Spanish (`es`), French (`fr`), Indonesian (`in`), Italian (`it`), Portuguese - Portugal (`pt`), Portuguese - Brazil (`pt-rBR`), and Romanian (`ro`).

---

## [1.1.0] - 2026-08-25

### Added
- **Image Compressor Engine (`ImageCompressor.kt`)**:
  - High-performance, 100% on-device image compression supporting JPG, PNG, and WebP formats.
  - Target Size Mode: Adaptive binary search with progressive downscale fallback to compress photos exactly to 2MB, 5MB, 10MB, or custom user-defined limits (MB/KB).
  - Graphic Analysis & AI Estimator: Real-time calculation of estimated output byte size and perceived visual quality loss level (*Negligible*, *Minimal*, *Moderate*, *Significant*).
  - Format Guardrails & Validation: Automatic validation for allowed graphics (JPEG, PNG, WebP, BMP, HEIC), rejection of SVGs, animated GIFs, corrupted files, and tiny files (<40KB) with actionable alternative suggestions.
  - Subsampling memory-safe bitmap decoding (`inSampleSize`) preventing OOM on massive multi-megapixel camera photos.
  - Quality tuning slider (10% to 100%) with instant presets (*Extreme 40%*, *Optimal 65%*, *Crisp 85%*, *Max 100%*).
  - Multi-resolution downscaling options (*Original*, *4K 3840px*, *FHD 1920px*, *HD 1280px*, *Web 800px*).
  - EXIF & Geolocation privacy stripping to remove GPS coordinates and camera metadata before sharing.
- **Single & Batch Compression UI (`ImageCompressorScreen.kt`)**:
  - Mode switch between Quality Percentage Slider and Target File Size (MB).
  - Live Estimator Card displaying estimated compressed size and visual quality fidelity rating upon graphic selection.
  - Informative Warning & Advisory cards with alternative recommendations.
  - Real-time photo thumbnail preview with dimension and file size indicators.
  - Dynamic result cards highlighting size reductions (e.g. *-85%* saved), before/after size comparisons, and direct Android ShareSheet integration.
  - Batch image progress tracking with total folder size savings summaries.
- **Automated Unit Test Suite (`ImageCompressorTest.kt`)**:
  - Automated tests covering target size compression limits, graphic analysis & estimator formulas, guardrail rejections, downscaling constraints, EXIF stripping, and inSampleSize calculations.
- **Full 9-Language Localization**:
  - Complete native translations across English (`en`), German (`de`), Spanish (`es`), French (`fr`), Indonesian (`in`), Italian (`it`), Portuguese - Portugal (`pt`), Portuguese - Brazil (`pt-rBR`), and Romanian (`ro`).
- **History Repository Integration**:
  - Automatically records compressed images and batch output folders in the local history repository.

---

## [1.0.0] - 2026-08-20

### Initial Release
- **PDF Optimization**: Single PDF Compression with quality slider, metadata stripping, and grayscale conversion; Batch PDF folder compression.
- **PDF Creation**: Scan to PDF with ML Kit Document Scanner; Images to PDF; Text to PDF formatted converter; Text Format Converter (TXT, Markdown, CSV, HTML, JSON).
- **PDF Organization**: Merge PDFs with drag-and-drop ordering; Split PDF page ranges; Delete pages; Extract embedded images; Rotate pages.
- **Inspection & Cleaners**: Inspect metadata; Strip metadata; Text cleaner utility; OCR / PDF to Text extractor.
- **Privacy & Security**: 100% offline-first on-device execution; Zero cloud uploads; Google Play Billing integration; Full dark mode and custom typography.
