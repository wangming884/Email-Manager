$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendDir = Join-Path $root "frontend"
$backendDir = Join-Path $root "backend"
$distDir = Join-Path $root "dist"
$appImageDir = Join-Path $distDir "MailManager"
$installerDir = Join-Path $distDir "installer"
$jarPath = Join-Path $backendDir "target\\mail-manager.jar"
$mavenSettings = Join-Path $root "settings-central.xml"
$mainClass = "org.springframework.boot.loader.launch.JarLauncher"

Write-Host "[1/4] Building frontend..."
Push-Location $frontendDir
try {
    npm run build
} finally {
    Pop-Location
}

Write-Host "[2/4] Packaging Spring Boot app..."
Push-Location $backendDir
try {
    mvn -q clean package -DskipTests -s $mavenSettings
} finally {
    Pop-Location
}

if (-not (Test-Path $jarPath)) {
    throw "Expected jar not found: $jarPath"
}

if (Test-Path $distDir) {
    Remove-Item -LiteralPath $distDir -Recurse -Force
}

New-Item -ItemType Directory -Path $distDir | Out-Null
New-Item -ItemType Directory -Path $installerDir | Out-Null

Write-Host "[3/4] Creating portable app image..."
jpackage `
  --type app-image `
  --name "MailManager" `
  --dest $distDir `
  --input (Join-Path $backendDir "target") `
  --main-jar "mail-manager.jar" `
  --main-class $mainClass `
  --app-version "1.0.0" `
  --vendor "OpenAI Codex" `
  --java-options "-Dspring.profiles.active=desktop" `
  --java-options "-Dfile.encoding=UTF-8" `
  --java-options "-Dsun.stdout.encoding=UTF-8" `
  --java-options "-Dsun.stderr.encoding=UTF-8"

Write-Host "[4/4] Creating Windows installer..."
$installerPath = Join-Path $installerDir "MailManager-1.0.0.exe"
try {
    jpackage `
      --type exe `
      --name "MailManager" `
      --dest $installerDir `
      --input (Join-Path $backendDir "target") `
      --main-jar "mail-manager.jar" `
      --main-class $mainClass `
      --app-version "1.0.0" `
      --vendor "OpenAI Codex" `
      --win-shortcut `
      --win-menu `
      --java-options "-Dspring.profiles.active=desktop" `
      --java-options "-Dfile.encoding=UTF-8" `
      --java-options "-Dsun.stdout.encoding=UTF-8" `
      --java-options "-Dsun.stderr.encoding=UTF-8"
} catch {
    Write-Warning "Installer generation failed, but the portable launcher is ready: $appImageDir\\MailManager.exe"
}

Write-Host ""
Write-Host "Portable launcher: $appImageDir\\MailManager.exe"
Write-Host "Installer: $installerPath"
