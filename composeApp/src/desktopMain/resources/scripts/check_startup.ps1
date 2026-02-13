param(
    [string]$AppName
)

$existingTask = Get-ScheduledTask -TaskName $AppName -ErrorAction SilentlyContinue

if ($existingTask) {
    $taskState = $existingTask.State
    $taskEnabled = $existingTask.Settings.Enabled
    
    if ($taskState -eq "Ready" -and $taskEnabled -eq $true) {
        Write-Output "True"
    } else {
        Write-Output "False"
    }
} else {
    Write-Output "False"
}