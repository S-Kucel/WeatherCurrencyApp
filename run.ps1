param([ValidateSet('Run', 'Build', 'Test', 'CheckApi')][string]$Action = 'Run')
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$toolsDirectory = Join-Path $projectRoot '.tools'
$mavenVersion = '3.9.16'
$mavenChecksum = 'ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3'

if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $javaBin = Join-Path $env:JAVA_HOME 'bin'
} else {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if (-not $compiler) { throw 'Zainstaluj JDK 21 lub nowsze i ustaw JAVA_HOME lub PATH.' }
    $javaBin = Split-Path -Parent $compiler.Source
}
$env:JAVA_HOME = Split-Path -Parent $javaBin
$javaVersion = (& (Join-Path $javaBin 'javac.exe') -version | Out-String)
if ($javaVersion -notmatch 'javac (?<major>\d+)' -or [int]$Matches['major'] -lt 21) {
    throw 'Ten projekt wymaga JDK 21 lub nowszego.'
}

$maven = Join-Path $toolsDirectory "apache-maven-$mavenVersion\bin\mvn.cmd"
if (-not (Test-Path -LiteralPath $maven)) {
    New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
    $archive = Join-Path $toolsDirectory "apache-maven-$mavenVersion-bin.zip"
    $download = "$archive.download"
    Write-Host 'Pierwsze uruchomienie: pobieranie Apache Maven...'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $previousProgress = $ProgressPreference
    try {
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip" -OutFile $download
        if ((Get-FileHash -LiteralPath $download -Algorithm SHA512).Hash.ToLowerInvariant() -ne $mavenChecksum) {
            throw 'Suma kontrolna pobranego Maven jest nieprawidlowa. Sprobuj ponownie.'
        }
        Move-Item -LiteralPath $download -Destination $archive -Force
        Expand-Archive -LiteralPath $archive -DestinationPath $toolsDirectory -Force
    } finally {
        $ProgressPreference = $previousProgress
    }
}
$mavenArguments = @('-B', '-ntp', '-f', (Join-Path $projectRoot 'pom.xml'), "-Dmaven.repo.local=$(Join-Path $toolsDirectory 'repository')")
switch ($Action) {
    'Run' { $mavenArguments += @('compile', 'javafx:run') }
    'Build' { $mavenArguments += @('clean', 'package') }
    'Test' { $mavenArguments += @('test') }
    'CheckApi' { $mavenArguments += @('compile', 'exec:java', '-Dexec.mainClass=pl.weathercurrency.ApiCheck') }
}
& $maven @mavenArguments
if ($LASTEXITCODE -ne 0) { throw "Operacja $Action nie powiodla sie (kod $LASTEXITCODE)." }
