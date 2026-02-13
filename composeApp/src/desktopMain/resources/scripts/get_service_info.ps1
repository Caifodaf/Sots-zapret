param(
    [string]$ServiceName
)

$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Output "Service not found"
    exit 0
}

Write-Output "Status: $($service.Status)"
Write-Output "DisplayName: $($service.DisplayName)"
Write-Output "Name: $($service.Name)"

$svc = Get-WmiObject -Class Win32_Service -Filter "Name='$ServiceName'"
Write-Output "WMI BinaryPathName: $($svc.PathName)" 