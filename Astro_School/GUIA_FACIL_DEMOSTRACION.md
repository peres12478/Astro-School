# Colegio en español · Guía para comenzar (versión de demostración)

## Qué se mejoró

- **Acceso normal:** ahora admite correo electrónico **o número de identificación (cédula/tarjeta)**, más contraseña. No necesitas conocer el ID interno de la tabla de la base de datos.
- **Avisos claros:** al entrar aparece «Sesión iniciada correctamente...» y una etiqueta de sesión activa arriba. Al agregar/editar notas aparecen mensajes de confirmación.
- **Demostración en un clic:** se crean **dos profesores y dos estudiantes ficticios**, solo en el perfil local H2 mientras `app.demo.enabled=true`. No es necesario registrarlos uno a uno.
- **Calificaciones más fáciles:** al entrar en «Calificaciones» con una cuenta docente se cargan automáticamente los estudiantes del salón `11-2`. El botón «Editar esta nota» distingue el ID de la nota del ID del alumno.
- **Código QR simplificado:** ahora es más compacto, se puede visualizar/copiar/descargar y sigue firmado y con vencimiento. El profesor puede escanearlo con una cámara compatible o pegar el texto del QR. Se siguen aceptando QR antiguos en formato JSON.
- **API:** Swagger tiene un apartado **«00. Prueba rápida»** para pedir un token de demostración, y el inicio de sesión explica cómo usar el correo o el documento.

## Cuentas de prueba incluidas

| Tipo | Nombre ficticio | Identificación | Correo de prueba | Materia / grupo |
|---|---|---|---|---|
| Profesor | Ana Docente (demo) | `9001001` | `profe.mates@demo.colegio.test` | Matemáticas |
| Profesor | Carlos Docente (demo) | `9001002` | `profe.ciencias@demo.colegio.test` | Ciencias |
| Estudiante | Laura Estudiante (demo) | `9002001` | `laura@demo.colegio.test` | 11-2 |
| Estudiante | Mateo Estudiante (demo) | `9002002` | `mateo@demo.colegio.test` | 11-2 |

**Contraseña de TODAS las cuentas ficticias:** `Demo2026!`. En el panel no es necesario escribirla: simplemente pulsa una cuenta de ejemplo.

Estos documentos y direcciones son inventados: no se envían correos reales a estas direcciones. Ninguna contraseña real está incluida en el ZIP.

## Instalar sin perder alumnos, notas ni profesores que ya tenías

1. Detén Spring Boot (`Ctrl + C`) y desconecta DBeaver.
2. Renombra `C:\java2\colegio-backend` a `C:\java2\colegio-backend-respaldo`.
3. Extrae el ZIP nuevo dentro de `C:\java2`, de forma que se cree `C:\java2\colegio-backend\pom.xml`.
4. Copia la carpeta `data` de `colegio-backend-respaldo` a la carpeta `colegio-backend` nueva. **No borres el respaldo.** La base de datos original está en `data\colegio.mv.db`. Copia con los programas detenidos.
5. Si habías modificado claves de registro, SMTP o JWT, recupera esos valores de tus configuraciones anteriores; evita copiar un `application.yml` antiguo completo, porque podría sobrescribir los cambios.
6. Abre PowerShell:

```powershell
cd C:\java2\colegio-backend
mvn clean verify
mvn spring-boot:run
```

7. Con el servidor encendido, abre http://localhost:8080/ . Si necesitas Swagger, abre http://localhost:8080/swagger-ui.html .

## Probar todo en menos pasos

1. En la portada elige **«Ana Docente (demo)»**. Debe aparecer «Modo demostración: sesión iniciada como profesor».
2. Pulsa **Estudiantes** o **Calificaciones**. En el grupo `11-2` deberían mostrarse **Laura** y **Mateo**.
3. Escoge **Agregar nota** a Laura, escribe por ejemplo `4.5` y guarda. Deberías ver **«Nota agregada correctamente»**.
4. Pulsa **Editar esta nota** para cambiarla. No necesitas escribir IDs a mano.
5. Pulsa **Salir**. Entra como **Laura Estudiante (demo)** y revisa **Mis notas**.
6. Abre **Mi QR**. El código debe aparecer; puedes **Descargar QR**, **Copiar contenido** o **Generar otro QR**.
7. Para comprobar el QR sin cámara: copia el contenido, sal, entra como Ana Docente, abre **Código QR**, pega el contenido y pulsa **Comprobar código**. Debe identificar a Laura. Puedes darle una nota directamente desde esta pantalla.
8. Si tu navegador admite `BarcodeDetector` y cámara, usa **Escanear con cámara**. Si no, el método de pegar texto siempre funciona.

## Probarlo desde Swagger (solo si necesitas ver la API)

1. Ejecuta `GET /api/demo/cuentas` para conocer las cuentas.
2. Ejecuta `POST /api/demo/entrar` con el JSON:

```json
{"identificacion":"9001001"}
```

3. La respuesta tendrá `mensaje`, `rol`, `usuarioId` y un `token`. **Si hay token y el mensaje dice «Sesión iniciada», sí has entrado.**
4. Haz clic en **Authorize** e introduce el token. Ahora prueba las rutas de profesor. Para las rutas de alumno, usa la cuenta `9002001` y cambia la autorización.
5. `POST /api/auth/login` ahora acepta:

```json
{"usuario":"9001001","password":"Demo2026!"}
```

También admite un correo en `usuario`, y por compatibilidad conserva el nombre de campo antiguo `correo`.

**Importante:** la API no debe permitir que una cuenta de estudiante ponga notas de profesor. En lugar de quitar esa protección, el panel crea una **sesión de prueba en un clic**. En Swagger aún necesitas autorizar el token para los endpoints protegidos.

## DBeaver en tiempo real



Usuario: `sa`; contraseña: vacía. En `PUBLIC → Tables → ESTUDIANTES → Data`, actualiza con `F5` al crear alumnos. La base no se sube a la nube; está en el archivo `data\colegio.mv.db` del equipo.

## Seguridad y limitaciones

- **Modo demostración es solo para pruebas locales con datos ficticios.** Las cuentas y su contraseña son públicas. NO lo utilices con datos reales de alumnos: las cuentas de demostración pueden leer y modificar información accesible a profesores. Antes de pasar a uso real configura en PowerShell `$env:APP_DEMO_ENABLED="false"` y reinicia; no uses credenciales públicas como cuentas de clase.
- Spring Boot escucha por defecto en `127.0.0.1` y las rutas de prueba rápida solo aceptan solicitudes desde el mismo equipo.
- Las cuentas de muestra se insertan **solo si no existen**, sin borrar, sustituir ni sobrescribir datos anteriores. Desactivar modo demo impide el acceso rápido, pero no borra cuentas que ya se habían creado.
- Para enviar correos reales hay que configurar SMTP. Para que los QR/tokens sobrevivan a los reinicios hay que configurar un secreto JWT persistente (`APP_JWT_SECRET_BASE64`).
- El lector de cámara de QR depende del navegador y de sus permisos. Si no funciona, usa «Copiar contenido» y pégalo.
- Los enlaces de Jitsi son reuniones virtuales; la app no las graba automáticamente.

## Si no se ve la nueva interfaz

Pulsa `Ctrl + F5` en Chrome/Edge para borrar caché de esa página y comprueba que la terminal muestra `Started ColegioApplication`. Si hay error de compilación, revisa el primer mensaje `[ERROR]` de `mvn clean verify` y conserva la carpeta de respaldo para recuperarte.

## Selector de perfiles

La barra superior indica la cuenta y el rol de su token. Haz clic en **Profesor** o **Estudiante** y selecciona una cuenta ficticia; el sistema obtiene el token automáticamente. Para cuentas reales hay que autenticarse. **Copiar token** sirve únicamente para Swagger → Authorize.

La conexión simultánea especial de DBeaver se retiró. La base H2 local se mantiene para almacenar datos.
