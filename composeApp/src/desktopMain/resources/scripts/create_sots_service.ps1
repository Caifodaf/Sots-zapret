param(
    [string]$ServiceName,
    [string]$ExecutablePath,
    [string]$Arguments
)

Write-Output "[DEBUG] ServiceName: $ServiceName"
Write-Output "[DEBUG] ExecutablePath: $ExecutablePath"
Write-Output "[DEBUG] Arguments: $Arguments"

# Проверяем, существует ли служба
$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($service) {
    Write-Output "Service already exists"
    exit 1
}

# Формируем правильный BinaryPathName
$binPath = '"' + $ExecutablePath + '"'
if ($Arguments -ne "") {
    $binPath += " " + $Arguments
}
Write-Output "BinaryPathName: $binPath"

# Создаём службу
New-Service -Name $ServiceName -BinaryPathName $binPath -DisplayName $ServiceName -StartupType Automatic

# Проверяем, что служба создана
Get-Service -Name $ServiceName

# Выводим реальные параметры запуска через WMI
$svc = Get-WmiObject -Class Win32_Service -Filter "Name='$ServiceName'"
Write-Output "WMI BinaryPathName: $($svc.PathName)"

# Запускаем службу
try {
    Start-Service -Name $ServiceName -ErrorAction Stop
    Write-Output "Service created and started"
} catch {
    Write-Output "ERROR: $($_.Exception.Message)"
} 