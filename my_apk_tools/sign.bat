@echo jarsigner程序由jdk提供
jarsigner.exe -keystore %~dp0libs\key.key -storepass 123456 -keypass 123456 -signedjar %2 %1 jar -digestalg SHA1 -sigalg MD5withRSA

