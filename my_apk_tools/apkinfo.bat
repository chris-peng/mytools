@echo off
%~dp0libs\aapt dump badging %1 | findstr "application-label package launchable"

