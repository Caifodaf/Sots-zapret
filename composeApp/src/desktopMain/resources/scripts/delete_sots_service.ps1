param(
    [string]$ServiceName
)

# Проверяем, существует ли служба
$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Output "Service not found"
    exit 0
}

# Останавливаем службу, если она запущена
if ($service.Status -eq 'Running') {
    Stop-Service -Name $ServiceName -Force
    Write-Output "Service stopped"
}

# Удаляем службу
sc.exe delete "$ServiceName"
Write-Output "Service deleted" 