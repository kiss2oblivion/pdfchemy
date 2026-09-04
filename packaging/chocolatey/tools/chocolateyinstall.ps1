$ErrorActionPreference = 'Stop'

$packageArgs = @{
  packageName   = 'pdfchemy'
  fileType      = 'msi'
  url64         = 'https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.1/PDFchemy-windows-x64-1.0.1.msi'
  checksum64    = '9a2f539818003e7248c2ff231df8bc25a2cdc069345f659720f747c65a2a81d0'
  checksumType64= 'sha256'
  silentArgs    = '/qn /norestart'
  validExitCodes= @(0, 3010)
  softwareName  = 'PDFchemy*'
}

Install-ChocolateyPackage @packageArgs
