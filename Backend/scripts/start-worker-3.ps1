$RepoRoot = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $RepoRoot

java -cp Backend/out cs324.worker.WorkerNode 3 5003
