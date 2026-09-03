# Official Multi-Store Distribution Guide 🚀

> **PDFchemy Tools (Shrink PDF)** — Windows & Linux Multi-Store Release Manual

This document provides complete instructions for publishing and maintaining PDFchemy across all major digital stores and package managers for Windows and Linux.

---

## 📦 Summary of Supported Formats & Channels

| Platform | Store / Channel | Package Format | Publish Command / Path |
| :--- | :--- | :--- | :--- |
| **Windows** | **Microsoft Store** | Win32 / MSIX | [Partner Center](https://partner.microsoft.com/dashboard) (`packaging/msstore/`) |
| **Windows** | **Windows Package Manager (`winget`)** | Native MSI | `wingetcreate submit` (`packaging/winget/`) |
| **Windows** | **Chocolatey** | `.nupkg` | `choco push` (`packaging/chocolatey/`) |
| **Windows** | **Scoop** | Manifest `.json` | Scoop Bucket PR (`packaging/scoop/`) |
| **Linux** | **Flathub (Flatpak)** | Flatpak bundle | [Flathub Submission](https://github.com/flathub/flathub) (`packaging/flathub/`) |
| **Linux** | **Snap Store (Canonical)** | `.snap` | `snapcraft upload` (`packaging/snap/`) |
| **Linux** | **Universal AppImage** | `.AppImage` | `./packaging/appimage/build-appimage.sh` |
| **Linux** | **Debian / Ubuntu / Mint** | `.deb` | `./gradlew :desktop:packageDeb` |
| **Linux** | **Fedora / RHEL / openSUSE** | `.rpm` | `./gradlew :desktop:packageRpm` |

---

## 1. 🪟 Windows Digital Stores

### A. Windows Package Manager (`winget`)
Winget is built into Windows 10 and 11. Users install via:
```powershell
winget install PDFchemy.PDFchemy
```

#### How to Submit / Update:
1. Install `wingetcreate`:
   ```powershell
   winget install Microsoft.WingetCreate
   ```
2. Submit the release MSI:
   ```powershell
   wingetcreate submit https://github.com/cucosandrei/pdfchemy/releases/download/v1.0.0/PDFchemy-windows-x64-1.0.0.msi
   ```
   *`wingetcreate` automatically downloads the MSI, calculates the SHA256 hash, parses version metadata, and creates a Pull Request directly against Microsoft's `microsoft/winget-pkgs` repository.*

---

### B. Microsoft Store (Win32 Store Program)
Microsoft Store allows developers to publish standard Win32 desktop apps (`.msi` / `.exe`) directly with zero commission on free apps.

#### Submission Steps:
1. Log in to [Microsoft Partner Center](https://partner.microsoft.com/dashboard).
2. Click **New Product** -> **Windows & Xbox app**.
3. Name: `PDFchemy Tools`.
4. Distribution method: Choose **Desktop application (Win32)**.
5. Installer URL:
   `https://github.com/cucosandrei/pdfchemy/releases/download/v1.0.0/PDFchemy-windows-x64-1.0.0.msi`
6. Silent install parameters: `/qn /norestart`.
7. Category: **Productivity > Document Management**.
8. Price: **Free**.
9. Privacy Policy: `https://github.com/cucosandrei/pdfchemy/blob/main/README.md`.
10. Submit for Certification (takes 24–48 hours).

---

### C. Chocolatey & Scoop
* **Chocolatey:**
  ```powershell
  cd packaging/chocolatey
  choco pack
  choco push pdfchemy.1.0.0.nupkg --api-key <YOUR_API_KEY> --source https://push.chocolatey.org/
  ```
  Users install via: `choco install pdfchemy`

* **Scoop:**
  Add `packaging/scoop/pdfchemy.json` to your custom scoop bucket or submit to `ScoopInstaller/Extras`.
  Users install via: `scoop install pdfchemy`

---

## 2. 🐧 Linux Digital Stores

### A. Flathub (The Universal Linux App Store)
Flathub is the standard app store for Fedora, Steam Deck, Ubuntu, Arch Linux, Linux Mint, and Debian.

#### Submission Steps:
1. Fork the [flathub/flathub](https://github.com/flathub/flathub) repository on GitHub.
2. Create a new branch named `com.pdfchemy.PDFchemy`.
3. Add the files from `packaging/flathub/`:
   * `com.pdfchemy.PDFchemy.yaml`
   * `com.pdfchemy.PDFchemy.metainfo.xml`
4. Open a Pull Request on GitHub. The Flathub automated test bot will build the Flatpak, verify permissions, and provide a test build link.
5. Once merged, PDFchemy is live on [flathub.org](https://flathub.org) and visible in GNOME Software & KDE Discover!
   Users install via: `flatpak install flathub com.pdfchemy.PDFchemy`

---

### B. Canonical Snap Store (Ubuntu & Modern Linux)
Snap is the default software store in Ubuntu.

#### Submission Steps:
1. Install Snapcraft:
   ```bash
   sudo snap install snapcraft --classic
   ```
2. Build the snap:
   ```bash
   cd packaging/snap
   snapcraft
   ```
3. Upload to the Snap Store:
   ```bash
   snapcraft login
   snapcraft register pdfchemy
   snapcraft upload --release=stable pdfchemy_1.0.0_amd64.snap
   ```
   Users install via: `snap install pdfchemy`

---

### C. Universal Standalone AppImage
Runs on any Linux distribution without root access:
```bash
./packaging/appimage/build-appimage.sh
```
Output: `PDFchemy-x86_64.AppImage`.
Can be submitted to **AppImageHub** (`https://appimage.github.io/`).

---

## 3. 🤖 Automated Release Pipeline (CI/CD)

Whenever you push a version tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions automatically executes `.github/workflows/desktop-release.yml` to:
1. Build Windows `.msi`, `.exe`, and portable `.jar`.
2. Build Linux `.deb`, `.rpm`, and `.AppImage`.
3. Generate SHA256 checksums (`SHA256SUMS.txt`).
4. Publish an official GitHub Release with all binaries attached.
