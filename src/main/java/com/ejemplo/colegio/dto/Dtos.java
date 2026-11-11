package com.ejemplo.colegio.dto;

import com.ejemplo.colegio.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/** DTOs: ningun controlador expone entidades JPA ni hashes de contrasena. */
public final class Dtos {
    private Dtos() { }

    public record RegistroProfesor(
            @NotBlank @Size(max = 30) @Schema(description = "Número de documento del docente (cédula). No es el ID interno.", example = "1020304050") String identificacion,
            @NotBlank @Size(max = 120) @Schema(description = "Nombre completo del profesor.", example = "Ana Gómez") String nombre,
            @NotBlank @Size(max = 100) @Schema(description = "Materia que enseña este profesor. Se asignará automáticamente a sus notas.", example = "Matemáticas") String materia,
            @NotBlank @Email @Size(max = 180) @Schema(description = "Correo con el que iniciará sesión el profesor.", example = "ana@colegio.edu") String correo,
            @NotBlank @Size(min = 8, max = 72) @Schema(description = "Contraseña personal, mínimo 8 caracteres (NO es la clave de alta docente).", example = "ClaveSegura123!", format = "password") String password
    ) { }

    public record RegistroEstudiante(
            @NotBlank @Size(max = 30) @Schema(description = "Documento del estudiante (cédula o tarjeta de identidad). No es el ID interno.", example = "100200300") String identificacion,
            @NotBlank @Size(max = 120) @Schema(description = "Nombre completo del estudiante.", example = "Luis Pérez") String nombre,
            @NotBlank @Size(max = 30) @Schema(description = "Salón o curso del estudiante; ejemplo: 11-2.", example = "11-2") String grupo,
            @NotBlank @Email @Size(max = 180) @Schema(description = "Correo con el que iniciará sesión el estudiante.", example = "luis@colegio.edu") String correo,
            @NotBlank @Size(min = 8, max = 72) @Schema(description = "Contraseña personal, mínimo 8 caracteres (NO es la clave de alta docente).", example = "ClaveSegura123!", format = "password") String password
    ) { }

    public record Login(
            @NotBlank @JsonAlias({"correo", "identificacion"})
            @Schema(description = "Correo electrónico O número de identificación (cédula o tarjeta). No es el ID interno de base de datos.", example = "9001001") String usuario,
            @NotBlank @Schema(description = "Contraseña personal de la cuenta.", example = "Demo2026!", format = "password") String password
    ) { }

    public record AuthRespuesta(
            @Schema(description = "Token de acceso. En Swagger, pulsa «Authorize» y pega este valor.") String token,
            @Schema(description = "Tipo de autorización; normalmente Bearer.") String tokenTipo,
            @Schema(description = "Tipo de cuenta: PROFESOR o ESTUDIANTE.") TipoUsuario rol,
            @Schema(description = "ID interno de tu usuario; no es la cédula ni la tarjeta de identidad.", example = "1") Long usuarioId,
            @Schema(description = "Nombre del usuario que inició sesión.") String nombre,
            @Schema(description = "Momento en que vence la sesión.") Instant expiraEn,
            @Schema(description = "Confirmación legible del inicio de sesión o registro.") String mensaje) { }

    public record RecuperarPassword(@NotBlank @Email @Schema(description = "Correo con el que se registró la cuenta.", example = "nombre@colegio.edu") String correo) { }
    public record RestablecerPassword(
            @NotBlank @Schema(description = "Token de recuperación recibido por correo.") String token,
            @NotBlank @Size(min = 8, max = 72) @Schema(description = "Contraseña nueva de mínimo 8 caracteres.", format = "password", example = "NuevaClave123!") String nuevaPassword) { }
    public record Mensaje(String mensaje) { }

    public record ProfesorVista(@Schema(description = "ID interno del profesor.") Long id, String identificacion, String nombre,
                               String materia, String correo) {
        public static ProfesorVista de(Profesor p) {
            return new ProfesorVista(p.getId(), p.getIdentificacion(), p.getNombre(), p.getMateria(), p.getCorreo());
        }
    }

    public record EstudianteVista(@Schema(description = "ID interno del estudiante: úsalo al poner notas (estudianteId).") Long id, String identificacion, String nombre,
                                 String grupo, String correo) {
        public static EstudianteVista de(Estudiante e) {
            return new EstudianteVista(e.getId(), e.getIdentificacion(), e.getNombre(), e.getGrupo(), e.getCorreo());
        }
    }

    public record CrearNota(
            @NotNull @Positive @Schema(description = "ID INTERNO del estudiante que recibirá la nota. Se encuentra en «Profesores → Listar estudiantes» (campo id), no es su número de documento.", example = "1") Long estudianteId,
            @NotNull @DecimalMin("0.0") @DecimalMax("5.0") @Digits(integer = 1, fraction = 2)
            @Schema(description = "Calificación numérica de 0.00 a 5.00. Se aceptan decimales.", example = "4.50") BigDecimal valor,
            @Size(max = 500) @Schema(description = "Comentario opcional del docente.", example = "Buen desempeño") String observacion
    ) { }

    /** Escaneo + calificacion atomica: el profesor no elige identidad ni materia libremente. */
    public record CalificarQr(
            @NotBlank @Schema(description = "Texto COMPLETO del QR del estudiante (no su ID).") String contenido,
            @NotNull @DecimalMin("0.0") @DecimalMax("5.0") @Digits(integer = 1, fraction = 2)
            @Schema(description = "Calificación numérica de 0.00 a 5.00. Se aceptan decimales.", example = "4.50") BigDecimal valor,
            @Size(max = 500) @Schema(description = "Comentario opcional.", example = "Buen trabajo") String observacion
    ) { }

    public record ResumenMateria(String materia, BigDecimal promedio, long cantidadNotas,
                                 boolean aprobado) { }

    public record ActualizarNota(
            @NotNull @DecimalMin("0.0") @DecimalMax("5.0") @Digits(integer = 1, fraction = 2)
            @Schema(description = "Nuevo valor de la nota (de 0.00 a 5.00).", example = "4.50") BigDecimal valor,
            @Size(max = 500) @Schema(description = "Nueva observación opcional.", example = "Mejoró su trabajo") String observacion
    ) { }

    public record NotaVista(
            @Schema(description = "ID de ESTA NOTA. Usa este número para modificar la calificación, no el ID del estudiante.", example = "7") Long id,
            @Schema(description = "ID interno del estudiante que recibió la nota.", example = "1") Long estudianteId,
            String estudiante,
            @Schema(description = "ID interno del profesor que asignó la nota.") Long profesorId,
            String materia, BigDecimal valor, String observacion, Instant creadaEn) {
        public static NotaVista de(Nota n) {
            return new NotaVista(n.getId(), n.getEstudiante().getId(), n.getEstudiante().getNombre(),
                    n.getProfesor().getId(), n.getMateria(), n.getValor(), n.getObservacion(), n.getCreadaEn());
        }
    }

    /** Respuesta de escritura: conserva todos los campos originales y añade una confirmación legible. */
    public record NotaConfirmada(
            @Schema(description = "ID de la nota creada o modificada (sirve para volver a editarla).") Long id,
            Long estudianteId, String estudiante, Long profesorId,
            String materia, BigDecimal valor, String observacion, Instant creadaEn,
            @Schema(description = "Confirmación de la operación.", example = "Nota agregada correctamente.") String mensaje) {
        public static NotaConfirmada de(NotaVista n, String mensaje) {
            return new NotaConfirmada(n.id(), n.estudianteId(), n.estudiante(), n.profesorId(),
                    n.materia(), n.valor(), n.observacion(), n.creadaEn(), mensaje);
        }
    }

    public record CrearTrabajo(
            @NotBlank @Size(max = 30) @Schema(description = "Grupo al que se publicará la tarea.", example = "11-2") String grupo,
            @NotBlank @Size(max = 160) @Schema(description = "Título del trabajo que verán los estudiantes.", example = "Taller de ecuaciones") String titulo,
            @NotBlank @Size(max = 4000) @Schema(description = "Instrucciones para realizar el trabajo.", example = "Resolver los ejercicios del 1 al 5.") String descripcion,
            @Schema(description = "Fecha límite opcional con hora; usa el formato ISO, p. ej. 2026-11-20T20:00:00Z.", example = "2026-11-20T20:00:00Z") Instant fechaEntrega
    ) { }

    public record TrabajoVista(@Schema(description = "ID interno de este trabajo.") Long id, Long profesorId, String profesor, String materia,
                               String grupo, String titulo, String descripcion,
                               Instant fechaEntrega, Instant creadaEn) {
        public static TrabajoVista de(Trabajo t) {
            return new TrabajoVista(t.getId(), t.getProfesor().getId(), t.getProfesor().getNombre(),
                    t.getProfesor().getMateria(), t.getGrupo(), t.getTitulo(),
                    t.getDescripcion(), t.getFechaEntrega(), t.getCreadaEn());
        }
    }

    public record IniciarClase(
            @NotBlank @Size(max = 30) @Schema(description = "Salón al que se invitará a la reunión.", example = "11-2") String grupo,
            @NotBlank @Size(max = 160) @Schema(description = "Nombre de la clase virtual.", example = "Clase de trigonometría") String titulo
    ) { }

    public record UrlGrabacion(@NotBlank @Size(max = 2048)
                               @Schema(description = "Enlace HTTPS a una grabación YA existente. Este sistema no graba la clase automáticamente.", example = "https://ejemplo.edu/grabaciones/abc123") String url) { }

    public record ClaseVista(@Schema(description = "ID de ESTA CLASE; úsalo para finalizarla o registrar una grabación.") Long id, Long profesorId, String profesor, String materia,
                             String grupo, String titulo, String enlace,
                             EstadoClase estado, Instant iniciadaEn, Instant finalizadaEn,
                             String urlGrabacion, EstadoNotificacion estadoNotificacion,
                             int correosEnviados, int correosFallidos) {
        public static ClaseVista de(ClaseVirtual c) {
            return new ClaseVista(c.getId(), c.getProfesor().getId(), c.getProfesor().getNombre(),
                    c.getProfesor().getMateria(), c.getGrupo(), c.getTitulo(), c.getEnlace(),
                    c.getEstado(), c.getIniciadaEn(), c.getFinalizadaEn(),
                    c.getUrlGrabacion(), c.getEstadoNotificacion(),
                    c.getCorreosEnviados(), c.getCorreosFallidos());
        }
    }

    public record QrVista(
            @Schema(description = "Texto firmado que codifica el QR. Copia este valor en la prueba de escaneo.") String contenido,
            @Schema(description = "Imagen PNG codificada en Base64 para mostrarla en una página.") String imagenBase64,
            String mimeType, Instant expiraEn) { }
    public record EscanearQr(@NotBlank @Schema(description = "Texto completo leído del código QR (token firmado). También acepta los QR antiguos en JSON.") String contenido) { }
    public record EstudianteEscaneado(Long estudianteId, String nombre, String grupo, String materiaProfesor) { }

    public record ErrorApi(Instant instante, int codigo, String mensaje) { }
}
