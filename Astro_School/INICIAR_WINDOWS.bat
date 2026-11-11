@echo off
cd /d "%~dp0"
chcp 65001 >nul
echo =============================================================
echo     ASTRO SCHOOL 3.0 - Portal ^& Swagger API
echo =============================================================
echo Portal:  http://localhost:8080/
echo Swagger: http://localhost:8080/swagger-ui.html
echo Noticias: abiertas a visitantes (modo local / demo)
echo Cursos: Robotica, Fisica y Astronomia (1 inscripcion/alumno)
echo Laboratorios: robots, editor, experimentos y astronomia
echo Galeria publica: Nuestras creaciones
echo Datos: data\colegio.mv.db + data\noticias\ + data\creaciones\
echo.
echo NO CIERRES ESTA VENTANA mientras el servidor este en uso.
echo.
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: No se encontro Java 17 o superior en PATH.
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo ERROR: No se encontro Maven en PATH. Consulta README.md
  pause
  exit /b 1
)
mvn spring-boot:run
if errorlevel 1 echo ERROR: Revisar salida anterior y README.md
pause
