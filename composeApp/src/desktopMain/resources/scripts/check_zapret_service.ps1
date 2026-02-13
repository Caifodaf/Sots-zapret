$serviceName = $args[0]
try {
    $s = Get-Service -Name $serviceName -ErrorAction Stop
    Write-Output $s.Status
} catch {
    Write-Output 'NOT_FOUND'
} 