[CmdletBinding()]
param(
    [string]$SecretsDir,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Split-Path -Parent $scriptDir

if ([string]::IsNullOrWhiteSpace($SecretsDir)) {
    $SecretsDir = [Environment]::GetEnvironmentVariable('MYBILL_SECRETS_DIR')
}
if ([string]::IsNullOrWhiteSpace($SecretsDir)) {
    $SecretsDir = Join-Path (Split-Path -Parent (Split-Path -Parent $backendDir)) 'Secrets'
}

$envPath = Join-Path $SecretsDir '.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Secrets env file not found: $envPath"
}

Get-Content -LiteralPath $envPath | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
        return
    }

    $match = [regex]::Match($line, '^\s*(?:export\s+)?([^#=\s]+)\s*=\s*(.*)\s*$')
    if (-not $match.Success) {
        return
    }

    $name = $match.Groups[1].Value.Trim()
    $value = $match.Groups[2].Value.Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($name, $value, [EnvironmentVariableTarget]::Process)
}

if ($MavenArgs.Count -eq 0) {
    $MavenArgs = @('spring-boot:run')
}

Push-Location $backendDir
try {
    & .\mvnw.cmd @MavenArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
