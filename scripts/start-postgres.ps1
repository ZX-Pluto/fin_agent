$ErrorActionPreference = 'Stop'

if (-not (Test-Path 'D:\A_app\pgsql\data')) {
    & 'D:\A_app\pgsql\bin\initdb.exe' -D 'D:\A_app\pgsql\data' -U postgres -E UTF8 --locale=C -A trust
}

& 'D:\A_app\pgsql\bin\pg_ctl.exe' -D 'D:\A_app\pgsql\data' -l 'D:\A_app\pgsql\pg.log' start

$exists = & 'D:\A_app\pgsql\bin\psql.exe' -U postgres -h localhost -tAc "SELECT 1 FROM pg_database WHERE datname='fin_agent'"
if ($exists -ne '1') {
    & 'D:\A_app\pgsql\bin\psql.exe' -U postgres -h localhost -c "CREATE DATABASE fin_agent;"
}
