package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.domain.CursoCodigo;
import com.ejemplo.colegio.service.CreacionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/astronomia")
@PreAuthorize("hasRole('ESTUDIANTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "12. Observatorio de Astronomia", description = "Espacio exclusivo para estudiantes inscritos en Astronomia. Publica observaciones e investigaciones.")
public class AstronomiaController {
    private final CreacionesService creaciones;
    public AstronomiaController(CreacionesService creaciones) { this.creaciones = creaciones; }
    @GetMapping("/mi-espacio")
    @Operation(summary = "Consultar mis proyectos de Astronomia")
    public CreacionesService.EspacioVista espacio() { return creaciones.espacio(CursoCodigo.ASTRONOMIA); }

    @PostMapping(value = "/proyectos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publicar un proyecto astronomico en Nuestras creaciones")
    public ResponseEntity<CreacionesService.CreacionVista> publicar(@RequestParam String titulo,
            @RequestParam String descripcion, @RequestPart(name = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creaciones.publicar(CursoCodigo.ASTRONOMIA, titulo, descripcion, archivo));
    }
}
