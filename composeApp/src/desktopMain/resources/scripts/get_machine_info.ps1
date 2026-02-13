# Получение Machine GUID
$machineGuid = (Get-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Cryptography' -Name 'MachineGuid').MachineGuid

# Получение имени пользователя
$userName = $env:USERNAME

# Формируем результат в виде JSON
$result = @{ MachineGuid = $machineGuid; UserName = $userName } | ConvertTo-Json -Compress

Write-Output $result 