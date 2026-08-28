# Changelog

All notable changes to the **PDFchemy Tools (Shrink PDF)** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-08-28 — The Major Overhaul Release

### 🔐 PDF Password Security & Encryption
- **Protect PDF with 256-Bit AES Encryption**: Lock sensitive documents with custom User (view/open) and Owner passwords with granular access permissions.
- **Unlock & Decrypt PDFs**: Remove passwords and decrypt encrypted files with permission.
- **Smart Encryption Guardrails**: Automatic detection across all tools (`isPdfPasswordProtected`) to prevent crashes and prompt for passwords gracefully.
- **Permanent Redaction**: Permanent black-box redaction of sensitive text, account numbers, and visual areas.
- **Metadata Sanitizer**: Comprehensive metadata inspection and one-tap stripping of author, creator, camera, and software history.

### ✍️ Interactive PDF Studio, Reader & Annotation
- **Interactive Full-Page Canvas (`PdfEditor`)**: 100% on-device hardware-accelerated reader canvas with multi-page navigation.
- **Freehand Drawing & Signature Capture**: High-precision Bezier stroke capture with adjustable thickness and full RGBA palette.
- **Translucent Highlighter**: Fluorescent alpha-blended highlighting for document reviews and study notes.
- **Draggable Text Overlays**: Insert custom styled text anywhere on pages with custom fonts and colors.
- **Enterprise Business Stamps**: Instant stamps (*APPROVED*, *CONFIDENTIAL*, *DRAFT*, *PAID*, *REJECTED*, *FINAL*, *URGENT*).
- **In-Canvas Page Actions**: Rotate 90°/180°/270° or delete individual pages directly inside the editor with vector preservation.
- **Accessible Reflow Reader (`ReflowReaderScreen`)**: Clean reading view with customizable typography, font scaling, night mode, and text-to-speech (TTS) accessibility.

### 🖼️ Advanced Image Optimization Suite
- **Target File Size Mode**: Compress photos to exact sizes (e.g., 2MB email limit) using binary-search optimization.
- **Live Quality & Size Estimator**: Real-time preview of projected compression ratio and visual fidelity rating (*Negligible*, *Minimal*, *Moderate*, *Significant*).
- **Batch Image Compression**: Compress entire gallery selections simultaneously with aggregate folder savings statistics.
- **EXIF & Geolocation Stripping**: Automatically remove camera parameters, GPS coordinates, and device metadata.
- **Multi-Resolution Downscaling**: Preset scales (*4K 3840px*, *FHD 1920px*, *HD 1280px*, *Web 800px*).

### 📑 Document Organization, Publishing & Layout
- **PDF Compare (Diff)**: Side-by-side visual and textual revision comparison between two document revisions.
- **Booklet Imposition**: Generate print-ready 2-up folded booklets with automated sheet reordering.
- **N-Up Multi-Page Layout**: Print multiple pages per sheet (2-up, 4-up, 9-up).
- **Visual Page Cropper**: Margin trimming and interactive custom crop box adjustments.
- **Running Headers, Footers & Numbering**: Dynamic page numbers, dates, and customizable header/footer positioning.
- **Bookmark & Outline Editor**: Build and reorganize document table-of-contents navigation trees.
- **E-Book & Comic Suite**: Convert PDFs to reflowable EPUB 3.0 ebooks (for Kindle/Kobo) or Comic CBZ archives, and CBZ back to PDF.
- **Markdown Studio**: Render rich Markdown documents directly into formatted PDFs with custom typography, code blocks, and tables.
- **Interactive Form Filler & AcroForms**: Fill out PDF form fields and flatten interactive elements into permanent pages.
- **Embedded Image Replacer**: Swap or optimize individual embedded raster images inside PDFs without rebuilding the document.
- **Find & Replace**: Search and replace text occurrences throughout PDF pages.

### 🔍 Inspection, Repair & Performance
- **Document Repair Engine**: Recover corrupted, damaged, or unclosed PDF files.
- **PDF/A Archival Validator**: Verify compliance with long-term digital preservation standards (PDF/A-1b, PDF/A-2b).
- **Embedded Font Inspector**: Inspect embedded fonts, glyph counts, subset encodings, and font structures.
- **Fast Web View (Linearization)**: Reorganize PDF internal object tables for instant page-at-a-time web streaming.
- **Grayscale & Monochrome Optimizer**: Convert full-color documents to clean, space-saving grayscale.
- **R8 Code Optimization & Resource Shrinking**: Minified release bundle with stripped unused resources for minimal memory footprint and fast startup.

### 🌍 Global Reach
- **20 Languages (21 Locales)**: Full native localization across English, Arabic, German, Spanish, French, Hindi, Indonesian, Italian, Japanese, Korean, Dutch, Polish, Portuguese (Portugal & Brazil), Romanian, Russian, Thai, Turkish, Vietnamese, and Chinese (Simplified & Traditional).
- **100% Offline & Local**: All operations run purely on-device with zero server dependencies or cloud uploads.

---

## [1.0.0] - 2026-08-20

### Initial Release
- **PDF Optimization**: Single PDF Compression with quality slider, metadata stripping, and grayscale conversion; Batch PDF folder compression.
- **PDF Creation**: Scan to PDF with ML Kit Document Scanner; Images to PDF; Text to PDF formatted converter; Text Format Converter (TXT, Markdown, CSV, HTML, JSON).
- **PDF Organization**: Merge PDFs with drag-and-drop ordering; Split PDF page ranges; Delete pages; Extract embedded images; Rotate pages.
- **Inspection & Cleaners**: Inspect metadata; Strip metadata; Text cleaner utility; OCR / PDF to Text extractor.
- **Privacy & Security**: 100% offline-first on-device execution; Zero cloud uploads; Google Play Billing integration; Full dark mode and custom typography.
