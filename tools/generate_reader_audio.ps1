$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputDirectory=Join-Path $workspace 'app/src/main/assets/audio'
$voice=New-Object -ComObject SAPI.SpVoice
$voices=$voice.GetVoices()
$spanish=$null; $english=$null
for($i=0;$i -lt $voices.Count;$i++) {
    $item=$voices.Item($i);$description=$item.GetDescription()
    if($description -like '*Sabina*'){$spanish=$item}
    if($description -like '*David*'){$english=$item}
}
if($null -eq $spanish -or $null -eq $english){throw 'Spanish Sabina and English David voices are required'}
$entries=Get-Content (Join-Path $PSScriptRoot 'reader_audio.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach($property in $entries.PSObject.Properties) {
    if($property.Name -notmatch '^read-[a-z0-9-]+$'){throw 'Invalid audio key'}
    $voice.Voice=if($property.Value.language -eq 'es'){$spanish}else{$english}
    $voice.Rate=-2
    $destination=Join-Path $outputDirectory ($property.Name+'.wav')
    if(Test-Path -LiteralPath $destination){continue}
    $stream=New-Object -ComObject SAPI.SpFileStream
    try{$stream.Open($destination,3,$false);$voice.AudioOutputStream=$stream;[void]$voice.Speak($property.Value.text,0)}finally{$stream.Close()}
}
Write-Output 'Reader audio generated locally.'
