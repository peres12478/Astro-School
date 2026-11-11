package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.*;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RecuperacionService {
    private final TokenRecuperacionRepository tokens;
    private final ProfesorRepository profesores;
    private final EstudianteRepository estudiantes;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventos;
    private final SecureRandom random = new SecureRandom();

    public RecuperacionService(TokenRecuperacionRepository tokens, ProfesorRepository profesores,
            EstudianteRepository estudiantes, PasswordEncoder encoder, ApplicationEventPublisher eventos) {
        this.tokens = tokens;
        this.profesores = profesores;
        this.estudiantes = estudiantes;
        this.encoder = encoder;
        this.eventos = eventos;
    }

    @Transactional
    public Mensaje solicitar(RecuperarPassword req) {
        String correo = AuthService.normalizarCorreo(req.correo());
        var profesor = profesores.findByCorreoIgnoreCase(correo);
        if (profesor.isPresent()) {
            crearYNotificar(TipoUsuario.PROFESOR, profesor.get().getId(), correo);
        } else {
            estudiantes.findByCorreoIgnoreCase(correo).ifPresent(e ->
                    crearYNotificar(TipoUsuario.ESTUDIANTE, e.getId(), correo));
        }
        // Mismo resultado aunque el correo no exista: evita enumeracion de cuentas.
        return new Mensaje("Si el correo esta registrado, se enviaran instrucciones de recuperacion.");
    }

    private void crearYNotificar(TipoUsuario tipo, Long usuarioId, String correo) {
        Instant ahora = Instant.now();
        boolean muyReciente = tokens.findTopByTipoUsuarioAndUsuarioIdOrderByCreadoEnDesc(tipo, usuarioId)
                .map(t -> t.getCreadoEn().isAfter(ahora.minus(60, ChronoUnit.SECONDS)))
                .orElse(false);
        if (muyReciente) return;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new TokenRecuperacion(tipo, usuarioId, hash(token),
                ahora.plus(15, ChronoUnit.MINUTES)));
        eventos.publishEvent(new RecuperacionEvent(correo, token));
    }

    @Transactional
    public Mensaje restablecer(RestablecerPassword req) {
        String tokenHash = hash(req.token());
        TokenRecuperacion token = tokens.findByTokenHashParaUso(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token invalido o vencido"));
        if (!token.esValido(Instant.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token invalido o vencido");
        var ultimo = tokens.findTopByTipoUsuarioAndUsuarioIdOrderByCreadoEnDesc(
                token.getTipoUsuario(), token.getUsuarioId());
        if (ultimo.isEmpty() || !ultimo.get().getId().equals(token.getId()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Existe un token de recuperacion mas reciente");

        String hashNuevo = encoder.encode(req.nuevaPassword());
        if (token.getTipoUsuario() == TipoUsuario.PROFESOR) {
            Profesor p = profesores.findById(token.getUsuarioId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token invalido"));
            p.cambiarPasswordHash(hashNuevo);
        } else {
            Estudiante e = estudiantes.findById(token.getUsuarioId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token invalido"));
            e.cambiarPasswordHash(hashNuevo);
        }
        token.marcarUtilizado(Instant.now());
        return new Mensaje("Contrasena actualizada. Inicia sesion nuevamente.");
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
