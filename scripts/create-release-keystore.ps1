[CmdletBinding()]
param(
    [string]$KeystorePath = (Join-Path $PSScriptRoot "..\tibo-button-release.jks"),
    [string]$Alias = "tibo-button",
    [int]$ValidityDays = 10000
)

$ErrorActionPreference = "Stop"

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    throw "keytool was not found. Install a JDK or add Android Studio's bundled JDK bin folder to PATH."
}

$fullKeystorePath = [System.IO.Path]::GetFullPath($KeystorePath)
$base64Path = "$fullKeystorePath.base64"

if (Test-Path $fullKeystorePath) {
    throw "Refusing to overwrite existing keystore: $fullKeystorePath"
}
if (Test-Path $base64Path) {
    throw "Refusing to overwrite existing Base64 file: $base64Path"
}

$parent = Split-Path -Parent $fullKeystorePath
New-Item -ItemType Directory -Path $parent -Force | Out-Null

Write-Host "Generating release keystore. keytool will ask for passwords and certificate details." -ForegroundColor Cyan
& $keytool.Source `
    -genkeypair `
    -v `
    -keystore $fullKeystorePath `
    -alias $Alias `
    -keyalg RSA `
    -keysize 4096 `
    -validity $ValidityDays

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $fullKeystorePath)) {
    throw "keytool did not create the release keystore."
}

$bytes = [System.IO.File]::ReadAllBytes($fullKeystorePath)
$base64 = [Convert]::ToBase64String($bytes)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($base64Path, $base64, $utf8NoBom)

Write-Host ""
Write-Host "Created:" -ForegroundColor Green
Write-Host "  $fullKeystorePath"
Write-Host "  $base64Path"
Write-Host ""
Write-Warning "Back up the .jks file and passwords securely. Never commit either generated file."
Write-Host "Use the complete .base64 file contents for GitHub secret RELEASE_KEYSTORE_BASE64."
