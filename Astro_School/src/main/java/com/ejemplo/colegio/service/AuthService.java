package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.*;
import com.ejemplo.colegio.security.TokenService;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class AuthService {
    private final ProfesorRepository profesores;
    private final EstudianteRepository estudiantes;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final String claveDocente;
    private final boolean demoEnabled;

    public AuthService(ProfesorRepository profesores, EstudianteRepository estudiantes,
            PasswordEncoder passwords, TokenService tokens,
            @Value("${app.registro-docente-clave}") String claveDocente,
            @Value("${app.demo.enabled:false}") boolean demoEnabled) {
        this.profesores = profesores;
        this.estudiantes = estudiantes;
        this.passwords = passwords;
        this.tokens = tokens;
        this.claveDocente = claveDocente;
        this.demoEnabled = demoEnabled;
    }

    @Transactional
    public AuthRespuesta registrarProfesor(RegistroProfesor dto, String clave) {
        if (clave == null || !MessageDigest.isEqual(clave.getBytes(StandardCharsets.UTF_8),
                claveDocente.getBytes(StandardCharsets.UTF_8)))
            throw new ApiException(HttpStatus.FORBIDDEN, "Clave de registro docente incorrecta");
        String correo = normalizarCorreo(dto.correo());
        validarNoRegistrado(correo, dto.identificacion().trim());
        Profesor p = profesores.save(new Profesor(dto.identificacion().trim(), dto.nombre().trim(),
                dto.materia().trim(), correo, passwords.encode(dto.password())));
        return emitirRespuesta(TipoUsuario.PROFESOR, p.getId(), p.getNombre(), p.getAuthVersion(),
                "Profesor registrado correctamente. Sesión iniciada.");
    }

    @Transactional
    public AuthRespuesta registrarEstudiante(RegistroEstudiante dto) {
        String correo = normalizarCorreo(dto.correo());
        validarNoRegistrado(correo, dto.identificacion().trim());
        if (estudiantes.existsByNumeroLista(dto.numeroLista()))
            throw new ApiException(HttpStatus.CONFLICT, "Ese número de lista ya pertenece a otro estudiante. Elige otro.");
        Estudiante e = estudiantes.save(new Estudiante(dto.identificacion().trim(), dto.nombre().trim(),
                normalizarGrupo(dto.grupo()), correo, passwords.encode(dto.password()), dto.numeroLista()));
        return emitirRespuesta(TipoUsuario.ESTUDIANTE, e.getId(), e.getNombre(), e.getAuthVersion(),
                "Estudiante registrado correctamente. Sesión iniciada.");
    }

    /** Permite entrar con correo o documento, conservando el campo JSON antiguo correo. */
    @Transactional(readOnly = true)
    public AuthRespuesta login(Login dto) {
        String usuario = dto.usuario().trim();
        // Desactivar el modo demo tambien bloquea el acceso normal a cuentas
        // ficticias previamente guardadas; desactivar el endpoint no bastaba.
        if (!demoEnabled && DemoCuentas.CUENTAS.stream().anyMatch(c ->
                c.identificacion().equals(usuario) || c.correo().equalsIgnoreCase(usuario)))
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Las cuentas de demostracion estan desactivadas en este servidor");
        if (usuario.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe el correo o número de identificación");
        boolean esCorreo = usuario.contains("@");
        var profesor = esCorreo ? profesores.findByCorreoIgnoreCase(normalizarCorreo(usuario))
                : profesores.findByIdentificacion(usuario);
        if (profesor.isPresent()) {
            Profesor p = profesor.get();
            if (passwords.matches(dto.password(), p.getPasswordHash()))
                return emitirRespuesta(TipoUsuario.PROFESOR, p.getId(), p.getNombre(), p.getAuthVersion(),
                        "Sesión iniciada correctamente como profesor.");
        } else {
            var estudiante = esCorreo ? estudiantes.findByCorreoIgnoreCase(normalizarCorreo(usuario))
                    : estudiantes.findByIdentificacion(usuario);
            if (estudiante.isPresent()) {
                Estudiante e = estudiante.get();
                if (passwords.matches(dto.password(), e.getPasswordHash())) {
                    if (dto.numeroLista() == null)
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Para iniciar sesión como estudiante, escribe también tu número de lista.");
                    if (!dto.numeroLista().equals(e.getNumeroLista()))
                        throw new ApiException(HttpStatus.UNAUTHORIZED, "El número de lista no corresponde a este estudiante.");
                    return emitirRespuesta(TipoUsuario.ESTUDIANTE, e.getId(), e.getNombre(), e.getAuthVersion(),
                            "Sesión iniciada correctamente como estudiante. Número de lista " + e.getNumeroLista() + ".");
                }
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED,
                "Correo o documento y contraseña no coinciden. Revisa tus datos.");
    }

    /** Entrada rápida EXCLUSIVA para cuentas ficticias del perfil H2, sin credenciales manuales. */
    @Transactional(readOnly = true)
    public AuthRespuesta loginDemo(String identificacion) {
        DemoCuentas.Cuenta cuenta = DemoCuentas.buscar(identificacion)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Cuenta de demostración no válida"));
        if (cuenta.rol() == TipoUsuario.PROFESOR) {
            Profesor p = profesores.findByIdentificacion(cuenta.identificacion())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cuenta de prueba no encontrada"));
            if (!p.getCorreo().equalsIgnoreCase(cuenta.correo()) ||
                    !passwords.matches(DemoCuentas.CLAVE, p.getPasswordHash()))
                throw new ApiException(HttpStatus.FORBIDDEN, "Esta cuenta de prueba fue modificada. Usa el acceso habitual.");
            return emitirRespuesta(TipoUsuario.PROFESOR, p.getId(), p.getNombre(), p.getAuthVersion(),
                    "Modo demostración: sesión iniciada como profesor.");
        }
        Estudiante e = estudiantes.findByIdentificacion(cuenta.identificacion())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cuenta de prueba no encontrada"));
        if (!e.getCorreo().equalsIgnoreCase(cuenta.correo()) ||
                !passwords.matches(DemoCuentas.CLAVE, e.getPasswordHash()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta cuenta de prueba fue modificada. Usa el acceso habitual.");
        return emitirRespuesta(TipoUsuario.ESTUDIANTE, e.getId(), e.getNombre(), e.getAuthVersion(),
                "Modo demostración: sesión iniciada como estudiante.");
    }

    private void validarNoRegistrado(String correo, String identificacion) {
        if (profesores.existsByCorreoIgnoreCase(correo) || estudiantes.existsByCorreoIgnoreCase(correo))
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        if (profesores.existsByIdentificacion(identificacion) || estudiantes.existsByIdentificacion(identificacion))
            throw new ApiException(HttpStatus.CONFLICT, "La identificacion ya esta registrada");
    }

    private AuthRespuesta emitirRespuesta(TipoUsuario rol, Long id, String nombre, long authVersion,
                                         String mensaje) {
        var emitido = tokens.emitirAcceso(rol, id, authVersion);
        return new AuthRespuesta(emitido.token(), "Bearer", rol, id, nombre, emitido.expiraEn(), mensaje);
    }

    public static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizarGrupo(String grupo) {
        return grupo.trim().toUpperCase(Locale.ROOT);
    }
}
