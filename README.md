> **Versión más amigable (demo)**: antes de instalar, lee [GUIA_FACIL_DEMOSTRACION.md](GUIA_FACIL_DEMOSTRACION.md). Incluye cuatro usuarios ficticios, inicio con correo o identificación, avisos claros, QR compacto y pruebas sin registrar usuarios manualmente. **NO uses el modo demo con datos reales de menores**; desactívalo con `APP_DEMO_ENABLED=false`. El perfil H2 sigue funcionando con DBeaver por TCP local.

> **NUEVO: acceso simultaneo a H2 y DBeaver.** Usa `jdbc:h2:tcp://localhost:9092/colegio` con una conexion H2 Server mientras Spring Boot esta encendido. No utilices H2 Embedded en DBeaver al mismo tiempo. Swagger ahora muestra un solo registro de estudiantes; las URLs antiguas se conservan para compatibilidad. Consulta `GUIA_PANEL_ES.md`.

> **NUEVO: panel escolar en español.** Inicia el servidor con `mvn spring-boot:run` y visita **http://localhost:8080/** para usar el sistema desde una interfaz con formularios y botones. Swagger sigue disponible en http://localhost:8080/swagger-ui.html para pruebas avanzadas. Consulta **[GUIA_PANEL_ES.md](GUIA_PANEL_ES.md)** para saber cómo actualizar sin perder la base de datos y qué significan los IDs.

# Colegio Backend - API REST escolar (Java 17 + Spring Boot 3)

Aplicacion local con registro y login, autenticacion JWT, estudiantes/profesores, notas por materia, promedio, tareas por grupo, QR firmado, clases Jitsi, grabaciones y correo de recuperacion.

> **Servidor local**: configurado para escuchar en `127.0.0.1:8080`. El registro de docentes pide una clave administrativa (ver abajo). Este repositorio es un **prototipo funcional para pruebas locales**, no una plataforma escolar homologada para datos reales de menores.

## ATENCION: esta version corrige los errores de `ApplicationContext`

La entrega anterior tenia controladores duplicados en `controller/` y `web/`.
**No copies estos archivos encima de la carpeta antigua sin borrar la anterior**:
si conservas los `.java` viejos o `target/classes`, Spring seguira fallando.

En Windows, renombra `C:\java\colegio-backend` a `C:\java\colegio-backend-anterior`,
extrae el ZIP nuevo dentro de `C:\java` (se crea `C:\java\colegio-backend`)
y abre **esa carpeta nueva** en IntelliJ. Si ya tenias datos de H2, respalda
`data/colegio.mv.db` antes de mover o borrar la carpeta antigua y copialo
solo cuando lo necesites, con el servidor apagado.

Abre una terminal en `C:\java\colegio-backend` y ejecuta:

```powershell
mvn clean verify
```

Tambien puedes hacer doble clic en `VERIFICAR_WINDOWS.bat`. El comando `clean`
es imprescindible para eliminar `.class` anteriores. Si `verify` termina con
`BUILD SUCCESS`, usa `mvn spring-boot:run` y abre Swagger.

Ver [`CAMBIOS_CORREGIDOS.md`](CAMBIOS_CORREGIDOS.md) para conocer las correcciones.

## Requisitos

- JDK 17+ (IntelliJ IDEA permite escogerlo al importar Maven).
- Maven 3.9+ (o el Maven integrado de IntelliJ).
- Opcional: Docker Desktop para SMTP Mailpit / MySQL.

## Inicio rapido (H2, sin instalar MySQL)

1. Abre **la carpeta completa** `colegio-backend` en IntelliJ IDEA como proyecto Maven.
2. En `pom.xml` permite importar las dependencias desde Maven Central y usa JDK 17+.
3. Define la variable `CLAVE_REGISTRO_DOCENTE` en la configuracion de ejecucion o consola, para reemplazar la clave de prueba. En PowerShell:
   ```powershell
   $env:CLAVE_REGISTRO_DOCENTE="una-clave-administrativa-larga-de-prueba"
   ```
4. (Recomendado) Crea un secreto JWT Base64 persistente con 32 bytes aleatorios para que los QR y JWT sobrevivan a un reinicio. En PowerShell:
   ```powershell
   [byte[]]$bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:APP_JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
   ```
   Puedes dejarlo vacio en pruebas, pero el servidor generara una clave efimera nueva al reiniciar y los tokens antiguos dejaran de servir.
5. (Para recibir correos de prueba) Instala Docker Desktop y ejecuta, en esta carpeta:
   ```bash
   docker compose up -d mailpit
   ```
   Panel de correos de prueba: <http://localhost:8025>. Si no hay SMTP activo, las clases se crean igual pero los envios fallan, y la recuperacion de contrasena no entrega el token.
6. Inicia `ColegioApplication.java` desde IntelliJ o desde una terminal:
   ```bash
   mvn spring-boot:run
   ```
7. Abre <http://localhost:8080/swagger-ui.html> o la definicion OpenAPI en <http://localhost:8080/v3/api-docs>.

El perfil predeterminado es `h2`. Los datos se guardan en `./data/colegio.mv.db` y no se pierden al reiniciar. `spring.jpa.hibernate.ddl-auto=update` se usa **solo para desarrollo**.

## Probar el sistema en Swagger: orden recomendado

### 1. Registrar profesor

`POST /api/auth/registro/profesores`

Cabecera `X-Clave-Docente`: el valor configurado en `CLAVE_REGISTRO_DOCENTE` (si no la cambiaste, el valor local por defecto es `clave-docente-local-cambiar`; **no usar fuera de desarrollo**).

```json
{
  "identificacion": "1020304050",
  "nombre": "Ana Gomez",
  "materia": "Matematicas",
  "correo": "ana@colegio.edu",
  "password": "ClaveSegura123!"
}
```

### 2. Registrar estudiantes de dos grupos

`POST /api/auth/registro/estudiantes`

```json
{
  "identificacion": "100200300",
  "nombre": "Luis Perez",
  "grupo": "11-2",
  "correo": "luis@colegio.edu",
  "password": "ClaveSegura123!"
}
```

Repitelo para otros estudiantes, por ejemplo en el grupo `11-3`.

### 3. Iniciar sesion

`POST /api/auth/login` con `correo` y `password` del profesor; copia la propiedad `token` de la respuesta. En Swagger pulsa **Authorize** (candado), pega **solo el token** (sin escribir `Bearer ` si Swagger lo anade automaticamente), y confirma. Las rutas docentes requieren rol PROFESOR; las rutas de estudiantes rol ESTUDIANTE.

### 4. Crear una nota

`POST /api/profesores/notas`

```json
{
  "estudianteId": 1,
  "valor": 4.50,
  "observacion": "Buen trabajo"
}
```

La materia es `Matematicas` porque el servidor la obtiene del JWT y del profesor en la base de datos; no se puede elegir otra desde el cuerpo JSON. El profesor puede cambiar sus propias notas con `PATCH /api/profesores/notas/{notaId}`.

### 5. QR del estudiante y escaneo

En Swagger cierra sesion del docente e introduce el token de estudiante.

`GET /api/estudiantes/qr` -> respuesta con `contenido`, `imagenBase64` y `expiraEn`. `GET /api/estudiantes/qr.png` sirve la imagen directamente. El QR contiene **nombre, grupo y token digital firmado**, pero **no la contrasena**.

Copia exactamente el campo `contenido` (un texto JSON que te entregaria un lector de QR), vuelve a autorizar como profesor y envia:

`POST /api/profesores/qr/escanear`

```json
{
  "contenido": "{\"nombre\":\"Luis Perez\",\"grupo\":\"11-2\",\"token\":\"eyJ...\"}"
}
```

El servidor valida la firma y expiracion, y devuelve el `estudianteId` con la materia del profesor. Para calificar sin hacer dos peticiones, usa `POST /api/profesores/qr/calificar` con ese mismo `contenido`, `valor` y `observacion`.

**Importante**: el backend no accede directamente a la camara: la interfaz web o el dispositivo escaner debe leer el QR y enviar su contenido a este endpoint. Un QR se puede fotografiar/copiar y reutilizar hasta que caduque; la identidad debe verificarse presencialmente si el colegio lo necesita.

### 6. Publicar tarea, iniciar clase y registrar grabacion

- `POST /api/profesores/trabajos` con `grupo=11-2`, `titulo`, `descripcion` y `fechaEntrega` opcional. Solo los alumnos de ese grupo la consultan mediante `GET /api/estudiantes/trabajos`.
- `POST /api/profesores/clases` con `grupo=11-2` y `titulo`. Devuelve un enlace Jitsi de sala aleatoria. **Despues de confirmar la transaccion**, encola correos SMTP para todos los alumnos de ese grupo.
- `GET /api/profesores/clases`: revisa `estadoNotificacion`, `correosEnviados`, `correosFallidos` porque la entrega es asincrona y la respuesta inicial puede mostrar `PENDIENTE`.
- `GET /api/estudiantes/clases/activas` (logueado como estudiante 11-2) permite acceder al enlace. Los de 11-3 no ven esta clase.
- `PATCH /api/profesores/clases/{id}/finalizar` y, luego, `PATCH /api/profesores/clases/{id}/grabacion` con `{"url":"https://..."}`. Las grabaciones se listan en `GET /api/estudiantes/clases/grabaciones`.

**Jitsi**: generar un enlace no es lo mismo que organizar una videoconferencia privada. El usuario debe abrirlo, y las funciones de administracion de sala y grabacion dependen de Jitsi/servidor externo. El sistema **registra la URL de la grabacion, no graba videos**.

### 7. Recuperar la contrasena

`POST /api/auth/olvide-password` con `{"correo":"luis@colegio.edu"}`. El sistema almacena el **SHA-256** del token, no el token en texto claro, y envia un email a Mailpit (o a tu SMTP configurado) con un token que dura 15 min. Usa `POST /api/auth/restablecer-password` con `{"token":"...","nuevaPassword":"..."}`. Es de un solo uso. Los JWT viejos no podran utilizar las rutas despues del cambio de contrasena.

## Usar MySQL en vez de H2

Puedes iniciar MySQL de prueba con:

```bash
docker compose --profile mysql up -d mysql
```

Configura `DB_USER`, `DB_PASSWORD` y `DB_URL` (por ejemplo `jdbc:mysql://localhost:3306/colegio?useSSL=false&serverTimezone=UTC`) y arranca con:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Si usas el MySQL del `docker-compose.yml`, configura `DB_USER=colegio` y `DB_PASSWORD` con el valor de `MYSQL_APP_PASSWORD`. Para la instalacion normal de MySQL, crea la base de datos y usuario primero. Nunca uses el usuario root de MySQL en produccion.

## Estructura

```text
src/main/java/com/ejemplo/colegio/
  ColegioApplication.java
  config/        Spring Security, OpenAPI, JWT y eventos asincronos
  controller/    Auth, Profesor, Estudiante, Nota, Trabajo y ClaseVirtual
  domain/        Profesor, Estudiante, Nota, Trabajo, ClaseVirtual, TokenRecuperacion
  dto/           Datos de entrada/salida (records con validaciones)
  repository/    JpaRepository para las seis entidades
  security/      Emision/verificacion de JWT, usuario actual
  service/       Auth, Recuperacion, QR, Notas, Trabajos, Clases y Correo
  support/       Excepciones y errores de API
src/main/resources/      application.yml y perfiles h2/mysql
src/test/java/           Pruebas de integracion MockMvc
```

## Pruebas automatizadas

```bash
mvn test
mvn clean package
java -jar target/colegio-backend-1.0.0.jar
```

Las pruebas usan H2 de memoria y un emisor de email simulado. No necesitan Docker ni datos previos. Se comprueban registro, roles, firma QR, asignacion de materia, promedio, publicacion por grupo y visibilidad de clases.

## Seguridad y siguientes pasos de produccion

- Mantener la API limitada a localhost durante las pruebas. Para exponerla, protegerla con HTTPS, proxy y politicas CORS adecuadas.
- Pasar registro de profesores y estudiantes a un flujo administrativo/invitaciones; exigir verificacion de correo y consentimiento de responsables en caso de menores.
- Usar secretos largos en variables de entorno, almacenamiento seguro de claves y estrategia para rotacion JWT. Nunca subir `.env` con credenciales.
- Aplicar rate limiting a registro, login, recuperacion y consultas de QR; agregar audit logs, pruebas y controles de acceso adicionales.
- Para entregas de correo confiables ante caidas/reintentos, implementar **outbox** persistente con retries e idempotencia en lugar de depender solamente de eventos en memoria.
- Reemplazar `ddl-auto=update` por Flyway/Liquibase, controlar backups y migraciones, y proteger la informacion personal de menores conforme a normativa aplicable.
- Spring Boot 3.5.16 es la ultima version de soporte OSS para 3.5.x; planifica migrar a 4.x con springdoc 3.x cuando lo permita el requisito de tecnologia.
