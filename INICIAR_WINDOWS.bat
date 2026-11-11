@echo off
cd /d "%~dp0"
echo =============================================================
echo   COLEGIO - PANEL EN ESPANOL
echo =============================================================
echo Al iniciar, abre http://localhost:8080/
echo Swagger avanzado: http://localhost:8080/swagger-ui.html
echo DEMOSTRACION: puedes elegir entre 2 docentes y 2 estudiantes al abrir la pagina.
echo Inicio normal: correo O documento mas contrasena.
echo DBeaver H2 Server: jdbc:h2:tcp://localhost:9092/colegio
echo NO CIERRES ESTA VENTANA MIENTRAS USES EL SISTEMA.
echo.
mvn spring-boot:run
if errorlevel 1 echo Asegurate de tener Java 17+ y Maven en el PATH.
pause
