param(
    [string]$ExePath,
    [int]$DelaySeconds = 3
)

Start-Sleep -Seconds $DelaySeconds
Start-Process -FilePath $ExePath -Verb runAs 