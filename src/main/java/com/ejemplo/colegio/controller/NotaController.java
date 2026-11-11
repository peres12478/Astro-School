package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.service.NotasService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/notas")
@PreAuthorize("hasRole('PROFESOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "04. Calificaciones", description = "Profesor: asignar una nota y modificarla usando el ID de esa nota, no el del alumno.")
public class NotaController {
    private final NotasService notas;

    public NotaController(NotasService notas) { this.notas = notas; }

    @PostMapping
    @Operation(summary = "Agregar nota a un estudiante", description = "1. Busca al alumno en «Profesores → Listar estudiantes». 2. Copia su campo id en estudianteId. 3. Indica valor (0 a 5) y, opcionalmente, observación. La materia se asigna automáticamente.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Nota creada"),
            @ApiResponse(responseCode = "400", description = "Nota fuera de rango"),
            @ApiResponse(responseCode = "404", description = "Estudiante inexistente")})
    public ResponseEntity<NotaConfirmada> crear(@Valid @RequestBody CrearNota dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(NotaConfirmada.de(notas.asignar(dto), "Nota agregada correctamente."));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una nota que ya registré", description = "El parámetro id es el ID de la NOTA (campo id en una nota ya creada). NO es el ID del estudiante. Puedes ver estos IDs en «Consultar notas de un estudiante». Solo se pueden cambiar notas propias.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Nota modificada"),
            @ApiResponse(responseCode = "404", description = "La nota no pertenece al profesor")})
    public NotaConfirmada modificar(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID de la NOTA a modificar; búscalo en el campo id de las notas anteriores (no es estudianteId).", example = "7", required = true)
            @PathVariable Long id, @Valid @RequestBody ActualizarNota dto) {
        return NotaConfirmada.de(notas.modificar(id, dto), "Nota modificada correctamente.");
    }

    @GetMapping("/estudiante/{estudianteId}")
    @Operation(summary = "Ver notas que puse a un estudiante", description = "Aquí escribes el ID del ESTUDIANTE. En la respuesta cada nota tiene su propio campo id, que es el que se usa para editarla.")
    public List<NotaVista> listarPorEstudiante(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID interno del ESTUDIANTE; sale en «Listar estudiantes» (no es la cédula).", example = "1", required = true)
            @PathVariable Long estudianteId) {
        return notas.notasDelProfesorPara(estudianteId);
    }
}
