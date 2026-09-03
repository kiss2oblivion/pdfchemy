$ErrorActionPreference = 'Stop'

$packageArgs = @{
  packageName   = 'pdfchemy'
  fileType      = 'msi'
  url64         = 'https://github.com/kiss2oblivion/pdfchemy/releases/download/v1.0.0/PDFchemy-windows-x64-1.0.0.msi'
  silentArgs    = '/qn /norestart'
  validExitCodes= @(0, 3010)
  softwareName  = 'PDFchemy*'
}

Install-ChocolateyPackage @packageArgs
