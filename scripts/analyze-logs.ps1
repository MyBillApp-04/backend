param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$events = Get-Content -LiteralPath $Path |
    ForEach-Object {
        try { $_ | ConvertFrom-Json } catch { $null }
    } |
    Where-Object { $_ -ne $null }

$requests = @($events | Where-Object { $_.message -like 'http_request*' })
$errors = @($events | Where-Object { $_.level -in @('ERROR', 'FATAL') })

[pscustomobject]@{
    JsonEvents = @($events).Count
    HttpRequests = $requests.Count
    Errors = $errors.Count
    CorrelatedErrors = @($errors | Where-Object requestId).Count
} | Format-List

$errors |
    Select-Object -First 20 '@timestamp', requestId, logger_name, message |
    Format-Table -AutoSize
