# Guía del panel escolar

**Lee primero [GUIA_FACIL_DEMOSTRACION.md](GUIA_FACIL_DEMOSTRACION.md)**: tiene las cuatro cuentas de ejemplo, el acceso con documento/correo, el QR corregido y el paso a paso actualizado.

# Guía del panel escolar en español

Esta versión añade un **panel web sencillo** para profesores y estudiantes. Mantiene la API REST, Swagger, la base H2, el sistema de notas, trabajos, clases, QR y recuperación de contraseña.

## Inicio rápido (Windows)

1. **Apaga Spring Boot** (`Ctrl + C` en su terminal). No borres la carpeta `data/` de tu proyecto anterior: allí se encuentra la base de datos H2 con las notas y los usuarios.
2. En el Explorador de Windows, ve a `C:\java2` y renombra tu carpeta actual `colegio-backend` como `colegio-backend-respaldo` (no la borres).
3. Extrae este ZIP en `C:\java2`. Debe quedar una **nueva** carpeta `C:\java2\colegio-backend` con un `pom.xml` dentro. Es más seguro empezar con fuentes limpias que copiar archivos encima de una versión antigua.
4. Copia la carpeta completa `C:\java2\colegio-backend-respaldo\data\` a `C:\java2\colegio-backend\data\` con el servidor aún apagado. Confirma que siga allí `colegio.mv.db`. Si habías personalizado `application.yml`, revisa también esos valores (claves, correo SMTP, etc.) antes de iniciar la nueva versión.
5. Abre PowerShell en `C:\java2\colegio-backend` (la carpeta con `pom.xml`).
6. Comprueba la compilación: `mvn clean verify`.
7. Inicia el servidor: `mvn spring-boot:run`.
8. Abre <http://localhost:8080/> para usar el **panel en español**. Si necesitas la API técnica, abre <http://localhost:8080/swagger-ui.html>.

También puedes abrir `INICIAR_WINDOWS.bat` para iniciar el proyecto desde su propia carpeta.

## Registrar una cuenta e iniciar sesión

En la portada selecciona **Iniciar sesión**, **Registrar estudiante** o **Registrar docente**, o usa una cuenta de demostración con un solo clic. Los profesores necesitan la **clave de alta docente** además de su contraseña personal. Esa clave se configura en `src/main/resources/application.yml` o con la variable de entorno `CLAVE_REGISTRO_DOCENTE`. Nunca compartas esa clave con estudiantes.

El panel recuerda temporalmente la sesión durante la pestaña del navegador mediante `sessionStorage`: al cerrar la pestaña o salir, se borra esa sesión local. No almacena contraseñas ni la clave de alta. Para usar el colegio en un servidor público, configura HTTPS y una clave JWT persistente.

## Poner y modificar notas

1. Como profesor, abre **Estudiantes**, escribe el salón (por ejemplo `11-2`) y pulsa **Buscar estudiantes**.
2. Pulsa **Agregar nota** junto al alumno.
3. Escribe la calificación entre `0` y `5` (hasta dos decimales), añade la observación si quieres y pulsa **Guardar nota**.
4. Aparecerá **«Nota agregada correctamente.»**.
5. Para cambiar una nota, abre **Calificaciones** y pulsa **Editar esta nota** sobre la calificación. Introduce el nuevo valor y guarda. Aparecerá **«Nota modificada correctamente.»**.

El panel selecciona los IDs internos automáticamente. Si usas Swagger:

| Campo | Qué significa | Dónde encontrarlo |
|---|---|---|
| `estudianteId` | ID interno del estudiante al que vas a calificar | `GET /api/profesores/estudiantes?grupo=11-2` → campo `id` del alumno |
| `id` de `PUT /api/notas/{id}` | **ID de la NOTA** que deseas modificar | `GET /api/notas/estudiante/{estudianteId}` → campo `id` de la nota |
| `estudianteId` de `GET /api/notas/estudiante/{estudianteId}` | **ID del ESTUDIANTE** del que consultarás las notas | `GET /api/profesores/estudiantes?grupo=11-2` |
| `id` de `PATCH /api/clases/{id}/finalizar` | **ID de la CLASE** que se finaliza | `GET /api/clases/mis` → campo `id` de la clase |
| `id` de `PUT /api/clases/{id}/grabacion` | **ID de la CLASE** ya finalizada | `GET /api/clases/mis` |

La **cédula** y la **tarjeta de identidad** son documentos personales, y **no** se usan como IDs internos. En Swagger ahora se explica cada parámetro y los resultados de crear/modificar una nota incluyen un campo `mensaje`, manteniendo los campos anteriores (`id`, `valor`, `materia` y otros).

## Otras secciones

- **Tareas:** publica tareas por salón y consulta las actividades de tu grupo.
- **Clases virtuales:** crea enlaces de Jitsi, consulta el estado de las invitaciones, finaliza clases y agrega enlaces HTTPS de grabaciones existentes. **La aplicación no graba reuniones automáticamente.**
- **QR:** el estudiante puede mostrar, descargar o copiar su código QR. El profesor puede pegar el contenido leído por un lector para verificarlo y calificar. **Si tu navegador admite `BarcodeDetector` puedes usar «Escanear con cámara»; de lo contrario puedes pegar el texto del código.**
- **Recuperación de contraseña:** usa el correo registrado y un token de un solo uso. **Para recibir correos reales debes configurar SMTP**; el servidor local de pruebas no los entrega a Gmail.

## Ver estudiantes en DBeaver en tiempo real, sin cerrar Spring Boot

Esta version **incluye un servidor H2 TCP local** integrado en Spring Boot.
Evita la conexion H2 Embedded simultanea, que bloquea `colegio.mv.db`.

1. **Con Spring Boot y DBeaver detenidos**, haz una copia de `data/` por seguridad antes de actualizar. Conserva toda la carpeta `data` cuando instales esta version.
5. Si DBeaver solicita descargar un controlador, usa la **version H2 2.3.232** (la que usaba tu Spring Boot 3.5.16), o la misma version de H2 que Maven muestre en tus dependencias. Puede configurarse en **Editar conexion -> Driver Settings -> Libraries**. Si la version no coincide, puede aparecer un error de protocolo.
6. Pulsa **Test Connection** y luego **Finish**. Abre `Schemas -> PUBLIC -> Tables -> ESTUDIANTES -> Data`, o ejecuta esta consulta SQL:

```sql
SELECT * FROM ESTUDIANTES ORDER BY ID DESC;
```

7. Registra un estudiante desde `http://localhost:8080/` o desde Swagger. Presiona `F5` en la tabla de DBeaver para actualizarla. Si tu version de DBeaver ofrece **Auto-refresh**, puedes usarlo con un intervalo de 3 segundos.


## Una sola opcion para registrar estudiantes

Swagger ahora muestra solamente **`POST /api/auth/registro/estudiantes`**. La antigua direccion singular sigue funcionando para scripts existentes, pero esta oculta de Swagger para evitar duplicados. Se aplica el mismo criterio al registro de profesores y otras rutas alternativas. La interfaz web ya tenia un unico formulario de registro por rol.

## Base de datos y compatibilidad

H2 sigue utilizando por defecto `jdbc:h2:file:./data/colegio`, desde el directorio de ejecución del proyecto. **Este ZIP no incluye ni borra tu base de datos**: es un conjunto de código y archivos de soporte. Para no perder datos, conserva `data/colegio.mv.db`. Mantén el mismo directorio de trabajo al ejecutar el programa.

La API existente sigue funcionando y los endpoints alternativos antiguos no se eliminaron; algunos están **ocultos solo en la documentación Swagger** para reducir ruido. El backend añade el atributo opcional `mensaje` a los resultados de crear/editar notas, sin quitar los atributos originales. Se incluyen pruebas de integración para comprobar los cambios.

## Si aparece un error

- `No se pudo conectar con el servidor`: verifica que la terminal con `mvn spring-boot:run` siga abierta.
- `401`: vuelve a iniciar sesión; en desarrollo las sesiones JWT anteriores dejan de servir al reiniciar el servidor si no has definido `APP_JWT_SECRET_BASE64`.
- `403`: probablemente la operación corresponde a otro tipo de cuenta (docente vs estudiante) o la clave de alta docente no coincide.
- `404`: el ID de un alumno, nota o clase no existe o no pertenece a tu usuario.
- Correo marcado como `ENVIADA`: el servidor SMTP aceptó la solicitud, no significa que el alumno lo haya leído.

## Nota sobre H2

El modo de conexión simultánea con DBeaver se eliminó. El panel permite ver alumnos y notas sin abrir la base externamente.
