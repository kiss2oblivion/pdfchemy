# PDFchemy Tools - Feature List

This document outlines all the tools and capabilities built into **PDFchemy Tools**, organized by their core categories. The app focuses on offline, local, and subscription-free PDF manipulation.

---

## 1. Compress (Optimization)
- **Compress PDF**: Shrink a single PDF file with an adjustable quality slider (0-100%). Includes options to strip metadata and convert to grayscale to further reduce file size.
- **Batch Compress PDF**: Select a folder and compress all PDF files inside it simultaneously.
- **Compress Images**: Compress single or multiple JPG, PNG, and WebP images with custom quality slider, downscale resolution limits (4K/1080p/720p/Web), format conversion, and EXIF privacy stripping.
- **Batch Compress Images**: Select multiple photos from your gallery and compress them together to a chosen destination directory with live progress tracking and total size savings statistics.

## 2. Create (Generation)
- **Scan to PDF**: Uses Google's native ML Kit Document Scanner to take photos of physical documents, crop them, enhance them, and save them silently to the app's internal repository.
- **Images to PDF**: Select multiple JPG/PNG images from your gallery and combine them into a single PDF document.
- **Text to PDF**: Type or paste raw text and convert it instantly into a formatted PDF.
- **Text Format Converter**: Parse and convert data seamlessly between TXT, Markdown, CSV, HTML, and JSON formats.

## 3. Organize (Structural Editing)
- **Merge PDFs**: Select multiple PDFs and combine them into one. Supports long-press and drag-to-reorder, and integrates directly with the app's History Repository to merge recent scans.
- **Split PDF**: Extract specific page ranges (e.g., `1-3, 5`) from a larger document into a new PDF.
- **Delete Pages**: Select specific pages to permanently remove from a PDF.
- **Extract Images**: Pull out all embedded images from a PDF and save them as individual image files.
- **Rotate Pages**: Rotate specific pages or entire documents (90°, 180°, 270°).

## 4. Check & Clean (Inspection & Security)
- **Inspect Metadata**: View hidden information embedded in a PDF, such as Author, Creator, Subject, Keywords, and Creation Date.
- **Strip Metadata**: Permanently remove all metadata from a PDF to ensure privacy before sharing.
- **Text Cleaner**: A utility to clean up raw text (trim spaces, normalize spacing, remove empty lines, remove duplicates, sort lines, and format/minify JSON).
- **PDF to Text / OCR**: Extract text from digital PDFs or use optical character recognition to extract text from scanned images.

## 5. System Features
- **History Repository**: Automatically saves your recent operations (like scanned documents) to an internal repository, allowing you to easily access them for subsequent tools (like merging).
- **Offline First**: All processing (compression, merging, splitting) happens entirely on your device using native Android APIs and PdfBox-Android. No data is sent to the cloud.
- **Dynamic Theming**: Full dark mode support with a soft neon, glassmorphic UI aesthetic. Includes toggles for haptic feedback and sound effects.
