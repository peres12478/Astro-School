# Correcciones aplicadas al proyecto escolar

## Error principal corregido

En la entrega anterior existian **dos conjuntos de controladores** (`controller/` y `web/`), con clases `AuthController`, `ProfesorController` y `EstudianteController` duplicadas. Spring intenta crear componentes con nombres como `authController` en ambos paquetes, produciendo un conflicto al arrancar el `ApplicationContext`. Tambien estaban duplicadas rutas como `POST /api/auth/login` y `POST /api/profesores/qr/escanear`, que causarian mapeos ambiguos.

## Cambios

1. Se unificaron todos los controladores REST en `src/main/java/com/ejemplo/colegio/controller/`.
2. Se eliminaron las clases duplicadas del paquete `web/`, incluido el segundo manejador global de errores. Permanece `GlobalExceptionHandler`.
3. Se mantuvieron como controladores separados `NotaController`, `TrabajoController` y `ClaseVirtualController` para sus rutas `/api/notas`, `/api/trabajos` y `/api/clases`.
4. Se conservaron las **rutas originales y alternativas** mediante alias de anotaciones, para no romper los dos conjuntos de pruebas existentes. Ejemplo: `/api/auth/registro/profesor` y `/api/auth/registro/profesores` invocan el mismo metodo.
5. Ambos archivos de pruebas de integracion usan ahora un `JavaMailSender` simulado para no depender de SMTP durante Maven Test.
6. Se agrego una comprobacion de que `/v3/api-docs` publique la especificacion Swagger.
7. Se agregaron `VERIFICAR_WINDOWS.bat` y `scripts/comprobar_estructura.py` para facilitar la verificacion.
8. Se verifico que Spring Boot 3.5.16 y springdoc 2.10.0 son versiones compatibles; se conservaron, sin realizar cambios innecesarios en `pom.xml`.

## Importante al reemplazar la entrega anterior

**NO** mezcles los nuevos archivos con los antiguos. Si dejas `src/main/java/com/ejemplo/colegio/web/AuthController.java` o los `.class` antiguos en `target/classes`, reapareceran los errores de Spring. Renombra la carpeta anterior, extrae esta entrega en una carpeta limpia y ejecuta `mvn clean verify`.

## Comprobaciones y limitaciones

- La revision estructural valida que no haya nombres de componentes ni rutas REST duplicadas en el codigo fuente.
- La compilacion, el arranque real de Spring/Hibernate y las pruebas Maven **requieren ejecutarse** en un ambiente con Maven y dependencias disponibles. La verificacion estatica por si sola no garantiza que no aparezcan otros errores de configuracion o del entorno.
- Si falla Maven, abre `target/surefire-reports/` y busca la **primera** linea `Caused by:`. Los tres mensajes `Failed to load ApplicationContext` del resumen no muestran la causa exacta anidada.
