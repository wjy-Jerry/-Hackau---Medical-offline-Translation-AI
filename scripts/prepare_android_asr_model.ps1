$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$target = Join-Path $root 'android/app/src/main/assets/models/ggml-base-q5_1.bin'
$expected = '422F1AE452ADE6F30A004D7E5C6A43195E4433BC370BF23FAC9CC591F01A8898'

if ((Test-Path -LiteralPath $target) -and
    (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -eq $expected) {
    Write-Host 'Bundled multilingual Whisper model is already present and verified.'
    exit 0
}

$directory = Split-Path -Parent $target
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$partial = "$target.part"
try {
    Invoke-WebRequest -Uri 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin' -OutFile $partial
    $actual = (Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash
    if ($actual -ne $expected) { throw "ASR model checksum mismatch: $actual" }
    Move-Item -LiteralPath $partial -Destination $target -Force
    Write-Host "Verified multilingual Whisper model: $target"
} finally {
    if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial }
}
