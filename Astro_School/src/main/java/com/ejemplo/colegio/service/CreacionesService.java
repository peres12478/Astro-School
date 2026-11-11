package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.repository.CreacionRepository;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@Service
public class CreacionesService {
    public record CreacionVista(Long id, CursoCodigo curso, String cursoNombre,
            String titulo, String descripcion, String autor, Instant creadaEn,
            String nombreArchivo, Long bytesArchivo, String urlArchivo) {
        static CreacionVista de(Creacion c) {
            return new CreacionVista(c.getId(), c.getCurso(), c.getCurso().getNombre(), c.getTitulo(),
                    c.getDescripcion(), c.getEstudiante().getNombre(), c.getCreadaEn(),
                    c.getNombreArchivo(), c.getBytesArchivo(), c.getClaveArchivo() == null ? null
                            : "/api/creaciones/" + c.getId() + "/archivo");
        }
    }
    public record EspacioVista(CursoCodigo curso, String nombre, String mensaje, List<CreacionVista> misProyectos) { }
    public record Descarga(Resource recurso, String nombre, String tipo) { }
    private record ArchivoGuardado(String nombre, String tipo, String clave, Long bytes, Path ruta) { }

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Map<String, String> TIPOS = Map.of(
            ".pdf", "application/pdf", ".png", "image/png", ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg", ".txt", "text/plain; charset=utf-8",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    private final CreacionRepository creaciones;
    private final CursosService cursos;
    private final Path carpeta;

    public CreacionesService(CreacionRepository creaciones, CursosService cursos,
            @Value("${app.creaciones.directorio:./data/creaciones}") String carpeta) {
        this.creaciones = creaciones; this.cursos = cursos;
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
    }
    @Transactional(readOnly = true)
    public List<CreacionVista> listar(CursoCodigo curso) {
        return (curso == null ? creaciones.findTop100ByOrderByCreadaEnDescIdDesc()
                : creaciones.findTop100ByCursoOrderByCreadaEnDescIdDesc(curso))
                .stream().map(CreacionVista::de).toList();
    }
    @Transactional(readOnly = true)
    public CreacionVista consultar(Long id) { return CreacionVista.de(buscar(id)); }
    private Creacion buscar(Long id) {
        return creaciones.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }
    @Transactional(readOnly = true)
    public EspacioVista espacio(CursoCodigo curso) {
        Estudiante estudiante = cursos.exigirMiembro(curso);
        String mensaje = switch (curso) {
            case ROBOTICA -> "Registra tus robots, guarda programas y comparte tus prototipos.";
            case FISICA -> "Documenta experimentos, hipotesis y resultados.";
            case ASTRONOMIA -> "Presenta observaciones, investigaciones y fotografias astronomicas.";
        };
        return new EspacioVista(curso, curso.getNombre(), mensaje, misProyectos(estudiante.getId(), curso));
    }
    private List<CreacionVista> misProyectos(Long estudianteId, CursoCodigo curso) {
        return creaciones.findByEstudianteIdAndCursoOrderByCreadaEnDescIdDesc(estudianteId, curso)
                .stream().map(CreacionVista::de).toList();
    }
    @Transactional
    public CreacionVista publicar(CursoCodigo curso, String titulo, String descripcion, MultipartFile adjunto) {
        Estudiante autor = cursos.exigirMiembro(curso);
        titulo = validar(titulo, "Titulo", 150);
        descripcion = validar(descripcion, "Descripcion", 4000);
        ArchivoGuardado archivo = guardarArchivo(adjunto);
        try {
            return CreacionVista.de(creaciones.saveAndFlush(new Creacion(autor, curso, titulo, descripcion,
                    archivo == null ? null : archivo.nombre(), archivo == null ? null : archivo.tipo(),
                    archivo == null ? null : archivo.clave(), archivo == null ? null : archivo.bytes())));
        } catch (RuntimeException ex) {
            if (archivo != null) try { Files.deleteIfExists(archivo.ruta()); } catch (IOException ignored) { }
            throw ex;
        }
    }
    @Transactional(readOnly = true)
    public Descarga descargar(Long id) {
        Creacion c = buscar(id);
        if (c.getClaveArchivo() == null)
            throw new ApiException(HttpStatus.NOT_FOUND, "Este proyecto no tiene un archivo adjunto.");
        Path archivo = carpeta.resolve(c.getClaveArchivo()).normalize();
        if (!archivo.startsWith(carpeta))
            throw new ApiException(HttpStatus.FORBIDDEN, "Ruta no permitida");
        try {
            Resource recurso = new UrlResource(archivo.toUri());
            if (!recurso.isReadable()) throw new ApiException(HttpStatus.NOT_FOUND, "Archivo no disponible");
            return new Descarga(recurso, c.getNombreArchivo(), c.getTipoArchivo());
        } catch (java.net.MalformedURLException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Archivo no disponible");
        }
    }
    private ArchivoGuardado guardarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) return null;
        if (archivo.getSize() > MAX_BYTES)
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo no debe superar 8 MB.");
        String nombre = Objects.toString(archivo.getOriginalFilename(), "").replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        if (nombre.isEmpty() || nombre.length() > 200 || nombre.indexOf('\0') >= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nombre de archivo invalido.");
        String ext = nombre.substring(Math.max(0, nombre.lastIndexOf('.'))).toLowerCase(Locale.ROOT);
        String tipo = TIPOS.get(ext);
        if (tipo == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Archivo no permitido. Usa PDF, JPG, PNG, TXT o documentos Office.");
        Path destino = null;
        try {
            byte[] bytes = archivo.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_BYTES)
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Archivo vacio o demasiado grande.");
            comprobarFirma(ext, bytes);
            Files.createDirectories(carpeta);
            String clave = UUID.randomUUID() + ext;
            destino = carpeta.resolve(clave).normalize();
            Files.write(destino, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new ArchivoGuardado(nombre, tipo, clave, (long) bytes.length, destino);
        } catch (IOException e) {
            if (destino != null) try { Files.deleteIfExists(destino); } catch (IOException ignored) { }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo.");
        }
    }
    private static String validar(String texto, String nombre, int max) {
        if (texto == null || texto.isBlank() || texto.trim().length() > max)
            throw new ApiException(HttpStatus.BAD_REQUEST, nombre + " obligatorio (maximo " + max + " caracteres).");
        return texto.trim();
    }
    private static void comprobarFirma(String ext, byte[] b) {
        boolean valido = switch (ext) {
            case ".pdf" -> b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
            case ".png" -> b.length >= 8 && b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N'
                    && b[3] == 'G' && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10;
            case ".jpg", ".jpeg" -> b.length >= 4 && b[0] == (byte) 0xff && b[1] == (byte) 0xd8 && b[2] == (byte) 0xff;
            case ".docx", ".xlsx", ".pptx" -> b.length >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
            case ".txt" -> esUtf8(b);
            default -> false;
        };
        if (!valido) throw new ApiException(HttpStatus.BAD_REQUEST, "El contenido no corresponde al formato del archivo.");
    }
    private static boolean esUtf8(byte[] bytes) {
        try {
            String texto = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return !texto.contains("\0");
        } catch (java.nio.charset.CharacterCodingException e) { return false; }
    }
}
