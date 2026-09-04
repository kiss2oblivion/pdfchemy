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
   wingetcreate submit https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.0/PDFchemy-windows-x64-1.0.0.msi
   ```
3. **Current Live PR:** [microsoft/winget-pkgs #429064](https://github.com/microsoft/winget-pkgs/pull/429064) (CLA Signed, Manifest & URL Validations Passed ✅, VM Testing in progress).
4. **v1.0.1 Update Command:**
   ```powershell
   wingetcreate update PDFchemy.PDFchemy --version 1.0.1 --urls https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.1/PDFchemy-windows-x64-1.0.1.msi
   ```

> [!TIP]
> **Footprint Optimization Breakthrough (v1.0.1):**
> - **Windows MSI:** Reduced from **135.9 MB** down to **68.74 MB** (**-49.4% footprint reduction**).
> - **Windows EXE:** Reduced from **136.6 MB** down to **69.39 MB** (**-49.2% footprint reduction**).
> - **Universal / Linux JAR:** Reduced from **110.2 MB** down to **40.22 MB** (**-63.5% footprint reduction**).



---

### B. Microsoft Store (Win32 Store Program)
Microsoft Store allows developers to publish standard Win32 desktop apps (`.msi` / `.exe`) directly with zero commission on free apps.

#### Submission Steps:
1. Log in to [Microsoft Partner Center](https://partner.microsoft.com/dashboard).
2. Click **New Product** -> **Windows & Xbox app**.
3. Name: `PDFchemy Tools`.
4. Distribution method: Choose **Desktop application (Win32)**.
5. Installer URL:
   `https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.1/PDFchemy-windows-x64-1.0.1.msi`
6. Silent install parameters: `/qn /norestart`.
7. Category: **Productivity > Document Management**.
8. Price: **Free**.
9. Privacy Policy: `https://github.com/kiss2oblivion/pdfchemy/blob/main/README.md`.
10. Submit for Certification (takes 24–48 hours).

---

### C. Chocolatey & Scoop
* **Chocolatey:**
  ```powershell
  cd packaging/chocolatey
  choco pack
  choco push pdfchemy.1.0.1.nupkg --api-key <YOUR_API_KEY> --source https://push.chocolatey.org/
  ```
  Users install via: `choco install pdfchemy`

* **Scoop:**
  Add `packaging/scoop/pdfchemy.json` to your custom scoop bucket or submit to `ScoopInstaller/Extras`.
  Users install via: `scoop install pdfchemy`

---

## 2. 🐧 Linux Digital Stores

### A. Flathub (The Universal Linux App Store)
Flathub is the standard app store for Fedora, Steam Deck, Ubuntu, Arch Linux, Linux Mint, and Debian.

#### Submission Status:
1. **Current Live PR:** [flathub/flathub #10071](https://github.com/flathub/flathub/pull/10071) (`Add io.github.kiss2oblivion.PDFchemy`).
2. Prepared manifest: `packaging/flathub/io.github.kiss2oblivion.PDFchemy.yaml` (utilizing official `PDFchemy-universal-1.0.0.jar` binary).
3. Once merged by the Flathub team, the app repository will be initialized under `https://github.com/flathub/io.github.kiss2oblivion.PDFchemy` and published across all Linux app stores (GNOME Software, KDE Discover, flathub.org).
   Users install via: `flatpak install flathub io.github.kiss2oblivion.PDFchemy`


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
