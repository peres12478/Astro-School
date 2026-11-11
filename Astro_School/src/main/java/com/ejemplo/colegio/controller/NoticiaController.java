package com.ejemplo.colegio.controller;

import com.ejemplo.colegio.service.NoticiasService;
import com.ejemplo.colegio.service.NoticiasService.NoticiaVista;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/noticias")
@Tag(name = "07. Noticiero Astro", description = "Noticias y archivos compartidos publicamente. En la demo cualquier visitante puede publicar; limita este acceso en produccion.")
public class NoticiaController {
    private final NoticiasService noticias;
    public NoticiaController(NoticiasService noticias) { this.noticias = noticias; }

    @GetMapping
    @Operation(summary = "Consultar las 50 noticias mas recientes (sin iniciar sesion)")
    public List<NoticiaVista> listar() { return noticias.listar(); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una noticia por ID (publico)")
    public NoticiaVista consultar(@PathVariable Long id) { return noticias.ver(id); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publicar una noticia con archivo opcional",
            description = "Publico en modo demostracion. Completa titulo, descripcion, autor y opcionalmente un archivo PDF, JPG, PNG, TXT, DOCX, XLSX o PPTX de hasta 8 MB. No se admiten HTML, SVG, ejecutables ni scripts.")
    @ApiResponse(responseCode = "201", description = "Noticia publicada")
    public ResponseEntity<NoticiaVista> publicar(@RequestParam String titulo,
            @RequestParam String descripcion, @RequestParam String autor,
            @RequestPart(name = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noticias.publicar(titulo, descripcion, autor, archivo));
    }

    @GetMapping("/{id}/archivo")
    @Operation(summary = "Descargar el archivo de una noticia (publico)")
    @ApiResponse(responseCode = "200", description = "Archivo adjunto",
            content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        var adjunto = noticias.descargar(id);
        // Descargar (no ejecutar) contenidos enviados por visitantes del sitio.
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(adjunto.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(adjunto.tipo()))
                .body(adjunto.recurso());
    }
}
