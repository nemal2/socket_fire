@echo off
echo Creating certificates for Socket Fire...

REM Generate server keystore
keytool -genkeypair -alias serverkey -keyalg RSA -keysize 2048 ^
  -validity 365 -keystore server.keystore ^
  -storepass changeit -keypass changeit ^
  -dname "CN=localhost, OU=Development, O=YourOrg, L=YourCity, ST=YourState, C=US"

REM Export server certificate
keytool -exportcert -alias serverkey -keystore server.keystore ^
  -storepass changeit -file server.cer

REM Generate client truststore and import server certificate
keytool -importcert -alias serverkey -file server.cer ^
  -keystore client.truststore -storepass changeit ^
  -noprompt

echo Certificate creation complete!
pause