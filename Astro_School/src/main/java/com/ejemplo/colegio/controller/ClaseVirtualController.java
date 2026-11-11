package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.service.ClasesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clases")
@PreAuthorize("hasRole('PROFESOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "06. Clases virtuales", description = "Crear una reunión, ver invitaciones enviadas, finalizarla y adjuntar su grabación.")
public class ClaseVirtualController {
    private final ClasesService clases;

    public ClaseVirtualController(ClasesService clases) { this.clases = clases; }

    @PostMapping
    @Operation(summary = "Iniciar clase virtual", description = "Genera una URL Jitsi impredecible, guarda la clase y envia un correo a cada estudiante del grupo DESPUES de confirmar la transaccion. El estado de notificaciones se consulta en GET /api/clases/mis.")
    @ApiResponse(responseCode = "201", description = "Clase iniciada; correos en proceso asincrono")
    public ResponseEntity<ClaseVista> iniciar(@Valid @RequestBody IniciarClase dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clases.iniciar(dto));
    }

    @GetMapping("/mis")
    @Operation(summary = "Ver mis clases y las notificaciones", description = "Cada clase muestra su ID, enlace, estado y los contadores de correos aceptados o fallidos. «ENVIADA» no significa que un alumno abrió el correo.")
    public List<ClaseVista> mias() { return clases.clasesDeProfesor(); }

    @PatchMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar clase", description = "Solo puede finalizarla el profesor que la inicio.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Clase finalizada"),
            @ApiResponse(responseCode = "403", description = "Clase de otro profesor")})
    public ClaseVista finalizar(
            @Parameter(description = "ID de la CLASE que vas a finalizar. Lo encuentras en «Mis clases creadas»; no es el ID del profesor.", example = "3", required = true) @PathVariable Long id) { return clases.finalizar(id); }

    @PutMapping("/{id}/grabacion")
    @Operation(summary = "Guardar el enlace de la grabación", description = "Primero finaliza la clase. Luego usa su ID (campo id de «Mis clases creadas») y pega el enlace HTTPS de una grabación que ya exista. El sistema no graba automáticamente.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Grabacion registrada"),
            @ApiResponse(responseCode = "409", description = "La clase sigue activa")})
    public ClaseVista grabar(
            @Parameter(description = "ID de la CLASE ya finalizada. Se ve en «Mis clases creadas».", example = "3", required = true) @PathVariable Long id, @Valid @RequestBody UrlGrabacion dto) {
        return clases.registrarGrabacion(id, dto);
    }
}
