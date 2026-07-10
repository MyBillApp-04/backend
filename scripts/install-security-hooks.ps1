$ErrorActionPreference = 'Stop'

if (-not (Get-Command git-secrets -ErrorAction SilentlyContinue)) {
    throw 'git-secrets is not installed. Install it from https://github.com/awslabs/git-secrets first.'
}

git secrets --install -f
git secrets --register-aws
git secrets --add '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
git secrets --add '(gh[pousr]_[A-Za-z0-9_]{30,}|sk_live_[A-Za-z0-9]{20,})'
git secrets --scan

Write-Host 'git-secrets hooks installed and the current Backend tree passed.'
