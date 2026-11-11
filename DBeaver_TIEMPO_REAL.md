# Como ver la base de datos en DBeaver mientras funciona el colegio

**No necesitas instalar MySQL.** Esta version usa tu misma base H2 por un servidor TCP **local** incluido en Spring Boot.

1. Deten el servidor y cierra las conexiones de DBeaver antes de cambiar la version del proyecto.
2. Respalda la carpeta `data` de tu colegio anterior; al instalar el nuevo ZIP, copiala completa al nuevo proyecto.
3. Ejecuta en PowerShell: `cd C:\java2\colegio-backend` y luego `mvn clean verify` y `mvn spring-boot:run`.
4. En DBeaver **crea una conexion nueva H2 Server** (NO H2 Embedded) con:

   - JDBC URL: `jdbc:h2:tcp://localhost:9092/colegio`
   - Host: `localhost` y puerto: `9092`
   - Nombre de la base (Database path en modo Server): `colegio`
   - Usuario: `sa`
   - Contrasena: vacia (si no la configuraste)
   - Controlador: version H2 compatible, normalmente `2.3.232` con este proyecto

5. Prueba conexion y abre `PUBLIC > Tables > ESTUDIANTES > Data`.
6. Registra un estudiante en la pagina `http://localhost:8080/`.
7. Pulsa `F5` en DBeaver para ver el nuevo registro. Puedes activar la actualizacion automatica si tu DBeaver la ofrece.

**Si aparece `Database may be already in use`:** estas usando la conexion antigua **H2 Embedded**; usa H2 Server con la direccion `tcp://localhost:9092/colegio`.

**Si aparece `Connection refused` o `Connection is broken`:** comprueba que Spring Boot ya termino de arrancar, que el puerto 9092 esta libre y que DBeaver usa la misma version del driver H2 que Spring Boot. El servidor TCP solo escucha en localhost. No lo expongas a internet.

**Si aparece `Database not found`:** inicia Spring Boot desde la carpeta del proyecto que contiene `pom.xml` y confirma que `data/colegio.mv.db` esta ahi. No crees una base nueva por error.

**Si aparece `BUILD FAILURE`:** revisa el error de Maven. No reemplaces la carpeta original ni borres la copia de seguridad hasta comprobar que arranca.
