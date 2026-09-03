# PDFchemy Tools (Shrink PDF) 📄✨

> **The emergency document utility for the people.**
> 100% Free • 100% Offline & Private • Zero Ads on Desktop • Original-Safe

---

## 📜 The Developer's Lifetime Manifesto

> *"Lifetime of updates until I personally die and free of charge; and all you have to do is make a solid valid request and it will be done and implemented; cuz it's from the people to the people; it may or may not be the same as a corpo app would do but at least it's gonna be free and I will make it as best as I possibly can; if I can't well I can't and that's that at least you have an option oh you enigmatic edge case that you are."*
> 
> — **Andrei Ioan Cucoș (John)**, Developer

---

## ⚡ Cross-Platform Support & Digital Stores

* **Android:** Google Play Store (Phones, Tablets & Foldables).
* **Windows:**
  * **Windows Package Manager:** `winget install PDFchemy.PDFchemy`
  * **Microsoft Store:** Available via Win32 Store Program (see [DISTRIBUTION.md](DISTRIBUTION.md)).
  * **Chocolatey & Scoop:** `choco install pdfchemy` • `scoop install pdfchemy`
  * **Native Installers:** 64-bit `.msi` and `.exe` direct downloads.
* **Linux:**
  * **Flathub (Flatpak):** `flatpak install flathub com.pdfchemy.PDFchemy`
  * **Snap Store:** `snap install pdfchemy`
  * **Universal AppImage:** Portable 1-click executable (`.AppImage`)
  * **Native Packages:** Debian/Ubuntu (`.deb`) and Fedora/RHEL (`.rpm`)

*See [DISTRIBUTION.md](DISTRIBUTION.md) for full publishing guides.*

## 🚀 Desktop Superpowers

1. **Visual Page Studio (The "PDF Arranger Killer"):**
   * Real-time Compose thumbnail grid of all pages.
   * In-place quick actions: 🔄 Rotate 90°, ⬅️ ➡️ Reorder sequence, 📄 Duplicate page, and 🗑️ Delete page.
   * One-click "Save Organized PDF".
2. **Smart Target-Size Compressor ("Fit Under 2MB"):**
   * Automatic iterative DPI and JPEG quality optimizer to guarantee your output fits under government, email, and portal upload thresholds (500 KB, 1.0 MB, 2.0 MB, 5.0 MB).
3. **Images ⇄ PDF Studio:**
   * **Images to PDF:** Select any collection of photos, scans, or receipts (PNG, JPG, BMP, WebP) and compile them into a unified PDF.
   * **PDF to Images:** Batch-extract every page as high-res 150/300 DPI PNG images into any chosen local folder.
4. **Multi-Core Batch Queue:**
   * Drop 10, 20, or 50+ PDFs and batch compress or merge in parallel across all CPU cores.
5. **Privacy & Security:**
   * 128/256-bit AES encryption and password removal.
   * Zero cloud uploads. Zero telemetry. Zero file leaks.

---

## ☕ Support the Developer (Tip Jar)

PDFchemy is 100% free with zero paywalls and zero subscriptions. If this tool saved your day, consider leaving a tip to support independent development:

* ☕ **Buy Me a Coffee:** [buymeacoffee.com/cucosandrei](https://buymeacoffee.com/cucosandrei)
* 💳 **PayPal:** [paypal.me/cucosandrei](https://paypal.me/cucosandrei)
* 💖 **GitHub Sponsors:** [github.com/sponsors/cucosandrei](https://github.com/sponsors/cucosandrei)
* 📬 **Contact / PayPal Email:** `cucosandreiioan@gmail.com`

---

## 🛠️ Building & Running Locally

### Prerequisites
* JDK 17 or higher (e.g. JetBrains Runtime / OpenJDK).

### Run Desktop App
```bash
./gradlew :desktop:run
```

### Build Runnable JAR
```bash
./gradlew :desktop:packageUberJarForCurrentOS
```
The output JAR is generated at `desktop/build/compose/jars/PDFchemy-<os>-x64-1.0.0.jar`.

### Build Android Debug APK
```bash
./gradlew :app:assembleDebug
```

---

## 📄 License
Built with passion for the people. Free for personal and commercial use.
