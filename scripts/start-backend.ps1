$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = 'D:\A_app\jdk-21.0.12+8'
$env:Path = "$env:JAVA_HOME\bin;D:\A_app\apache-maven-3.9.9\bin;$env:Path"
Set-Location 'D:\A_code\AI\fin_agent\backend'

mvn -B -q -DskipTests package
java -jar 'target\ai-material-backend-0.1.0-SNAPSHOT.jar'
