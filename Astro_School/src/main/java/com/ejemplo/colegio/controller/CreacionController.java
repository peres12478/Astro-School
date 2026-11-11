package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.domain.CursoCodigo;
import com.ejemplo.colegio.service.CreacionesService;
import com.ejemplo.colegio.service.CreacionesService.CreacionVista;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/creaciones")
@Tag(name = "09. Nuestras creaciones", description = "Galeria PUBLICA de proyectos realizados por estudiantes de Robotica, Fisica y Astronomia.")
public class CreacionController {
    private final CreacionesService creaciones;
    public CreacionController(CreacionesService creaciones) { this.creaciones = creaciones; }

    @GetMapping
    @Operation(summary = "Ver los 100 proyectos mas recientes (publico)",
            description = "Se puede filtrar con ?curso=ROBOTICA, ?curso=FISICA o ?curso=ASTRONOMIA. Sin parametro incluye los tres.")
    public List<CreacionVista> listar(@RequestParam(required = false) CursoCodigo curso) {
        return creaciones.listar(curso);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle de un proyecto (publico)")
    public CreacionVista consultar(@PathVariable Long id) { return creaciones.consultar(id); }

    @GetMapping("/{id}/archivo")
    @Operation(summary = "Ver imagen o descargar adjunto de un proyecto (publico)", description = "PNG/JPG se muestran en linea para la galeria; los demas formatos se descargan.")
    @ApiResponse(responseCode = "200", description = "Archivo adjunto",
            content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<Resource> archivo(@PathVariable Long id) {
        var a = creaciones.descargar(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (a.tipo().equals("image/png") || a.tipo().equals("image/jpeg")
                            ? ContentDisposition.inline() : ContentDisposition.attachment())
                            .filename(a.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(a.tipo()))
                .body(a.recurso());
    }
}
