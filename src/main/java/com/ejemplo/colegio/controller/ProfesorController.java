package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.EstudianteRepository;
import com.ejemplo.colegio.security.UsuarioActual;
import com.ejemplo.colegio.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profesores")
@PreAuthorize("hasRole('PROFESOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "02. Profesores", description = "Consulta tu perfil y busca alumnos de un grupo. Para calificar usa también «04. Calificaciones».")
public class ProfesorController {
    private final UsuarioActual actual;
    private final EstudianteRepository estudiantes;
    private final NotasService notas;
    private final TrabajosService trabajos;
    private final ClasesService clases;
    private final QrService qr;

    public ProfesorController(UsuarioActual actual, EstudianteRepository estudiantes,
            NotasService notas, TrabajosService trabajos, ClasesService clases, QrService qr) {
        this.actual = actual;
        this.estudiantes = estudiantes;
        this.notas = notas;
        this.trabajos = trabajos;
        this.clases = clases;
        this.qr = qr;
    }

    @GetMapping({"/yo", "/me"})
    @Operation(summary = "Obtener mi perfil de profesor")
    public ProfesorVista miPerfil() { return ProfesorVista.de(actual.profesor()); }

    @GetMapping("/estudiantes")
    @Operation(summary = "Buscar estudiantes de un salón", description = "Escribe el grupo (por ejemplo 11-2). La respuesta muestra el campo id de cada estudiante: ese es el estudianteId que necesitas para agregarle notas.")
    public List<EstudianteVista> estudiantesPorGrupo(
            @Parameter(description = "Número o nombre del salón que quieres consultar (ejemplo: 11-2).", required = true, example = "11-2")
            @RequestParam String grupo) {
        actual.profesor();
        return estudiantes.findByGrupoOrderByNombreAsc(AuthService.normalizarGrupo(grupo))
                .stream().map(EstudianteVista::de).toList();
    }

    @GetMapping("/grupos/{grupo}/estudiantes")
    @Operation(summary = "Buscar estudiantes por grupo (alternativa)")
    @Hidden
    public List<EstudianteVista> estudiantesPorRuta(
            @Parameter(description = "Salón o curso, por ejemplo 11-2.", example = "11-2", required = true) @PathVariable String grupo) {
        return estudiantesPorGrupo(grupo);
    }

    @PostMapping("/qr/escanear")
    @Operation(summary = "Identificar estudiante con su QR", description = "Pega en contenido el texto que lee el QR del estudiante (token firmado). Se verifica su autenticidad y vencimiento. También acepta QR antiguos en JSON. No sirve como contraseña del profesor.")
    @ApiResponse(responseCode = "400", description = "QR alterado o vencido")
    public EstudianteEscaneado escanear(@Valid @RequestBody EscanearQr body) {
        return qr.escanear(body);
    }

    @PostMapping("/notas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asignar nota a estudiante", description = "La materia se toma SIEMPRE del profesor autenticado (no viene en el JSON). Valor entre 0 y 5.")
    @ApiResponse(responseCode = "201", description = "Nota creada")
    @Hidden
    public NotaConfirmada asignarNota(@Valid @RequestBody CrearNota body) {
        return NotaConfirmada.de(notas.asignar(body), "Nota agregada correctamente.");
    }

    @PostMapping("/qr/calificar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Escanear QR y calificar en una sola operacion", description = "Valida la firma del QR, identifica al estudiante y registra una nota de la materia del profesor autenticado.")
    @ApiResponse(responseCode = "201", description = "Nota creada desde QR")
    public NotaConfirmada calificarQr(@Valid @RequestBody CalificarQr body) {
        return NotaConfirmada.de(notas.asignarPorQr(body), "Nota agregada correctamente desde el código QR.");
    }

    @PatchMapping("/notas/{notaId}")
    @Operation(summary = "Modificar una nota (ruta alternativa)", description = "notaId = ID de la NOTA que quieres cambiar. Lo ves al consultar las notas del estudiante.")
    @Hidden
    public NotaConfirmada modificarNota(
            @Parameter(description = "ID de la NOTA específica que se quiere editar; no es el ID del estudiante.", example = "7", required = true)
            @PathVariable Long notaId, @Valid @RequestBody ActualizarNota body) {
        return NotaConfirmada.de(notas.modificar(notaId, body), "Nota modificada correctamente.");
    }

    @GetMapping("/estudiantes/{estudianteId}/notas")
    @Operation(summary = "Consultar notas que puse a un estudiante", description = "estudianteId es el ID del alumno obtenido al listar estudiantes.")
    @Hidden
    public List<NotaVista> misNotasDeEstudiante(
            @Parameter(description = "ID interno del estudiante que aparece al listar el grupo.", example = "1", required = true)
            @PathVariable Long estudianteId) {
        return notas.notasDelProfesorPara(estudianteId);
    }

    @PostMapping("/trabajos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publicar tarea para un grupo", description = "La tarea se publica solo para el grupo indicado; queda asociada al profesor y materia actual.")
    @Hidden
    public TrabajoVista crearTrabajo(@Valid @RequestBody CrearTrabajo body) {
        return trabajos.crear(body);
    }

    @GetMapping("/trabajos")
    @Operation(summary = "Consultar mis tareas publicadas")
    @Hidden
    public List<TrabajoVista> misTrabajos() { return trabajos.misTrabajosComoProfesor(); }

    @PostMapping("/clases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar clase virtual", description = "Genera sala Jitsi y programa correos automaticos a TODOS los alumnos del grupo. La respuesta inicial muestra notificacion PENDIENTE; luego consultar GET /clases.")
    @ApiResponse(responseCode = "201", description = "Clase iniciada; correos pendientes de entrega")
    @Hidden
    public ClaseVista iniciarClase(@Valid @RequestBody IniciarClase body) {
        return clases.iniciar(body);
    }

    @GetMapping("/clases")
    @Operation(summary = "Consultar mis clases y estado del envio de correos")
    @Hidden
    public List<ClaseVista> misClases() { return clases.clasesDeProfesor(); }

    @PatchMapping("/clases/{claseId}/finalizar")
    @Operation(summary = "Finalizar una clase propia")
    @Hidden
    public ClaseVista finalizarClase(
            @Parameter(description = "ID de la CLASE que deseas finalizar; sale al consultar tus clases.", example = "3", required = true)
            @PathVariable Long claseId) { return clases.finalizar(claseId); }

    @PatchMapping("/clases/{claseId}/grabacion")
    @Operation(summary = "Registrar URL de grabacion", description = "La clase debe estar finalizada. Solo admite URL HTTPS; no graba el video automaticamente.")
    @Hidden
    public ClaseVista registrarGrabacion(
            @Parameter(description = "ID de la CLASE ya finalizada, no del profesor.", example = "3", required = true)
            @PathVariable Long claseId, @Valid @RequestBody UrlGrabacion body) {
        return clases.registrarGrabacion(claseId, body);
    }
}
