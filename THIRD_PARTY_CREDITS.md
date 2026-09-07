# Third-Party Open-Source Credits & Attributions 💖

PDFchemy Tools stands on the shoulders of giants. We believe in absolute, uncompromised honesty, ethical development, and giving credit where credit is due.

The core promise of PDFchemy is **100% offline, local-first document utility**. Every single feature—from rendering and compressing to signing, form building, and optical character recognition—is powered by battle-tested, permissive open-source software running locally on your device.

Below is the complete, comprehensive record of all open-source libraries, engines, and frameworks that make PDFchemy possible, along with their respective authors and licenses.

---

## 🏛️ Core PDF & Document Processing Engines

### 1. [Apache PDFBox](https://pdfbox.apache.org/)
* **Authors:** The Apache Software Foundation & PDFBox Community
* **Version:** 2.0.31
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy (Desktop):**
  * Powers document parsing, page geometry, visual thumbnail extraction, and iterative compression.
  * Interactive AcroForms architecture (field generation, form inspection, text field, checkbox, and radio button authoring).
  * PDF/A archiving conversion, linear fast web view optimization, Bates numbering, and watermarking.
  * PDF text extraction and search indexing for the Directory Spotlight Search.

### 2. [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android)
* **Authors:** Tom Roush & The Apache Software Foundation
* **Version:** 2.0.27.0
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy (Android):**
  * Mobile port of Apache PDFBox enabling native Android PDF parsing, AcroForm filler, metadata editing, page tree restructuring, and document manipulation without any remote cloud dependency.

---

## 🎨 UI Frameworks & Language Tooling

### 3. [JetBrains Compose Multiplatform & Kotlin](https://github.com/JetBrains/compose-multiplatform)
* **Authors:** JetBrains s.r.o. & Contributors
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy:**
  * Cross-platform declarative UI for the Desktop application (Linux & Windows).
  * Modern, reactive state management and high-performance hardware-accelerated desktop rendering via Skiko/Skia.

### 4. [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* **Authors:** JetBrains s.r.o.
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy:**
  * Asynchronous processing pipelines, non-blocking UI operations, and multi-core parallel execution across CPU threads in the Batch Processing Queue.

### 5. [Android Jetpack & Jetpack Compose](https://developer.android.com/jetpack)
* **Authors:** Google LLC & The Android Open Source Project (AOSP)
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy (Android):**
  * Native modern Android declarative UI (`Material3`, navigation, window size classes, dynamic color, edge-to-edge window insets).
  * Core system integration (`androidx.core`, `androidx.lifecycle`, `androidx.activity`).

---

## 🔐 Cryptography, PKI & Security

### 6. [The Legion of the Bouncy Castle](https://www.bouncycastle.org/)
* **Authors:** The Legion of the Bouncy Castle Inc.
* **Libraries:** `bcprov-jdk18on`, `bcpkix-jdk18on` (v1.78)
* **License:** [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html) (Permissive MIT/BSD-style license)
* **Usage in PDFchemy (Android & Desktop):**
  * Industrial-grade cryptographic provider.
  * Powers PKI digital signatures, self-signed X.509 certificate generation, CMS/PKCS#7 detached signature verification, and secure document integrity checking.

---

## 👁️ Optical Character Recognition (OCR) & Scanning

### 7. [Tess4J & Tesseract OCR](https://tess4j.sourceforge.net/)
* **Authors:** Quan Nguyen, Ray Smith, Google Inc. & Tesseract OCR Contributors
* **Version:** 5.7.0
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy (Desktop):**
  * Local, offline Optical Character Recognition (OCR) engine for converting scanned PDFs into searchable, selectable text without third-party web services.

### 8. [Google ML Kit (On-Device Vision)](https://developers.google.com/ml-kit)
* **Authors:** Google LLC
* **Libraries:** `play-services-mlkit-document-scanner`, `com.google.mlkit:text-recognition`
* **License:** Android Software Development Kit License / Google APIs Terms
* **Usage in PDFchemy (Android):**
  * High-speed on-device document camera perspective warping, boundary detection, and offline mobile text recognition.

---

## 🔄 Document Parsing, Formats & Text Converters

### 9. [jsoup: Java HTML Parser](https://jsoup.org/)
* **Authors:** Jonathan Hedley
* **Version:** 1.17.2
* **License:** [MIT License](https://jsoup.org/license)
* **Usage in PDFchemy:**
  * Fast HTML and EPUB eBook DOM parsing, tag stripping, entity unescaping, and structured text extraction for the EPUB-to-PDF and Web-to-PDF engines.

### 10. [Flexmark-Java](https://github.com/vsch/flexmark-java)
* **Authors:** Vladimir Schneider & Contributors
* **Version:** 0.64.8
* **License:** [BSD 2-Clause License](https://github.com/vsch/flexmark-java/blob/master/LICENSE.txt)
* **Usage in PDFchemy:**
  * CommonMark / Markdown parser and AST renderer for Markdown-to-PDF document compiling.

### 11. [FasterXML Jackson](https://github.com/FasterXML/jackson)
* **Authors:** FasterXML, LLC & Tatu Saloranta
* **Libraries:** `jackson-module-kotlin`, `jackson-dataformat-csv`, `jackson-dataformat-yaml`, `jackson-dataformat-xml` (v2.17.0)
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy:**
  * High-performance structured data streaming and formatting for CSV table, XML, and YAML document export tools.

---

## 🖼️ Media & Asset Loading

### 12. [Coil (Coroutine Image Loader)](https://coil-kt.github.io/coil/)
* **Authors:** Colin White & Coil Contributors
* **Version:** 2.6.0
* **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
* **Usage in PDFchemy (Android):**
  * High-performance, lightweight, coroutine-based image loading and caching for page previews and UI graphics.

---

## ⚖️ License Summary & Compliance

All third-party open-source components used in PDFchemy are distributed under permissive licenses:
* **Apache License 2.0** (Apache PDFBox, PdfBox-Android, JetBrains Compose, Kotlin Coroutines, Android Jetpack, Tess4J, Jackson, Coil)
* **MIT License** (jsoup)
* **BSD 2-Clause License** (flexmark-java)
* **Bouncy Castle Licence** (Bouncy Castle Cryptography)

These permissive licenses explicitly allow free use, modification, and redistribution. We express our deepest gratitude to all authors and communities who created and maintain these incredible open-source tools.
