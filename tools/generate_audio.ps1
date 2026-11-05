$ErrorActionPreference = 'Stop'
# Original exercise vocabulary. Runtime playback uses these bundled WAVs, never system TTS.
$workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputDirectory = [IO.Path]::GetFullPath((Join-Path $workspace 'app/src/main/assets/audio'))
if (-not $outputDirectory.StartsWith($workspace + [IO.Path]::DirectorySeparatorChar)) { throw 'Output must stay inside the project' }
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$voice = New-Object -ComObject SAPI.SpVoice
$spanish = @($voice.GetVoices() | Where-Object { $_.GetDescription() -like '*Sabina*' })
if ($spanish.Count -ne 1) { throw 'Install the local Microsoft Sabina Spanish (Mexico) voice before regenerating audio' }
$voice.Voice = $spanish[0]
$voice.Rate = -2
foreach ($word in @('mano','mesa','sol','luna','sapo','pato','lobo')) {
    $stream = New-Object -ComObject SAPI.SpFileStream
    try {
        $stream.Open((Join-Path $outputDirectory ($word + '.wav')), 3, $false)
        $voice.AudioOutputStream = $stream
        [void]$voice.Speak($word, 0)
    } finally { $stream.Close() }
}
Get-ChildItem -LiteralPath $outputDirectory -Filter '*.wav' | Select-Object Name,Length
