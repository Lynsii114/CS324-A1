$RepoRoot = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $RepoRoot

javac -d Backend/out (Get-ChildItem -Recurse Backend/src/main/java -Filter *.java).FullName
