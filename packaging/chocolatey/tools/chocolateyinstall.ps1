$ErrorActionPreference = 'Stop'

$packageArgs = @{
  packageName   = 'pdfchemy'
  fileType      = 'msi'
  url64         = 'https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.0/PDFchemy-windows-x64-1.0.0.msi'
  checksum64    = '6f80fe067e0fcf3d22211e619e867b856d9f81753fcc8a9da2f25cf7b453ed99'
  checksumType64= 'sha256'
  silentArgs    = '/qn /norestart'
  validExitCodes= @(0, 3010)
  softwareName  = 'PDFchemy*'
}

Install-ChocolateyPackage @packageArgs
