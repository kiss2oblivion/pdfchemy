$ErrorActionPreference = 'Stop'

$packageArgs = @{
  packageName   = 'pdfchemy'
  fileType      = 'msi'
  url64         = 'https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.1/PDFchemy-windows-x64-1.0.1.msi'
  checksum64    = 'c9dd052337e22ea4c0f560c40bc58cb3f67971c447bc2c9150fdcb4fce0377d1'
  checksumType64= 'sha256'
  silentArgs    = '/qn /norestart'
  validExitCodes= @(0, 3010)
  softwareName  = 'PDFchemy*'
}

Install-ChocolateyPackage @packageArgs
