package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.repository.*;
import com.ejemplo.colegio.support.ApiException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class RobotsService {
    public record NuevoRobot(@NotBlank @Size(max = 100) String nombre,
                             @NotBlank @Size(max = 100) String plataforma,
                             @NotBlank @Size(max = 1500) String descripcion) { }
    public record NuevoCodigo(@NotBlank @Size(max = 120) String nombre,
                              @NotBlank String lenguaje,
                              @NotBlank @Size(max = 100000) String contenido) { }
    public record RobotVista(Long id, String nombre, String plataforma, String descripcion, Instant creadoEn) {
        static RobotVista de(Robot r) { return new RobotVista(r.getId(), r.getNombre(), r.getPlataforma(), r.getDescripcion(), r.getCreadoEn()); }
    }
    public record CodigoVista(Long id, Long robotId, String nombre, String lenguaje, String contenido,
                             Instant creadoEn, String urlDescarga) {
        static CodigoVista de(CodigoRobot c) {
            return new CodigoVista(c.getId(), c.getRobot().getId(), c.getNombre(), c.getLenguaje(),
                    c.getContenido(), c.getCreadoEn(), "/api/robotica/robots/" + c.getRobot().getId()
                            + "/codigos/" + c.getId() + "/descargar");
        }
    }
    public record CodigoDescarga(String nombre, byte[] bytes) { }

    private static final Set<String> LENGUAJES = Set.of("ARDUINO", "PYTHON", "C_CPP", "JAVASCRIPT", "TEXTO");
    private static final Map<String, String> EXTENSIONES = Map.of(
            ".ino", "ARDUINO", ".py", "PYTHON", ".c", "C_CPP", ".cpp", "C_CPP",
            ".h", "C_CPP", ".hpp", "C_CPP", ".js", "JAVASCRIPT", ".txt", "TEXTO");
    private final CursosService cursos;
    private final RobotRepository robots;
    private final CodigoRobotRepository codigos;

    public RobotsService(CursosService cursos, RobotRepository robots, CodigoRobotRepository codigos) {
        this.cursos = cursos; this.robots = robots; this.codigos = codigos;
    }
    private Estudiante miembro() { return cursos.exigirMiembro(CursoCodigo.ROBOTICA); }
    private Robot robotPropio(Long id) {
        return robots.findByIdAndEstudianteId(id, miembro().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Robot no encontrado en tu cuenta"));
    }
    @Transactional(readOnly = true)
    public List<RobotVista> misRobots() {
        return robots.findByEstudianteIdOrderByCreadoEnDescIdDesc(miembro().getId()).stream().map(RobotVista::de).toList();
    }
    @Transactional
    public RobotVista registrar(NuevoRobot datos) {
        Estudiante estudiante = miembro();
        return RobotVista.de(robots.save(new Robot(estudiante, limpiar(datos.nombre(), 100),
                limpiar(datos.plataforma(), 100), limpiar(datos.descripcion(), 1500))));
    }
    @Transactional(readOnly = true)
    public List<CodigoVista> listarCodigos(Long robotId) {
        robotPropio(robotId);
        return codigos.findByRobotIdOrderByCreadoEnDescIdDesc(robotId).stream().map(CodigoVista::de).toList();
    }
    @Transactional
    public CodigoVista guardarCodigo(Long robotId, NuevoCodigo datos) {
        Robot robot = robotPropio(robotId);
        String lenguaje = limpiar(datos.lenguaje(), 20).toUpperCase(Locale.ROOT);
        if (!LENGUAJES.contains(lenguaje))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lenguaje desconocido. Usa ARDUINO, PYTHON, C_CPP, JAVASCRIPT o TEXTO.");
        String contenido = validarCodigo(datos.contenido());
        return CodigoVista.de(codigos.save(new CodigoRobot(robot, limpiar(datos.nombre(), 120), lenguaje, contenido)));
    }
    @Transactional
    public CodigoVista subirCodigo(Long robotId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona un archivo de codigo.");
        if (archivo.getSize() > 100000) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "El codigo no puede superar 100 KB.");
        String nombre = Objects.toString(archivo.getOriginalFilename(), "").replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        if (nombre.isBlank() || nombre.length() > 120 || nombre.indexOf('\0') >= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nombre de archivo invalido (maximo 120 caracteres).");
        String ext = nombre.substring(Math.max(0, nombre.lastIndexOf('.'))).toLowerCase(Locale.ROOT);
        String lenguaje = EXTENSIONES.get(ext);
        if (lenguaje == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Formato no admitido. Usa .ino, .py, .cpp, .c, .h, .hpp, .js o .txt.");
        try {
            byte[] bytes = archivo.getBytes();
            if (bytes.length > 100000) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "El codigo no puede superar 100 KB.");
            String contenido = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return guardarCodigo(robotId, new NuevoCodigo(nombre, lenguaje, contenido));
        } catch (java.nio.charset.CharacterCodingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo debe contener texto valido en UTF-8.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo.");
        }
    }
    @Transactional(readOnly = true)
    public CodigoDescarga descargar(Long robotId, Long codigoId) {
        robotPropio(robotId);
        CodigoRobot codigo = codigos.findByIdAndRobotId(codigoId, robotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Codigo no encontrado"));
        String extension = switch (codigo.getLenguaje()) {
            case "ARDUINO" -> ".ino"; case "PYTHON" -> ".py"; case "C_CPP" -> ".cpp";
            case "JAVASCRIPT" -> ".js"; default -> ".txt";
        };
        String nombre = codigo.getNombre();
        if (!nombre.toLowerCase(Locale.ROOT).endsWith(extension)) nombre += extension;
        return new CodigoDescarga(nombre, codigo.getContenido().getBytes(StandardCharsets.UTF_8));
    }
    private static String limpiar(String valor, int max) {
        if (valor == null || valor.isBlank() || valor.trim().length() > max)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Campo obligatorio o demasiado largo (maximo " + max + ").");
        return valor.trim();
    }
    private static String validarCodigo(String contenido) {
        if (contenido == null || contenido.isBlank() || contenido.length() > 100000 || contenido.indexOf('\0') >= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe entre 1 y 100000 caracteres de codigo, sin NUL.");
        return contenido;
    }
}
