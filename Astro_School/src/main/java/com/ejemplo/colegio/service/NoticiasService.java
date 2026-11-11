package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.Noticia;
import com.ejemplo.colegio.repository.NoticiaRepository;
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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class NoticiasService {
    public record NoticiaVista(Long id, String titulo, String descripcion, String autor,
                               java.time.Instant creadaEn, String nombreArchivo, String tipoArchivo,
                               Long bytesArchivo, String urlArchivo) {
        public static NoticiaVista de(Noticia n) {
            return new NoticiaVista(n.getId(), n.getTitulo(), n.getDescripcion(), n.getAutor(),
                    n.getCreadaEn(), n.getNombreArchivo(), n.getTipoArchivo(), n.getBytesArchivo(),
                    n.getClaveArchivo() == null ? null : "/api/noticias/" + n.getId() + "/archivo");
        }
    }
    public record Descarga(Resource recurso, String nombre, String tipo) { }

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Map<String, String> TIPOS = Map.of(
            ".pdf", "application/pdf", ".png", "image/png", ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg", ".txt", "text/plain; charset=utf-8",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final NoticiaRepository noticias;
    private final Path carpeta;
    private final boolean publico;

    public NoticiasService(NoticiaRepository noticias,
            @Value("${app.noticias.directorio:./data/noticias}") String directorio,
            @Value("${app.noticias.publicacion-publica:true}") boolean publico) {
        this.noticias = noticias;
        this.carpeta = Path.of(directorio).toAbsolutePath().normalize();
        this.publico = publico;
    }

    @Transactional(readOnly = true)
    public List<NoticiaVista> listar() {
        return noticias.findTop50ByOrderByCreadaEnDescIdDesc().stream().map(NoticiaVista::de).toList();
    }

    @Transactional(readOnly = true)
    public NoticiaVista ver(Long id) { return NoticiaVista.de(buscar(id)); }

    private Noticia buscar(Long id) {
        return noticias.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Noticia no encontrada"));
    }

    @Transactional
    public NoticiaVista publicar(String titulo, String descripcion, String autor, MultipartFile archivo) {
        if (!publico) throw new ApiException(HttpStatus.FORBIDDEN,
                "La publicacion publica esta desactivada por el administrador.");
        titulo = validar(titulo, "El titulo", 160);
        descripcion = validar(descripcion, "La descripcion", 5000);
        autor = validar(autor, "El nombre del autor", 100);
        String nombre = null, tipo = null, clave = null;
        Long bytes = null;
        Path destino = null;
        if (archivo != null && !archivo.isEmpty()) {
            if (archivo.getSize() > MAX_BYTES)
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera los 8 MB permitidos.");
            nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
            // Se muestra solo el nombre, nunca la ruta enviada por el cliente.
            nombre = nombre.replace('\\', '/');
            nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
            if (nombre.isEmpty() || nombre.length() > 200 || nombre.indexOf('\0') >= 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Nombre de archivo invalido (maximo 200 caracteres).");
            String ext = nombre.substring(Math.max(0, nombre.lastIndexOf('.'))).toLowerCase(Locale.ROOT);
            tipo = TIPOS.get(ext);
            if (tipo == null) throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Formato no permitido. Solo PDF, PNG, JPG, JPEG, TXT, DOCX, XLSX o PPTX.");
            try {
                byte[] contenido = archivo.getBytes();
                if (contenido.length > MAX_BYTES || contenido.length == 0)
                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Archivo vacio o demasiado grande.");
                validarFirma(ext, contenido);
                Files.createDirectories(carpeta);
                clave = UUID.randomUUID() + ext;
                destino = carpeta.resolve(clave).normalize();
                // CREATE_NEW evita sobrescrituras; el nombre fisico nunca proviene del usuario.
                Files.write(destino, contenido, java.nio.file.StandardOpenOption.CREATE_NEW,
                        java.nio.file.StandardOpenOption.WRITE);
                bytes = (long) contenido.length;
            } catch (IOException ex) {
                if (destino != null) try { Files.deleteIfExists(destino); } catch (IOException ignored) { }
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible guardar el adjunto.");
            }
        }
        try {
            return NoticiaVista.de(noticias.saveAndFlush(new Noticia(titulo, descripcion, autor,
                    nombre, tipo, clave, bytes)));
        } catch (RuntimeException ex) {
            if (destino != null) try { Files.deleteIfExists(destino); } catch (IOException ignored) { }
            throw ex;
        }
    }

    private static String validar(String valor, String campo, int max) {
        if (valor == null || valor.isBlank() || valor.trim().length() > max)
            throw new ApiException(HttpStatus.BAD_REQUEST, campo + " es obligatorio (maximo " + max + " caracteres).");
        return valor.trim();
    }

    private static void validarFirma(String ext, byte[] b) {
        boolean valido = switch (ext) {
            case ".pdf" -> b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
            case ".png" -> b.length >= 8 && b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N'
                    && b[3] == 'G' && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10;
            case ".jpg", ".jpeg" -> b.length >= 4 && b[0] == (byte) 0xff && b[1] == (byte) 0xd8
                    && b[2] == (byte) 0xff;
            case ".docx", ".xlsx", ".pptx" -> b.length >= 4 && b[0] == 'P' && b[1] == 'K'
                    && b[2] == 3 && b[3] == 4;
            case ".txt" -> esTextoUtf8(b);
            default -> false;
        };
        if (!valido) throw new ApiException(HttpStatus.BAD_REQUEST,
                "El contenido del archivo no corresponde al formato indicado.");
    }

    private static boolean esTextoUtf8(byte[] b) {
        try {
            String texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b)).toString();
            return texto.indexOf('\0') < 0;
        } catch (CharacterCodingException ex) { return false; }
    }

    @Transactional(readOnly = true)
    public Descarga descargar(Long id) {
        Noticia n = buscar(id);
        if (n.getClaveArchivo() == null) throw new ApiException(HttpStatus.NOT_FOUND,
                "Esta noticia no tiene un archivo adjunto.");
        Path archivo = carpeta.resolve(n.getClaveArchivo()).normalize();
        if (!archivo.startsWith(carpeta))
            throw new ApiException(HttpStatus.FORBIDDEN, "Ruta de archivo no permitida.");
        try {
            Resource resource = new UrlResource(archivo.toUri());
            if (!resource.isReadable()) throw new ApiException(HttpStatus.NOT_FOUND, "Archivo no disponible.");
            return new Descarga(resource, n.getNombreArchivo(), n.getTipoArchivo());
        } catch (java.net.MalformedURLException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Archivo no disponible.");
        }
    }
}
