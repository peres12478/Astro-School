package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.Estudiante;
import com.ejemplo.colegio.domain.Profesor;
import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.EstudianteRepository;
import com.ejemplo.colegio.security.TokenService;
import com.ejemplo.colegio.security.UsuarioActual;
import com.ejemplo.colegio.support.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class QrService {
    private final UsuarioActual actual;
    private final EstudianteRepository estudiantes;
    private final TokenService tokens;
    private final ObjectMapper mapper;

    public QrService(UsuarioActual actual, EstudianteRepository estudiantes,
            TokenService tokens, ObjectMapper mapper) {
        this.actual = actual;
        this.estudiantes = estudiantes;
        this.tokens = tokens;
        this.mapper = mapper;
    }

    /** QR compacto: codifica solo el token firmado y evita un código demasiado denso. */
    @Transactional(readOnly = true)
    public QrVista generar() {
        Estudiante e = actual.estudiante();
        var token = tokens.emitirQr(e.getId(), e.getNombre(), e.getGrupo());
        try {
            String contenido = token.token();
            byte[] png = imagenPng(contenido);
            return new QrVista(contenido, Base64.getEncoder().encodeToString(png),
                    "image/png", token.expiraEn());
        } catch (WriterException | IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear la imagen QR. Vuelve a intentarlo.");
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarPng() {
        return Base64.getDecoder().decode(generar().imagenBase64());
    }

    public byte[] imagenPng(String texto) throws WriterException, IOException {
        BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 440, 440);
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matriz, "PNG", buffer);
            return buffer.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public EstudianteEscaneado escanear(EscanearQr req) {
        Profesor profesor = actual.profesor();
        try {
            String contenido = req.contenido().trim();
            String firmadoTexto = contenido;
            String nombreAntiguo = null;
            String grupoAntiguo = null;
            // Compatible con los antiguos QR en JSON; los nuevos solo llevan el JWT firmado.
            if (contenido.startsWith("{")) {
                JsonNode json = mapper.readTree(contenido);
                if (!json.isObject() || !json.hasNonNull("token"))
                    throw new IllegalArgumentException("Formato QR incorrecto");
                firmadoTexto = json.get("token").asText();
                nombreAntiguo = json.path("nombre").asText(null);
                grupoAntiguo = json.path("grupo").asText(null);
            }
            Jwt firmado = tokens.validarQr(firmadoTexto);
            Long id = Long.parseLong(firmado.getSubject().substring("ESTUDIANTE:".length()));
            Estudiante estudiante = estudiantes.findById(id)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
            if (!estudiante.getNombre().equals(firmado.getClaimAsString("nombre"))
                    || !estudiante.getGrupo().equals(firmado.getClaimAsString("grupo"))
                    || (nombreAntiguo != null && !estudiante.getNombre().equals(nombreAntiguo))
                    || (grupoAntiguo != null && !estudiante.getGrupo().equals(grupoAntiguo)))
                throw new ApiException(HttpStatus.BAD_REQUEST, "El QR fue alterado o está desactualizado");
            return new EstudianteEscaneado(estudiante.getId(), estudiante.getNumeroLista(), estudiante.getNombre(),
                    estudiante.getGrupo(), profesor.getMateria());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pudo leer el QR. Copia el contenido completo y vuelve a intentarlo.");
        }
    }
}
