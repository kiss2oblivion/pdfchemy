# Microsoft Store Submission Guide for PDFchemy Tools

Microsoft Store officially supports two publishing methods for desktop applications:

---

## Method 1: Win32 Desktop App (Fastest & Recommended by Microsoft)

Microsoft allows publishing existing Win32 `.exe` or `.msi` installers directly to the Microsoft Store without rewriting or repackaging.

### Steps:
1. **Log in to Partner Center:**
   * Go to [partner.microsoft.com/dashboard/apps/overview](https://partner.microsoft.com/dashboard/apps/overview).
2. **Create New App:**
   * Click **New product** -> **Windows & Xbox app**.
   * Reserve the product name: `PDFchemy Tools` (or `PDFchemy`).
3. **Select Win32 App Distribution:**
   * When asked for package type, select **Desktop app (Win32)**.
4. **Provide Installer URL:**
   * Enter the GitHub Release URL of the `.msi` or `.exe` installer:
     `https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.0/PDFchemy-1.0.0.msi`
   * Silent install command: `/qn` (for MSI) or `/VERYSILENT` (for Inno/standard EXE).
5. **Add Store Listing & Privacy Details:**
   * Privacy policy URL: `https://github.com/kiss2oblivion/pdfchemy/blob/main/PRIVACY.md`
   * Category: **Productivity** / **Document Management**
   * Pricing: **Free**
6. **Submit for Certification:**
   * Microsoft tests the installer URL and publishes the listing within 24–48 hours.

---

## Method 2: MSIX Package via MSIX Packaging Tool

If you prefer a signed `.msixupload` bundle:

1. **Install MSIX Packaging Tool** from Microsoft Store.
2. Build the local Windows distribution:
   ```powershell
   $env:JAVA_HOME="E:\Android_Studio\jbr"
   .\gradlew :desktop:createDistributable
   ```
3. Run the Packaging Tool pointing to the template `packaging/msstore/AppxManifest.xml` and the binaries in `desktop/build/compose/binaries/main/app`.
4. Upload the generated `.msixupload` file to Partner Center.
