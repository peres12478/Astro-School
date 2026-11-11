package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.domain.CursoCodigo;
import com.ejemplo.colegio.service.CreacionesService;
import com.ejemplo.colegio.service.RobotsService;
import com.ejemplo.colegio.service.RobotsService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/robotica")
@PreAuthorize("hasRole('ESTUDIANTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "10. Taller de Robotica", description = "EXCLUSIVO de estudiantes inscritos en Robotica. Registra robots, escribe codigo y sube proyectos; el codigo NO se ejecuta en el servidor.")
public class RoboticaController {
    private final RobotsService robots;
    private final CreacionesService creaciones;
    public RoboticaController(RobotsService robots, CreacionesService creaciones) {
        this.robots = robots; this.creaciones = creaciones;
    }
    @GetMapping("/mi-espacio")
    @Operation(summary = "Mi espacio de Robotica y mis proyectos")
    public CreacionesService.EspacioVista espacio() { return creaciones.espacio(CursoCodigo.ROBOTICA); }

    @GetMapping("/robots")
    @Operation(summary = "Listar mis robots registrados")
    public List<RobotVista> listar() { return robots.misRobots(); }

    @PostMapping("/robots")
    @Operation(summary = "Registrar un robot propio")
    public ResponseEntity<RobotVista> crear(@Valid @RequestBody NuevoRobot datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(robots.registrar(datos));
    }

    @GetMapping("/robots/{robotId}/codigos")
    @Operation(summary = "Ver versiones de codigo de MI robot")
    public List<CodigoVista> codigos(@PathVariable Long robotId) { return robots.listarCodigos(robotId); }

    @PostMapping("/robots/{robotId}/codigos")
    @Operation(summary = "Escribir y guardar un programa desde el editor (JSON)",
            description = "Guarda una version. No ejecuta ni compila programas. Lenguajes: ARDUINO, PYTHON, C_CPP, JAVASCRIPT, TEXTO. Hasta 100000 caracteres.")
    public ResponseEntity<CodigoVista> escribir(@PathVariable Long robotId, @Valid @RequestBody NuevoCodigo datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(robots.guardarCodigo(robotId, datos));
    }

    @PostMapping(value = "/robots/{robotId}/codigos/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo de codigo fuente a MI robot",
            description = "Solo texto UTF-8 de hasta 100 KB: .ino, .py, .cpp, .c, .h, .hpp, .js, .txt. Se almacena sin ejecutar.")
    public ResponseEntity<CodigoVista> subir(@PathVariable Long robotId, @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(robots.subirCodigo(robotId, archivo));
    }

    @GetMapping("/robots/{robotId}/codigos/{codigoId}/descargar")
    @Operation(summary = "Descargar MI programa como archivo de texto")
    public ResponseEntity<byte[]> descargar(@PathVariable Long robotId, @PathVariable Long codigoId) {
        var archivo = robots.descargar(robotId, codigoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.TEXT_PLAIN)
                .body(archivo.bytes());
    }

    @PostMapping(value = "/proyectos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publicar mi proyecto de Robotica en Nuestras creaciones")
    public ResponseEntity<CreacionesService.CreacionVista> publicar(@RequestParam String titulo,
            @RequestParam String descripcion, @RequestPart(name = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creaciones.publicar(CursoCodigo.ROBOTICA, titulo, descripcion, archivo));
    }
}
