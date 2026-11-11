# Astro School 3.0 — Laboratorios y Nuestras creaciones

**Tarea Realizada por Miguel Ángel Pérez García**


**Astro School** amplía el proyecto escolar original. Conserva sus APIs REST para autenticación, profesores, estudiantes, notas, QR, tareas y clases virtuales. Añade una interfaz con identidad visual espacial, un noticiero comunitario con archivos, tres cursos optativos, laboratorios propios y la galería pública **Nuestras creaciones**. **Swagger no fue eliminado**: las funciones se organizan por secciones y siguen accesibles en `/swagger-ui.html`.

## Empezar en Windows

1. Instala **Java 17 o superior** y **Apache Maven 3.9+** (añade ambos al `PATH`).
2. Descomprime este ZIP. Abre la carpeta `colegio-backend`, que contiene `pom.xml`.
3. Ejecuta `INICIAR_WINDOWS.bat` o, desde una terminal en esa carpeta, `mvn spring-boot:run`.
4. Abre **http://localhost:8080/** para la interfaz Astro School.
5. Abre **http://localhost:8080/swagger-ui.html** para las rutas API y las pruebas técnicas.

En Linux/macOS, abre la terminal en `colegio-backend` y ejecuta `mvn spring-boot:run`.

Para ejecutar la batería completa de pruebas de Java, usa `mvn clean verify`.
Para los tests de interfaz simulados, usa `node scripts/probar_panel_node.cjs`, `node scripts/probar_astro_node.cjs` y `node scripts/probar_laboratorios_node.cjs` (Node.js 18+).

## Nuevas funciones

### Noticiero público

En la portada pulsa **Explorar el noticiero**. Cualquier persona con acceso a la web puede publicar noticias y consultarlas **sin iniciar sesión**. Para publicar, completa título, descripción y nombre público de autor. Se puede añadir opcionalmente un archivo de hasta **8 MB** en formatos: **PDF, PNG, JPG, JPEG, TXT, DOCX, XLSX o PPTX**. Los archivos siempre se ofrecen como descarga, nunca se ejecutan en el servidor.

- `GET /api/noticias`: últimas 50 publicaciones.
- `GET /api/noticias/{id}`: una noticia.
- `POST /api/noticias`: publicación **multipart/form-data** con campos `titulo`, `descripcion`, `autor` y `archivo` opcional.
- `GET /api/noticias/{id}/archivo`: descargar el adjunto.

La información de las noticias se guarda en la base H2 por defecto (`data/colegio.mv.db`). Los archivos físicos se guardan en **`data/noticias/`**. Para respaldar o trasladar la instalación, conserva **toda la carpeta `data`**; NO solo el archivo `.mv.db`.

**Advertencia:** el nombre del autor es libre (no equivale a identidad verificada). La publicación sin cuenta es una función intencional para la demo local. Si vas a exponer el proyecto a Internet o usar datos reales, habilita antes controles de moderación, límites por usuario/IP, análisis de malware y privacidad de estudiantes. Puedes **desactivar la publicación pública** con `APP_NEWS_PUBLIC_UPLOADS=false` (la consulta pública seguirá disponible).

### Cursos Astro

La sección **Cursos Astro** muestra:

- **Robótica** (`ROBOTICA`)
- **Física** (`FISICA`)
- **Astronomía** (`ASTRONOMIA`)

Solo una cuenta **ESTUDIANTE** autenticada puede inscribirse. **La regla se verifica en el backend y la base de datos**: cada estudiante puede tener **una única inscripción activa**, incluso con dos pestañas abiertas. Si quiere cambiar de curso, debe **cancelar primero** la inscripción anterior; no pierde las notas ni el resto de sus datos.

- `GET /api/cursos`: listado público de cursos y número de inscritos.
- `GET /api/cursos/mi-inscripcion`: información de mi curso (token estudiante; `204` si no tiene).
- `POST /api/cursos/inscripciones`: cuerpo JSON `{"curso":"ROBOTICA"}`; `201` si inscribe, `409` si ya tiene uno; token estudiante.
- `DELETE /api/cursos/mi-inscripcion`: cancelar para elegir otro; token estudiante.

Las inscripciones se guardan en la misma base de datos que los alumnos. No se han creado nuevas contraseñas ni un segundo sistema de registro.

## Swagger: /api y autenticación

Las rutas antiguas siguen funcionando. Swagger conserva **07. Noticiero Astro** y **08. Cursos Astro**, y añade cuatro grupos: **09. Nuestras creaciones**, **10. Taller de Robotica**, **11. Laboratorio de Fisica** y **12. Observatorio de Astronomia**. Los endpoints públicos de lectura y publicación de noticias no necesitan token, pero inscribirse sí.

Para probar inscripciones en Swagger:

1. Usa `POST /api/demo/entrar` con `{ "identificacion": "9002001" }`, si el modo demo está habilitado.
2. Copia el campo `token` de la respuesta. Pulsa **Authorize** y pégalo tal cual.
3. Abre `POST /api/cursos/inscripciones`, pulsa **Try it out**, escribe `{"curso":"ASTRONOMIA"}` y ejecuta.
4. Comprueba el curso con `GET /api/cursos/mi-inscripcion`.
5. Intenta otro curso: recibirás `409 Conflict`. Usa `DELETE /api/cursos/mi-inscripcion` antes de cambiar.

El panel web utiliza los tokens de forma automática. Para probar las APIs de docentes, pega un token de **PROFESOR**.

## Bases de datos y configuración

El perfil predeterminado **H2** conserva la base local en `data/colegio.mv.db`. Hibernate puede añadir las nuevas tablas a una base existente durante el desarrollo. **Antes de abrir una base antigua**, apaga el programa y haz copia completa de la carpeta `data`.

Para trabajar con MySQL, sigue vigente el perfil `mysql` (consulta `docker-compose.yml` y `application-mysql.yml`). Las carpetas de archivos de noticias y de creaciones deben respaldarse por separado de MySQL.

Variables opcionales:

| Variable | Propósito | Predeterminado |
|---|---|---|
| `APP_NEWS_DIR` | Carpeta donde guardar adjuntos de noticias | `./data/noticias` |
| `APP_CREATIONS_DIR` | Adjuntos de Nuestras creaciones | `./data/creaciones` |
| `APP_NEWS_PUBLIC_UPLOADS` | Permitir publicar noticias sin una cuenta | `true` (solo demo/local) |
| `APP_DEMO_ENABLED` | Cuentas de demostración en H2 | `true` |
| `SERVER_ADDRESS` | Dirección del servidor | `127.0.0.1` |
| `SERVER_PORT` | Puerto HTTP | `8080` |
| `CLAVE_REGISTRO_DOCENTE` | Clave para registrar docentes | `clave-docente-local-cambiar` (CAMBIAR) |
| `APP_JWT_SECRET_BASE64` | Clave JWT permanente de mínimo 32 bytes codificados en Base64 | aleatoria al reiniciar |

**Para acceder desde otro equipo** en la misma red, configura `SERVER_ADDRESS=0.0.0.0`, permite el puerto en el firewall y entra desde ese equipo a `http://IP-DEL-SERVIDOR:8080/`. Esta configuración **no es segura para Internet público sin HTTPS, controles de acceso y revisión de seguridad**.

## Qué se preservó

Se mantienen las rutas y funciones originales de registro/login, perfil de profesores y estudiantes, notas de 0 a 5, edición de notas, códigos QR, tareas y clases virtuales. Se conserva también el botón **Copiar token** para Swagger y las cuentas de prueba originales. No se elimina ni se sustituye Swagger por el frontend.


## Astro School 3.0: laboratorio propio de cada curso

**No hay que registrarse otra vez.** Cada estudiante usa su cuenta escolar y se inscribe en **un curso a la vez**. En la barra lateral aparece **Mi laboratorio**. Los permisos se verifican en Spring Security y consultando la inscripción de la base, no solamente ocultando botones.

### 10. Robótica — robots y editor de código

- `GET /api/robotica/mi-espacio` — proyectos propios publicados.
- `GET /api/robotica/robots` — robots de la cuenta, no de otros estudiantes.
- `POST /api/robotica/robots` — registrar uno con JSON, por ejemplo `{"nombre":"AstroBot","plataforma":"Arduino Uno","descripcion":"Seguidor de línea"}`.
- `GET /api/robotica/robots/{robotId}/codigos` — historial de versiones guardadas.
- `POST /api/robotica/robots/{robotId}/codigos` — guardar código con JSON `{"nombre":"seguidor.ino","lenguaje":"ARDUINO","contenido":"void setup() {}\nvoid loop() {}"}`.
- `POST /api/robotica/robots/{robotId}/codigos/archivo` — subir un archivo fuente mediante `multipart/form-data`, campo `archivo`.
- `GET /api/robotica/robots/{robotId}/codigos/{codigoId}/descargar` — descargar el archivo (con token propio).
- `POST /api/robotica/proyectos` — publicar un prototipo en la galería compartida.

El **editor visual** admite Arduino, Python, C/C++, JavaScript y texto. **Los archivos subidos** admiten `.ino`, `.py`, `.c`, `.cpp`, `.h`, `.hpp`, `.js`, `.txt`, solo texto UTF-8 de hasta **100 KB**. Cada guardado crea otra versión, que se puede abrir de nuevo en el editor o descargar. **El servidor NO ejecuta ni compila ni flashea el código a un robot físico**; para subirlo a una placa usa Arduino IDE u otra herramienta compatible.

### 11. Física — experimentos

- `GET /api/fisica/mi-espacio` — ver mi laboratorio y mis proyectos.
- `POST /api/fisica/proyectos` — compartir una investigación o experimento con título, descripción y adjunto opcional en `multipart/form-data`.

### 12. Astronomía — observaciones

- `GET /api/astronomia/mi-espacio` — ver mi observatorio y mis proyectos.
- `POST /api/astronomia/proyectos` — compartir observaciones o investigaciones.

### 09. Nuestras creaciones — galería pública

- `GET /api/creaciones` — las 100 creaciones más recientes de todos los cursos, **sin iniciar sesión**.
- `GET /api/creaciones?curso=ROBOTICA` (o `FISICA`, `ASTRONOMIA`) — filtrar categoría.
- `GET /api/creaciones/{id}` — detalles, autor y fecha.
- `GET /api/creaciones/{id}/archivo` — descarga pública del adjunto si existe.

La **publicación** de proyectos requiere iniciar sesión como estudiante y estar inscrito en el curso correcto. Los proyectos muestran el **nombre real de la cuenta escolar** (no un autor inventado). Es posible crear proyectos sin adjuntos. Se admiten PDF, JPG, PNG, TXT, DOCX, XLSX y PPTX de hasta 8 MB; los archivos se ofrecen como adjuntos descargables con `nosniff`, nunca se ejecutan ni interpretan en el servidor. La galería pública no expone por sí sola el historial privado de programas de un robot: si se quiere compartir código, hay que incorporarlo voluntariamente al material del proyecto (p. ej., un archivo `.txt` o un documento).

La inscripción controla el acceso **actual** al laboratorio. Al cancelar o cambiar de curso, los proyectos ya publicados permanecen visibles en la galería con su categoría original; los robots y códigos quedan guardados en la base y vuelven a ser accesibles cuando el estudiante se reinscribe en Robótica.

### Prueba rápida en Swagger

1. `POST /api/demo/entrar` → `{ "identificacion": "9002001" }` (solo modo demo local).
2. Copiar el campo `token` y pegarlo en **Authorize** (sin prefijo `Bearer`).
3. `POST /api/cursos/inscripciones` → `{ "curso":"ROBOTICA" }`.
4. `POST /api/robotica/robots` → registra un robot; toma su `id`.
5. `POST /api/robotica/robots/{robotId}/codigos` → guarda tu primer programa, usando ese ID.
6. `POST /api/robotica/proyectos` → publica un proyecto.
7. `GET /api/creaciones` → revisa el resultado **sin token**.
8. Para probar Física/Astronomía, cancela tu inscripción anterior o usa otra cuenta de estudiante. La restricción sigue siendo un curso por alumno.

### Seguridad, persistencia y pruebas

Las nuevas tablas (`robots_estudiantiles`, `codigos_robots`, `creaciones`) se crean mediante Hibernate sobre la misma base del proyecto para desarrollo. **Haz una copia de la carpeta `data` antes de actualizar**. También respalda `data/creaciones/`, que contiene los adjuntos. Las notas, los usuarios, las inscripciones, las noticias y Swagger no se eliminan.

El laboratorio exige **JWT del estudiante**, verifica la inscripción correcta y solo permite consultar robots/código del dueño. Los lectores anónimos solo acceden a proyectos que sus autores hayan publicado expresamente. La interfaz escapa el texto aportado por usuarios. Las pruebas de frontend están en `scripts/probar_laboratorios_node.cjs`; las pruebas de integración Spring están en `src/test/java/com/ejemplo/colegio/LaboratoriosIntegrationTest.java` y se ejecutan con `mvn clean verify`.

**Antes de usarlo en un centro educativo real**: activar HTTPS, aplicar moderación de publicaciones, permisos de tutores, límites por usuario, revisar archivos subidos con antivirus y gestionar la privacidad/consentimiento de nombres de menores; además, sustituir la clave docente de prueba y desactivar las cuentas demo. Esta versión es un prototipo educativo, no un producto auditado para producción.

---

## Astro School 4.0 — ejemplos de demostración por sección

Para que una instalación nueva no aparezca vacía, al iniciar con el perfil `h2` y `APP_DEMO_ENABLED=true` (valor predeterminado), la aplicación incluye **datos ficticios claramente marcados `[DEMO]`**:

| Apartado | Lo que aparece como ejemplo |
|---|---|
| Profesores | Ana (Matemáticas) y Carlos (Ciencias), con cuentas de prueba |
| Estudiantes | Laura (n.º 101), Mateo (n.º 102), Sofía (n.º 103), grupo 11-2 |
| Notas | Nota de Matemáticas 4,50 y nota de Ciencias 4,20 |
| Tareas | Una actividad de ciencias para 11-2 |
| Clases virtuales | Una sala Jitsi marcada como demo; **no representa una clase en vivo** |
| Noticiero | Una noticia de bienvenida ficticia |
| Cursos | Laura inscrita en Robótica, Mateo en Física y Sofía en Astronomía |
| Robótica | Robot AstroBot de muestra y un programa Arduino `parpadeo.ino` |
| Física | Ejemplo de experimento de péndulo con ilustración PNG |
| Astronomía | Ejemplo de observación lunar con ilustración PNG |
| Nuestras creaciones | Tres proyectos públicos, cada uno con **imagen de demostración visible en la galería** y archivo descargable |

Las imágenes se incluyen dentro del paquete; son **ilustraciones originales de demostración**, no fotos reales de trabajos estudiantiles. Los estudiantes pueden subir **fotografías reales en PNG/JPG** con sus publicaciones, que también se verán como miniaturas. Los archivos de texto, PDF y Office seguirán apareciendo como enlaces de descarga.

Las semillas se ejecutan **solo en H2 local con modo demo** y comprueban que el ejemplo no exista antes de insertarlo, sin eliminar registros previos. Si estableces `APP_DEMO_ENABLED=false`, no se crearán más ejemplos en nuevas bases de datos (los ejemplos ya persistidos no se borran). Para una implementación real: desactiva el modo demo, utiliza credenciales seguras y no cargues información privada con las cuentas de prueba.

Para entrar como cada alumno: desde la página, selecciona **Probar sin registrarme** y elige Laura (Robótica), Mateo (Física) o Sofía (Astronomía). La API y las funciones originales de Swagger se conservan: las creaciones aparecen en `GET /api/creaciones`; las noticias en `GET /api/noticias`; los cursos en `GET /api/cursos`. Los datos de ejemplo no sustituyen el registro de usuarios ni la publicación de nuevos proyectos.

Comprobaciones offline: `python scripts/verificar_ejemplos.py` y `node scripts/probar_laboratorios_node.cjs`. Para validar el backend real: `mvn clean verify` (Maven y Java 17+).
