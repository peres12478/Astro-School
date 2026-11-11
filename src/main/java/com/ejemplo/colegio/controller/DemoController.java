package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.AuthRespuesta;
import com.ejemplo.colegio.service.AuthService;
import com.ejemplo.colegio.service.DemoCuentas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.http.HttpStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Solo se publica cuando el perfil H2 local tiene activado el modo demo. */
@RestController
@RequestMapping("/api/demo")
@Tag(name = "00. Prueba rápida", description = "Usa cuentas inventadas: entra con un clic, sin registrar usuarios ni recordar contraseñas. Solo se activa en H2 local.")
@Profile("h2")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoController {
    private final AuthService auth;
    public DemoController(AuthService auth) { this.auth = auth; }

    @GetMapping("/cuentas")
    @Operation(summary = "Ver las cuatro cuentas de ejemplo", description = "Dos profesores y dos estudiantes del grupo 11-2. Muestra documento y correo; nunca datos personales reales.")
    public List<DemoCuentas.Cuenta> cuentas(@Parameter(hidden = true) HttpServletRequest request) {
        soloDesdeEsteComputador(request);
        return DemoCuentas.CUENTAS;
    }

    public record EntrarDemo(
            @NotBlank @Schema(description = "Documento FICTICIO de una cuenta de prueba. Ejemplo: 9001001 profesor, 9002001 estudiante; no es un ID interno.", example = "9001001")
            String identificacion) { }

    @PostMapping("/entrar")
    @Operation(summary = "Probar como profesor o estudiante, sin registrar una cuenta", description = "Selecciona una identificación de GET /api/demo/cuentas (por ejemplo 9001001 para profesor o 9002001 para estudiante). Devuelve un token de prueba; pega el token en «Authorize» si usas Swagger. En la web, los botones realizan este paso automáticamente. Disponible solo con app.demo.enabled=true y perfil h2.")
    public AuthRespuesta entrar(@Valid @RequestBody EntrarDemo body,
            @Parameter(hidden = true) HttpServletRequest request) {
        soloDesdeEsteComputador(request);
        return auth.loginDemo(body.identificacion());
    }

    private void soloDesdeEsteComputador(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!"127.0.0.1".equals(ip) && !"::1".equals(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip))
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "El acceso de demostración solo está disponible desde este computador");
    }
}
