package com.ejemplo.colegio.support;

import com.ejemplo.colegio.dto.Dtos.ErrorApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.validation.FieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorApi> api(ApiException e) {
        return responder(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validar(MethodArgumentNotValidException e) {
        String detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> nombreCampo(f.getField()) + ": " + descripcionError(f))
                .distinct().collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, detalles);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorApi> peticionInvalida(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos incorrectos. Comprueba los campos e inténtalo de nuevo.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> conflictoDatos(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, "Ya existe un registro con esos datos o hay una referencia incorrecta.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> accesoDenegado(AccessDeniedException e) {
        return responder(HttpStatus.FORBIDDEN, "Tu cuenta no tiene permiso para realizar esta operación.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception e) {
        log.error("Error de servidor", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Inténtalo de nuevo.");
    }

    private static String nombreCampo(String campo) {
        return switch (campo) {
            case "estudianteId" -> "Estudiante (ID interno)";
            case "identificacion" -> "Número de documento";
            case "valor" -> "Nota";
            case "nombre" -> "Nombre";
            case "grupo" -> "Grupo o salón";
            case "materia" -> "Materia";
            case "correo" -> "Correo electrónico";
            case "password", "nuevaPassword" -> "Contraseña";
            case "contenido" -> "Contenido del código QR";
            case "observacion" -> "Observación";
            case "titulo" -> "Título";
            case "descripcion" -> "Descripción";
            case "url" -> "Enlace URL";
            default -> campo;
        };
    }

    private static String descripcionError(FieldError error) {
        String codigo = error.getCode();
        if (codigo == null) return "valor incorrecto";
        return switch (codigo) {
            case "NotBlank", "NotNull" -> "este campo es obligatorio";
            case "Email" -> "escribe un correo válido";
            case "Positive" -> "escribe un ID mayor que cero";
            case "DecimalMin", "DecimalMax" -> "debe estar entre 0 y 5";
            case "Digits" -> "solo se permiten dos decimales";
            case "Size" -> "la longitud no cumple los límites permitidos";
            default -> "valor incorrecto";
        };
    }

    private ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado)
                .body(new ErrorApi(Instant.now(), estado.value(), mensaje));
    }
}
