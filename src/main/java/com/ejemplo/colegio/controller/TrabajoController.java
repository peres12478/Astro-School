package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.service.TrabajosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/trabajos")
@PreAuthorize("hasRole('PROFESOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "05. Trabajos y tareas", description = "Publicar y consultar actividades para un grupo.")
public class TrabajoController {
    private final TrabajosService trabajos;

    public TrabajoController(TrabajosService trabajos) { this.trabajos = trabajos; }

    @PostMapping
    @Operation(summary = "Publicar una tarea para un salón", description = "Especifica el grupo, título, descripción y, si quieres, la fecha límite. El profesor y la materia se asignan automáticamente.")
    @ApiResponse(responseCode = "201", description = "Tarea creada")
    public ResponseEntity<TrabajoVista> crear(@Valid @RequestBody CrearTrabajo dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trabajos.crear(dto));
    }

    @GetMapping("/mis")
    @Operation(summary = "Mis tareas publicadas como profesor")
    public List<TrabajoVista> listar() { return trabajos.misTrabajosComoProfesor(); }
}
