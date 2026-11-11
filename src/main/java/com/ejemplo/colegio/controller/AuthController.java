package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.service.AuthService;
import com.ejemplo.colegio.service.RecuperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "01. Acceso y registro", description = "Empieza aquí: registra una cuenta, inicia sesión o recupera tu contraseña.")
public class AuthController {
    private final AuthService auth;
    private final RecuperacionService recuperacion;

    public AuthController(AuthService auth, RecuperacionService recuperacion) {
        this.auth = auth;
        this.recuperacion = recuperacion;
    }

    @PostMapping({"/registro/profesores", "/registro/profesor"})
    @Operation(summary = "Crear cuenta de profesor", description = "La clave de registro docente NO es la contraseña personal del profesor. Escríbela en X-Clave-Docente. Está configurada en application.yml o CLAVE_REGISTRO_DOCENTE.")
    @ApiResponse(responseCode = "201", description = "Profesor registrado")
    @ApiResponse(responseCode = "403", description = "Clave de registro incorrecta")
    @ApiResponse(responseCode = "409", description = "Identificacion o correo duplicados")
    public ResponseEntity<AuthRespuesta> registrarProfesor(
            @Parameter(description = "Clave especial para autorizar el ALTA de un profesor. No es la contraseña de su cuenta.", required = true)
            @RequestHeader("X-Clave-Docente") String clave,
            @Valid @RequestBody RegistroProfesor body) {
        AuthRespuesta resultado = auth.registrarProfesor(body, clave);
        return ResponseEntity.created(URI.create("/api/profesores/yo")).body(resultado);
    }

    @PostMapping({"/registro/estudiantes", "/registro/estudiante"})
    @Operation(summary = "Registrar estudiante", description = "Crea la cuenta y devuelve un JWT. El auto-registro de estudiantes es solo para demostracion.")
    @ApiResponse(responseCode = "201", description = "Estudiante registrado")
    @ApiResponse(responseCode = "409", description = "Identificacion o correo duplicados")
    public ResponseEntity<AuthRespuesta> registrarEstudiante(@Valid @RequestBody RegistroEstudiante body) {
        AuthRespuesta resultado = auth.registrarEstudiante(body);
        return ResponseEntity.created(URI.create("/api/estudiantes/yo")).body(resultado);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con correo O documento", description = "En usuario escribe tu correo electrónico o tu cédula/tarjeta de identidad (NO el ID de la tabla). Añade la contraseña personal. Ejemplo: usuario=9001001 y password=Demo2026! (solo en la demostración H2). Si la respuesta incluye un token, la sesión quedó iniciada. En Swagger pulsa «Authorize» e introduce ese token para consultar las funciones privadas. En el panel web se hace automáticamente.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada correctamente; devuelve token, usuario, rol y mensaje")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    public AuthRespuesta login(@Valid @RequestBody Login body) {
        return auth.login(body);
    }

    @PostMapping("/olvide-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pedir recuperación de contraseña", description = "Si el correo está registrado y SMTP está bien configurado, se enviarán instrucciones. La respuesta NO confirma la entrega del correo.")
    @ApiResponse(responseCode = "202", description = "Solicitud recibida")
    public Mensaje olvidePassword(@Valid @RequestBody RecuperarPassword body) {
        return recuperacion.solicitar(body);
    }

    @PostMapping("/restablecer-password")
    @Operation(summary = "Restablecer contrasena", description = "Consume el token recibido por correo. Invalida los JWT de acceso emitidos antes del cambio de contrasena.")
    @ApiResponse(responseCode = "200", description = "Contrasena modificada")
    @ApiResponse(responseCode = "400", description = "Token invalido, ya usado o expirado")
    public Mensaje restablecerPassword(@Valid @RequestBody RestablecerPassword body) {
        return recuperacion.restablecer(body);
    }
}
