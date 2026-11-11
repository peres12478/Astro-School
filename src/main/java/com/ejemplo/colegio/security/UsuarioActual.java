package com.ejemplo.colegio.security;

import com.ejemplo.colegio.domain.Estudiante;
import com.ejemplo.colegio.domain.Profesor;
import com.ejemplo.colegio.repository.EstudianteRepository;
import com.ejemplo.colegio.repository.ProfesorRepository;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {
    private final ProfesorRepository profesores;
    private final EstudianteRepository estudiantes;

    public UsuarioActual(ProfesorRepository profesores, EstudianteRepository estudiantes) {
        this.profesores = profesores;
        this.estudiantes = estudiantes;
    }

    private Long idDeRol(String rol) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || !a.getAuthorities().stream()
                .anyMatch(x -> x.getAuthority().equals("ROLE_" + rol))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No autorizado para esta operacion");
        }
        String sub = a.getName(); // El autenticador JWT toma 'sub'.
        if (!sub.startsWith(rol + ":"))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token no valido");
        try { return Long.parseLong(sub.substring(rol.length() + 1)); }
        catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token no valido");
        }
    }

    private void comprobarVersion(long version) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token no valido");
        Object claim = jwtAuth.getToken().getClaim("authVersion");
        if (!(claim instanceof Number n) || n.longValue() != version)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sesion revocada; inicia sesion nuevamente");
    }

    public Profesor profesor() {
        Profesor p = profesores.findById(idDeRol("PROFESOR"))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Profesor no encontrado"));
        comprobarVersion(p.getAuthVersion());
        return p;
    }

    public Estudiante estudiante() {
        Estudiante e = estudiantes.findById(idDeRol("ESTUDIANTE"))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Estudiante no encontrado"));
        comprobarVersion(e.getAuthVersion());
        return e;
    }
}
