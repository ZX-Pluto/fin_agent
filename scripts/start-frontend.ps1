$ErrorActionPreference = 'Stop'

$env:Path = "D:\A_app\node-v18.20.4-win-x64;$env:Path"
Set-Location 'D:\A_code\AI\fin_agent\frontend'

npm install
npm run dev
