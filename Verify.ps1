param([string]$Java = 'java')
$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot 'EchoServer.jar'
function Assert($condition, $message) {
    if (-not $condition) { throw $message }
}
function New-Client($port) {
    $client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', $port)
    $stream = $client.GetStream()
    $stream.ReadTimeout = 5000
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    $reader = New-Object System.IO.StreamReader($stream, $utf8)
    $writer = New-Object System.IO.StreamWriter($stream, $utf8)
    $writer.AutoFlush = $true
    return @{ Client = $client; Reader = $reader; Writer = $writer; Stream = $stream }
}
function Start-Server($mode, $timestamp) {
    $probe = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 0)
    $probe.Start()
    $port = $probe.LocalEndpoint.Port
    $probe.Stop()
    $arguments = '-jar "{0}" --port {1} --mode {2} --seconds 0.2 --text Test' -f $jar, $port, $mode
    if ($timestamp) { $arguments += ' --timestamp' }
    $process = Start-Process -FilePath $Java -ArgumentList $arguments -WindowStyle Hidden -PassThru
    try {
        for ($i = 0; $i -lt 50; $i++) {
            try { $client = New-Client $port; return @{ Process = $process; Client = $client; Port = $port } }
            catch { if ($process.HasExited) { throw 'Server beendet' }; Start-Sleep -Milliseconds 100 }
        }
        throw 'Server startet nicht'
    } catch { if (-not $process.HasExited) { $process.Kill() }; throw }
}

$server = Start-Server 'echo' $false
try {
    $a = $server.Client
    $b = New-Client $server.Port
    try {
        $a.Writer.Write('Teil')
        Start-Sleep -Milliseconds 300
        Assert (-not $a.Stream.DataAvailable) 'Echo vor Zeilenende'
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $a.Writer.WriteLine('text äöü')
        $b.Writer.WriteLine('zweiter Client')
        Assert ($a.Reader.ReadLine() -eq 'Teiltext äöü') 'Text/UTF-8 verändert'
        Assert ($watch.ElapsedMilliseconds -ge 150) 'Verzögerung fehlt'
        Assert ($b.Reader.ReadLine() -eq 'zweiter Client') 'Zweiter Client fehlgeschlagen'
        $a.Writer.WriteLine('')
        $a.Writer.WriteLine('danach')
        Assert ($a.Reader.ReadLine() -ceq '') 'Leere Nachricht fehlgeschlagen'
        Assert ($a.Reader.ReadLine() -eq 'danach') 'Reihenfolge fehlgeschlagen'
    } finally { $b.Client.Close() }
} finally { $server.Client.Client.Close(); $server.Process.Kill(); $server.Process.WaitForExit() }

foreach ($mode in @('echo', 'periodic')) {
    $server = Start-Server $mode $true
    try {
        if ($mode -eq 'echo') { $server.Client.Writer.WriteLine('Test') }
        $line = $server.Client.Reader.ReadLine()
        Assert ($line -match '^\[([^\]]+Z)\] Test$') 'Zeitstempel/Text fehlt'
        $null = [DateTimeOffset]::Parse($Matches[1])
        if ($mode -eq 'periodic') {
            $watch = [Diagnostics.Stopwatch]::StartNew()
            $line = $server.Client.Reader.ReadLine()
            Assert ($line -match '^\[.+Z\] Test$') 'Periodische Wiederholung fehlt'
            Assert ($watch.ElapsedMilliseconds -ge 100) 'Sendeintervall fehlt'
        }
    } finally { $server.Client.Client.Close(); $server.Process.Kill(); $server.Process.WaitForExit() }
}
& $Java -jar $jar --port 0 2>&1 | Out-Null
Assert ($LASTEXITCODE -eq 1) 'Ungueltiger Port akzeptiert'
& $Java -jar $jar --mode periodic --seconds 0 2>&1 | Out-Null
Assert ($LASTEXITCODE -eq 1) 'Ungueltiges Intervall akzeptiert'
Write-Host 'OK: Echo, UTF-8, Zeilenrahmung, mehrere Clients, Reihenfolge, Verzögerung, Zeitstempel, periodisches Senden und Eingabeprüfung.'
