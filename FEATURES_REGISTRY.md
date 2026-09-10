# PDFchemy Tools — Master Features Registry 📜

> **Single Source of Truth for Implemented Capabilities**
> **Core Invariant:** 100% Local-First & Private. Zero Cloud. Zero AI/LLMs. Zero Telemetry. Original-Safe.
> **Performance Architecture:** Linear-Time O(N) Document Traversal. Single-pass custom TextStripper architecture eliminates quadratic O(N^2) page-tree lookups across all text search, redaction, reflow reading, visual diffing, EPUB conversion, and Office export modules.
> **Last Updated:** September 2026

---

## 📱 Android Edition (`app`)

### 1. 🗜️ Compression & Optimization
| Feature | UI Screen | Engine / Logic | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **PDF Compressor** | `CompressScreen.kt` | `PdfCompressor.kt` | ✅ Live | 4 Presets (Extreme, Recommended, High Quality, Custom DPI/Quality), Flate & JBIG2 |
| **Grayscale Optimizer** | `GrayscaleOptimizerScreen.kt` | `PdfCompressor.kt` | ✅ Live | Converts color PDF pages to monochrome/grayscale to drastically reduce size |
| **Linearize (Fast Web View)** | `LinearizePdfScreen.kt` | `PdfManipulator.kt` | ✅ Live | Restructures PDF dictionary and stream orders for instant first-page web/mobile streaming |
| **Flatten PDF** | `FlattenPdfScreen.kt` | `PdfEditor.kt` | ✅ Live | Permanently bakes form fields, comments, and annotations into the base page layer |

---

### 2. 📑 Page Studio & Organization
| Feature | UI Screen | Engine / Logic | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Merge PDFs** | `MergePdfsScreen.kt` | `PdfManipulator.kt` | ✅ Live | Multi-document combiner with drag-and-drop reordering |
| **Split PDFs (Range / All)** | `OrganizeScreens.kt` | `PdfManipulator.kt` | ✅ Live | Split into individual pages or arbitrary page ranges (e.g. `1-3, 5, 8-10`) |
| **Split by Blank Pages** | `OrganizeScreens.kt` | `MainViewModel.splitByBlankPages` | ✅ Live | Auto-detects blank separator sheets in batch scanner feeds and splits into individual documents |
| **Split by Bookmarks / Chapters** | `OrganizeScreens.kt` | `MainViewModel.splitByBookmarks` | ✅ Live | Auto-splits multi-chapter books or court bundles based on PDF document outlines |
| **Page Organizer** | `PageOrganizerScreen.kt` | `PdfManipulator.kt` | ✅ Live | Visual thumbnail grid: reorder, delete, duplicate, rotate individual pages |
| **Rotate Pages** | `OrganizeScreens.kt` | `PdfManipulator.kt` | ✅ Live | Lossless 90°, 180°, 270° orientation correction |
| **Auto-Deskew & Straighten** | `DeskewScreen.kt` | `PdfDeskewEngine.kt` | ✅ Live | Hough transform scan tilt auto-detection, manual angle slider, live rotation preview |
| **Page Cropper & Margin Trimmer** | `PageCropperScreen.kt` | `PdfManipulator.kt` | ✅ Live | CropBox adjustment to remove scanner borders and margins |
| **Paper Canvas Resizer** | `PageLayoutScreen.kt` | `PdfLayoutEngine.kt` | ✅ Live | Resizes standard paper dimensions (A4, Letter, Legal, A3, Executive) with content re-centering |
| **N-Up Handouts** | `NUpScreen.kt` | `PdfManipulator.kt` | ✅ Live | Imposes 2, 4, 6, 9, or 16 pages per sheet with Z-order / N-order and subtle borders |
| **Booklet Imposition** | `BookletScreen.kt` | `PdfManipulator.kt` | ✅ Live | Saddle-stitch fold printer ordering (4-page signature booklet imposition) |

---

### 3. 🔄 Creation & Conversion
| Feature | UI Screen | Engine / Logic | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Images to PDF** | `ImageToPdfScreen.kt` | `ImageToPdfConverter.kt` | ✅ Live | Converts camera photos, receipts, and gallery images into standardized PDF |
| **PDF to High-Res Images** | `PdfToImageScreen.kt` | `PdfRenderer` | ✅ Live | Exports all pages as PNG or JPEG image files |
| **Scan to PDF** | `ScanPdfScreen.kt` | `CameraCaptureHelper.kt` | ✅ Live | Hardware camera scan with edge auto-detection and perspective correction |
| **EPUB to PDF Converter** | `EbookConverterScreen.kt` | `EpubConverter.kt` | ✅ Live | Parses standard EPUB ebooks, styles fonts/margins, and exports paginated PDF |
| **Markdown to PDF Studio** | `MarkdownStudioScreen.kt` | `MarkdownParser.kt` | ✅ Live | Rich Markdown text editor with instant live HTML/PDF rendering |
| **Text to PDF Converter** | `TextConverterScreen.kt` | `TextConverter.kt` | ✅ Live | Converts `.txt`, logs, and source code into clean paginated documents |
| **Table Extractor to CSV** | `TableExtractorScreen.kt` | `PdfTableExtractorEngine.kt` | ✅ Live | Spatial 2D column clustering: extracts tables to RFC 4180 CSV / Excel spreadsheets |
| **Office Export (Word / Excel / PPTX)** | `OfficeExportScreen.kt` | `OfficeExportEngine.kt` | ✅ Live | Pure OpenXML archive generators: exports PDF to `.docx`, `.xlsx`, and `.pptx` (with slide backdrops) |
| **On-Device OCR** | `OcrScreens.kt` | `PdfOcrEngine.kt` | ✅ Live | 100% offline optical character recognition, creating searchable text layers |

---

### 4. ✍️ Form Filling & Document Editing
| Feature | UI Screen | Engine / Logic | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Visual PDF Editor** | `PdfEditorScreen.kt` | `PdfEditor.kt` | ✅ Live | Freehand pen, highlighter, custom text overlays, shape rectangles, signature stamp |
| **Quick Fill & Sign** | `QuickFillSignScreen.kt` | `PdfEditor.kt` | ✅ Live | Designed for flat/scanned forms: tap anywhere to place Text, Checkmarks (✓), Crosses (✗), Dates, Signatures |
| **Interactive Form Builder** | `FormBuilderScreen.kt` | `AcroFormEngine.createAcroFormWithFields` | ✅ Live | Converts flat PDFs into genuine fillable forms with interactive text fields, checkboxes, and dropdowns |
| **AcroForm Interactive Filler** | `AcroFormScreens.kt` | `PdfEditor.kt` | ✅ Live | Inspects and fills standard interactive PDF forms, text boxes, and checkboxes |
| **Visual Signer** | `SignPdfScreen.kt` | `PdfEditor.kt` | ✅ Live | Draw signatures with vector smoothing, save reusable presets, place anywhere on page |
| **Watermark Studio** | `WatermarkScreen.kt` | `PdfEditor.kt` | ✅ Live | Custom text/image watermarks with opacity, angle, scaling, and diagonal tiling |
| **Header & Footer Studio** | `HeaderFooterScreen.kt` | `PdfEditor.kt` | ✅ Live | Embed running headers and footers with custom margins and alignment |
| **Page Numbering Studio** | `PageNumberScreen.kt` | `PdfEditor.kt` | ✅ Live | Custom page numbers (`Page X of Y`, `X/Y`, `X`), position, font, and start offset |
| **Bates Numbering** | `BatesNumberScreen.kt` | `PdfEditor.kt` | ✅ Live | Legal numbering (`PREFIX-00001-SUFFIX`), 6 placement positions, custom zero-padding |
| **Find & Replace Text** | `FindAndReplaceScreen.kt` | `PdfEditor.kt` | ✅ Live | Search text across pages and replace or redact matches |
| **Image Replacer** | `ImageReplacerScreen.kt` | `PdfEditor.kt` | ✅ Live | Replace embedded raster image objects in PDF streams without touching text |

---

### 5. 🛡️ Security, Privacy & Compliance
| Feature | UI Screen | Engine / Logic | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Encrypt / Password Protect** | `EncryptPdfScreen.kt` | `PdfSecurity.kt` | ✅ Live | AES-128 / AES-256 standard PDF encryption with user and owner passwords |
| **Decrypt / Unlock PDF** | `DecryptPdfScreen.kt` | `PdfSecurity.kt` | ✅ Live | Strips passwords and permissions restrictions permanently |
| **Permanent Smart Redaction** | `RedactionScreen.kt` | `PdfRedactionEngine.kt` | ✅ Live | Regex PII auto-detection (emails, phone numbers, SSNs) + permanent underlying stream scrubbing |
| **Deep Threat Sanitizer** | `DocumentSanitizerScreen.kt` | `PdfSanitizerEngine.kt` | ✅ Live | Audits and strips embedded JavaScript triggers, launch actions, URI tracking beacons |
| **Vanguard Zero-Trust Shield** | `MainActivity.kt`, `PdfEditorScreen.kt`, `ReflowReaderScreen.kt` | `PdfSanitizerEngine.checkVanguardThreat` | ✅ Live | Pre-flight zero-trust gatekeeper: fails closed on encrypted/password-protected PDFs (cannot inspect scripts without key) with 1-click Unlock PDF routing, and immediately blocks opening of PDFs containing embedded JavaScript, /Launch actions, /OpenAction, /AA auto-run hooks, or attachments |
| **Metadata Sanitizer** | `MetadataSanitizerScreen.kt` | `PdfMetadataSanitizer.kt` | ✅ Live | Inspects and purges author name, software creator, GPS coordinates, editing history |
| **PDF/A Preflight Validator** | `PdfAValidatorScreen.kt` | `PdfAValidator.kt` | ✅ Live | Audits ISO 19005 compliance (OutputIntents, DeviceRGB/CMYK, font subsets, XMP) |
| **Typography & Font Inspector** | `FontInspectorScreen.kt` | `FontInspector.kt` | ✅ Live | Lists embedded font programs, TrueType/Type1/Type0, subsets, and character encodings |
| **Embedded Attachments Manager**| `AttachmentManagerScreen.kt` | `PdfManipulator.kt` | ✅ Live | Inspects, extracts, and embeds arbitrary file attachments and PDF portfolios |
| **PDF Repair Studio** | `RepairPdfScreen.kt` | `PdfRepairEngine.kt` | ✅ Live | Reconstructs broken cross-reference tables, truncated trailers, and corrupted streams |

---

### 6. 📖 Reading & Accessibility
| Feature | UI Screen | Engine / Logic | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Reflow Reader Studio** | `ReflowReaderScreen.kt` | `ReflowEngine.kt` | ✅ Live | E-reader mode with font scaling, themes (Light, Sepia, Dark, OLED), continuous flow |
| **Offline TTS (Read Aloud)** | `ReflowReaderScreen.kt` | `android.speech.tts.TextToSpeech` | ✅ Live | 100% offline text-to-speech with speed controls (0.75x–2.0x) and synced paragraph tracking |
| **Dual-Page Spread & Tabletop** | `ReflowReaderScreen.kt` | Compose layout | ✅ Live | Supports tabletop / flex mode on Samsung Fold / Pixel Fold devices and tablets |
| **Device Memory Safeguard** | `DeviceSafeguard.kt` | Runtime memory monitor | ✅ Live | Prevents phone lag/OOM on ultra-heavy scans with lightweight sequential safe mode |

---

## 💻 Desktop Edition (`desktop`)

| Tab / Category | Tools & Features | Engine / Implementation | Status |
| :--- | :--- | :--- | :--- |
| **Home Dashboard** | Recent documents list, quick drag-and-drop, quick action cards, Manifesto | `DesktopApp.kt`, `RecentDocumentsManager.kt` | ✅ Live |
| **Compress** | 3 Presets + Custom Quality/DPI, Grayscale toggle, Metadata stripping | `DesktopPdfEngine.compressPdf` | ✅ Live |
| **Page Studio** | Rotate (left/right), Reorder, Duplicate, Delete, Extract, Impose N-Up (2-Up, 4-Up), Booklet Creator, Margin Cropper, Auto-Deskew | `DesktopPdfEngine.kt` | ✅ Live |
| **Convert** | • Images to PDF<br>• PDF to High-Res PNG Images<br>• Extract Plain Text (.txt)<br>• OCR Searchable PDF (Tesseract)<br>• PDF to ISO 19005-1b PDF/A<br>• Extract Tables to RFC 4180 CSV<br>• **Office Export:** PDF to Word (.docx), Excel (.xlsx), PowerPoint (.pptx) | `DesktopPdfEngine.kt`, `DesktopOfficeExportEngine.kt` | ✅ Live |
| **Reader** | Single page, dual-page spread, zoom in/out, fit width, page rotation, dark/light theme | `DesktopPdfEngine.renderPage` | ✅ Live |
| **Security** | • Encrypt & Lock (User/Owner Password)<br>• Decrypt & Unlock<br>• Deep Threat Sanitizer (JS, Actions, Beacons)<br>• PDF Recovery & Repair (Broken XRef/Trailers)<br>• Permanent Redaction (Pattern & manual scrub)<br>• Embedded File Attachments (Inspect, Extract, Embed) | `DesktopPdfEngine.kt` | ✅ Live |
| **Compare Studio** | Side-by-side synchronized comparison, line-by-line textual diffs, revision change statistics | `DesktopApp.kt` (`CompareView`), `DesktopPdfEngine.compareDocuments` | ✅ Live |
| **Merge** | Multi-document combiner with reorderable list | `DesktopPdfEngine.mergePdfs` | ✅ Live |
| **Batch Studio** | Multi-file batch processing queue (Batch Compress, Batch Decrypt, Batch PDF/A) | `DesktopPdfEngine.kt` | ✅ Live |
| **Sign & Form Studio** | Draw signatures, upload seal images, business stamps, AcroForm fill & flatten, **Interactive Form Builder** (add text/checkbox/dropdown fields) | `DesktopPdfEngine.kt`, `SignAndStampView` | ✅ Live |
| **Spotlight Search** | Multi-file directory keyword search across hundreds of PDFs with line snippet extraction and instant 1-click page jump to Reader | `DesktopDirectorySearchEngine.kt`, `DirectorySpotlightSearchDialog` | ✅ Live |
| **Digital Signatures (PKI)** | Cryptographic certificate signing (`.p12` / `.pfx` keystores) with visual seal | `PdfCryptoSigner.kt` (BouncyCastle) | ✅ Live |
| **Bates Numbering** | Legal bates numbering engine with 6 placement positions | `DesktopPdfEngine.applyBatesNumbering` | ✅ Live |
| **Split Studio** | Split by Page Ranges, Split by Blank Pages, Split by Bookmarks | `DesktopPdfEngine.splitByBlankPages`, `splitByBookmarks` | ✅ Live |
| **Multi-Language (i18n)** | 20 languages / 21 locales with runtime top-bar switcher and CLI `--lang` flags | `DesktopLocalization.kt`, `DesktopStrings.kt` | ✅ Live |
| **In-App Updater** | Checks GitHub Releases for new updates without third-party tracking | `DesktopUpdateManager.kt` | ✅ Live |
| **The Lifetime Manifesto** | 4 Guarantees dialog & developer tip jar | `DesktopApp.kt` | ✅ Live |

---

## 🌍 Supported Locales (20 Languages / 21 Locales)
All user strings are 100% localized and AAPT format-escaped across:
1. `en` (English)
2. `ro` (Română)
3. `de` (Deutsch)
4. `es` (Español)
5. `fr` (Français)
6. `it` (Italiano)
7. `pt` (Português - Portugal)
8. `pt-rBR` (Português - Brasil)
9. `nl` (Nederlands)
10. `pl` (Polski)
11. `ru` (Русский)
12. `tr` (Türkçe)
13. `ar` (العربية)
14. `hi` (हिन्दी)
15. `in` / `id` (Bahasa Indonesia)
16. `ja` (日本語)
17. `ko` (한국어)
18. `th` (ไทย)
19. `vi` (Tiếng Việt)
20. `zh-rCN` (简体中文)
21. `zh-rTW` (繁體中文)

---

## ⏳ What Is NOT Yet Implemented (Future Roadmap)
1. **Audiobook / MP3 Audio Export:** Exporting TTS read aloud output to `.mp3` / `.wav` audio files.

