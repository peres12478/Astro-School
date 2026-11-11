@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo   COMPROBACION DEL BACKEND ESCOLAR - Spring Boot
 echo =============================================================
where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java no esta disponible en PATH. Necesitas JDK 17 o superior.
    exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven no esta disponible en PATH. Abre el proyecto en IntelliJ IDEA
    echo        o instala Maven 3.9+.
    exit /b 1
)
echo Limpiando clases anteriores y ejecutando todas las pruebas...
call mvn clean verify
if errorlevel 1 (
    echo.
    echo Las pruebas fallaron. Revisa target\surefire-reports\*.txt
    echo Busca la PRIMERA linea con 'Caused by:' en el reporte.
    exit /b 1
)
echo.
echo VERIFICACION CORRECTA. El JAR esta en la carpeta target.
exit /b 0
