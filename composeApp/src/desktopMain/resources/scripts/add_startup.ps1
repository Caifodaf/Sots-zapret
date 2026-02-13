param(
    [string]$AppName,
    [string]$ExePath,
    [string]$Description
)

$existingTask = Get-ScheduledTask -TaskName $AppName -ErrorAction SilentlyContinue

if ($existingTask) {
    Unregister-ScheduledTask -TaskName $AppName -Confirm:$false -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

if (-not (Test-Path $ExePath)) {
    exit 1
}

try {
    $action = New-ScheduledTaskAction -Execute $ExePath -Argument "--autostart"
    $trigger = New-ScheduledTaskTrigger -AtLogOn
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable
    $principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Highest

    $task = New-ScheduledTask -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description $Description
    Register-ScheduledTask -TaskName $AppName -InputObject $task -Force

    Start-Sleep -Seconds 2
    $createdTask = Get-ScheduledTask -TaskName $AppName -ErrorAction SilentlyContinue

    if ($createdTask) {
        Write-Output "SUCCESS"
    } else {
        Write-Output "ERROR: Task not created"
        exit 1
    }
} catch {
    Write-Output "ERROR: $($_.Exception.Message)"
    exit 1
}
