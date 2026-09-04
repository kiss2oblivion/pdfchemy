# Privacy Policy — PDFchemy Tools (Shrink PDF)

**Last Updated:** September 4, 2026  
**Developer:** Andrei Ioan Cucoș (John / cucosandreiioan@gmail.com)  
**Repository:** [https://github.com/kiss2oblivion/pdfchemy](https://github.com/kiss2oblivion/pdfchemy)

---

## 1. 100% Local-First & Air-Gapped Architecture

PDFchemy Tools is designed from the ground up as a **zero-leak, local-first, emergency document utility**.

* **Zero Server Uploads:** All document processing (compression, conversion, page organization, visual editing, digital signatures, OCR, metadata sanitization, and encryption) executes entirely on your device's CPU and RAM.
* **No Cloud Dependencies:** We do not operate external processing servers, conversion APIs, or cloud storage backends. Your documents never touch the internet.
* **Zero Telemetry & Analytics:** PDFchemy does not track user behavior, does not collect analytics, does not log document names or content, and does not report usage statistics.

---

## 2. Document & Data Security

* **Original-Safe Operation:** PDFchemy never alters, overwrites, or damages your original source files. All operations produce new output files in locations explicitly selected by the user.
* **Cryptographic Privacy:** Password protection and encryption algorithms (AES-128 and AES-256) are computed natively on-device. Your passphrases and encryption keys are held temporarily in volatile memory only for the duration of the cryptographic operation and are never stored or transmitted.
* **True Redaction:** When redacting confidential information (e.g. SSNs, credit cards, legal names), PDFchemy physically strips and erases the underlying vector text and raster pixels beneath the redaction bounding box so that the redacted information cannot be recovered via copy-pasting, reverse-engineering, or search indexing.

---

## 3. Network Access & Permissions

* **Desktop Application (Windows & Linux):** Requires zero network permissions. The application functions identically when completely disconnected from the internet (air-gapped environments).
* **Android Application:** 
  * Storage access is requested solely via standard Android Storage Access Framework (SAF) pickers to read and save documents chosen by the user.
  * Camera permission (optional) is requested solely for the on-device ML Kit document scanner. Images captured by the scanner are processed on-device and never transmitted.

---

## 4. Third-Party Services

* **No Advertising on Desktop:** The Windows and Linux desktop editions contain zero advertisements and zero tracking SDKs.
* **Respectful Monetization on Mobile:** The Android mobile edition does not share document data with advertising networks. Document processing workflows are never interrupted or blocked.

---

## 5. Contact & Questions

If you have questions or inquiries regarding the privacy practices of PDFchemy Tools, contact the developer:

* **Email:** [cucosandreiioan@gmail.com](mailto:cucosandreiioan@gmail.com)
* **GitHub Issues:** [https://github.com/kiss2oblivion/pdfchemy/issues](https://github.com/kiss2oblivion/pdfchemy/issues)
