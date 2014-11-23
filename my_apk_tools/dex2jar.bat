@echo off
%~dp0libs\dex2jar\dex2jar classes.dex|pause
%~dp0libs\jd-gui classes_dex2jar.jar
