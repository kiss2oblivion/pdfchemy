$ErrorActionPreference = 'Stop'

$packageArgs = @{
  packageName   = 'pdfchemy'
  fileType      = 'msi'
  url64         = 'https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.2/PDFchemy-windows-x64-1.0.2.msi'
  checksum64    = 'a57e9a2e09c6fd0b8161e409c95bd15b36b23f6062c46f41c52ce4bd9fe5bad7'
  checksumType64= 'sha256'
  silentArgs    = '/qn /norestart'
  validExitCodes= @(0, 3010)
  softwareName  = 'PDFchemy*'
}

Install-ChocolateyPackage @packageArgs
