package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.security.UsuarioActual;
import com.ejemplo.colegio.service.ClasesService;
import com.ejemplo.colegio.service.NotasService;
import com.ejemplo.colegio.service.QrService;
import com.ejemplo.colegio.service.TrabajosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@PreAuthorize("hasRole('ESTUDIANTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "03. Estudiantes", description = "Perfil, QR, calificaciones, trabajos, clases y grabaciones")
public class EstudianteController {
    private final UsuarioActual actual;
    private final NotasService notas;
    private final TrabajosService trabajos;
    private final ClasesService clases;
    private final QrService qr;

    public EstudianteController(UsuarioActual actual, NotasService notas,
            TrabajosService trabajos, ClasesService clases, QrService qr) {
        this.actual = actual;
        this.notas = notas;
        this.trabajos = trabajos;
        this.clases = clases;
        this.qr = qr;
    }

    @GetMapping({"/yo", "/me"})
    @Operation(summary = "Consultar mi perfil")
    public EstudianteVista miPerfil() { return EstudianteVista.de(actual.estudiante()); }

    @GetMapping({"/qr", "/me/qr"})
    @Operation(summary = "Mostrar mi QR personal", description = "Solo para estudiantes: devuelve el texto firmado y una imagen PNG (Base64) del QR. En el panel web el código se muestra automáticamente al abrir «Mi QR». Para probarlo sin registrar una cuenta, entra primero con el estudiante de ejemplo 9002001 mediante POST /api/demo/entrar y autoriza el token.")
    public QrVista generarQr() { return qr.generar(); }

    @GetMapping(value = {"/qr.png", "/me/qr.png"}, produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Descargar la imagen de mi QR (PNG)", description = "El navegador puede mostrarla o guardarla. Necesita autorización de ESTUDIANTE: en Swagger pulsa «Authorize» con el token de un alumno.")
    @ApiResponse(responseCode = "200", description = "Imagen PNG del QR",
            content = @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> descargarQrPng() {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr.generarPng());
    }

    @GetMapping({"/notas", "/me/notas"})
    @Operation(summary = "Consultar mis notas y la materia correspondiente")
    public List<NotaVista> misNotas() { return notas.notasDelEstudiante(); }

    @GetMapping("/notas/resumen")
    @Operation(summary = "Promedio por materia", description = "Media aritmetica (0 a 5), aprobado desde 3.0. Sin porcentajes.")
    public List<ResumenMateria> resumenNotas() { return notas.resumenPorMateria(); }

    @GetMapping({"/trabajos", "/me/trabajos"})
    @Operation(summary = "Consultar tareas de mi grupo", description = "El grupo se obtiene del JWT y de la base de datos; el alumno no puede cambiarlo en la peticion.")
    public List<TrabajoVista> misTrabajos() { return trabajos.misTrabajosComoEstudiante(); }

    @GetMapping({"/clases/activas", "/me/clases/activas"})
    @Operation(summary = "Consultar clases virtuales activas de mi grupo")
    public List<ClaseVista> clasesActivas() { return clases.clasesActivasDeEstudiante(); }

    @GetMapping({"/clases/grabaciones", "/me/clases/grabadas"})
    @Operation(summary = "Consultar grabaciones de clases de mi grupo")
    public List<ClaseVista> grabaciones() { return clases.grabacionesDeEstudiante(); }
}
