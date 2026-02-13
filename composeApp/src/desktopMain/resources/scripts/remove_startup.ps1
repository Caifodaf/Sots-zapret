param(
    [string]$AppName
)

$existingTask = Get-ScheduledTask -TaskName $AppName -ErrorAction SilentlyContinue

if ($existingTask) {
    Unregister-ScheduledTask -TaskName $AppName -Confirm:$false -ErrorAction SilentlyContinue
}