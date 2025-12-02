@echo off
REM Ejecuta la aplicación de procesamiento de pedidos con Spring Boot
cd /d "%~dp0"
call mvn spring-boot:run

