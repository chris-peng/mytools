@echo 请将libs文件夹加入到path环境变量中（aapt）
@echo 打包成功后，apk包位于源文件夹disk目录下！
@echo off
%~dp0libs\apktool b %1
