# Installs DatabaseSchedulerExecutor as a Windows scheduled task (runs at startup as SYSTEM).
# Run from the folder containing the built jar + app.properties, as Administrator.
# Uninstall: schtasks /Delete /TN DatabaseSchedulerExecutor /F
param(
    [string]$InstallDir = "$env:ProgramFiles\DatabaseSchedulerExecutor"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Jar = Get-ChildItem -Path $ScriptDir -Filter "DatabaseScheduleExecutor-*.jar" | Select-Object -First 1
$Config = Join-Path $ScriptDir "app.properties"

if (-not $Jar) {
    Write-Error "no DatabaseScheduleExecutor-*.jar found in $ScriptDir"
    exit 1
}
if (-not (Test-Path $Config)) {
    Write-Error "app.properties not found in $ScriptDir"
    exit 1
}
$IsAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $IsAdmin) {
    Write-Error "run as Administrator"
    exit 1
}

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Copy-Item $Jar.FullName -Destination $InstallDir -Force
Copy-Item $Config -Destination $InstallDir -Force

$JarPath = Join-Path $InstallDir $Jar.Name
$ConfigPath = Join-Path $InstallDir "app.properties"
icacls $ConfigPath /inheritance:r /grant:r "SYSTEM:F" "Administrators:F" | Out-Null
$Command = "java -jar `"$JarPath`" --config `"$ConfigPath`""

schtasks /Create /TN "DatabaseSchedulerExecutor" /TR $Command /SC ONSTART /RU SYSTEM /RL HIGHEST /F | Out-Null
schtasks /Run /TN "DatabaseSchedulerExecutor" | Out-Null

Write-Host "installed to $InstallDir, status: schtasks /Query /TN DatabaseSchedulerExecutor"
# ponytail: runs as SYSTEM (no dedicated service account), add one if hardening needed
