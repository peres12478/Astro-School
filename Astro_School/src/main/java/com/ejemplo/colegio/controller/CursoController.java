package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.domain.CursoCodigo;
import com.ejemplo.colegio.service.CursosService;
import com.ejemplo.colegio.service.CursosService.CursoVista;
import com.ejemplo.colegio.service.CursosService.InscripcionVista;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@Tag(name = "08. Cursos Astro", description = "Catalogo publico de Robotica, Fisica y Astronomia. Un estudiante solo puede tener UNA inscripcion activa.")
public class CursoController {
    public record ElegirCurso(@NotNull CursoCodigo curso) { }

    private final CursosService cursos;
    public CursoController(CursosService cursos) { this.cursos = cursos; }

    @GetMapping
    @Operation(summary = "Ver los tres cursos y su numero de inscritos (publico)")
    public List<CursoVista> listar() { return cursos.catalogo(); }

    @GetMapping("/mi-inscripcion")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Ver mi inscripcion", description = "Requiere token de ESTUDIANTE. Devuelve null si aun no hay inscripcion.")
    public ResponseEntity<InscripcionVista> mia() {
        InscripcionVista vista = cursos.miInscripcion();
        return vista == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(vista);
    }

    @PostMapping("/inscripciones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Inscribirme en UNO de los tres cursos",
            description = "Requiere token ESTUDIANTE y cuerpo {\"curso\":\"ROBOTICA\"}. Devuelve 409 si ya pertenece a otro curso.")
    public ResponseEntity<InscripcionVista> inscribirme(@Valid @RequestBody ElegirCurso datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursos.inscribirme(datos.curso()));
    }

    @DeleteMapping("/mi-inscripcion")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancelar mi inscripcion para poder elegir otro curso")
    public ResponseEntity<Void> cancelar() {
        cursos.cancelarMiInscripcion();
        return ResponseEntity.noContent().build();
    }
}
