# Load environment variables from .env file and run Spring Boot app

$envPath = Join-Path (Get-Location) ".env"

if (-not (Test-Path $envPath)) {
    Write-Host "❌ ERROR: .env file not found!" -ForegroundColor Red
    Write-Host "Please create .env from .env.example:"
    Write-Host "  cp .env.example .env"
    exit 1
}

Write-Host "📂 Loading environment from .env file..." -ForegroundColor Cyan
$envContent = Get-Content $envPath | Where-Object { $_ -notmatch '^\s*#' -and $_ -notmatch '^\s*$' }

$loadedVars = 0
foreach ($line in $envContent) {
    $parts = $line -split '=', 2
    if ($parts.Count -eq 2) {
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        Set-Item -Path "Env:$key" -Value $value
        $loadedVars++
    }
}
Write-Host "✅ Loaded $loadedVars environment variables" -ForegroundColor Green

# Verify critical variables
Write-Host "`n🔍 Verifying critical variables..." -ForegroundColor Cyan
$criticalVars = @('APP_DB_URL', 'APP_DB_USERNAME', 'APP_DB_PASSWORD', 'APP_COACH_DEFAULT_EMAIL')
$missing = @()

foreach ($var in $criticalVars) {
    $value = [Environment]::GetEnvironmentVariable($var)
    if ($value) {
        $displayValue = if ($var -match "PASSWORD|EMAIL") { "***" } else { $value }
        Write-Host "  ✓ $var = $displayValue" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $var is missing!" -ForegroundColor Red
        $missing += $var
    }
}

if ($missing.Count -gt 0) {
    Write-Host "`n❌ Missing required variables: $($missing -join ', ')" -ForegroundColor Red
    Write-Host "Please update .env file" -ForegroundColor Yellow
    exit 1
}

# Run the app
Write-Host "`n🚀 Starting Spring Boot application..." -ForegroundColor Cyan
.\mvnw spring-boot:run
