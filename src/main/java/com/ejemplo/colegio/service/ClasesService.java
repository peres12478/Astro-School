package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.ClaseVirtualRepository;
import com.ejemplo.colegio.security.UsuarioActual;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
public class ClasesService {
    private final ClaseVirtualRepository clases;
    private final UsuarioActual actual;
    private final ApplicationEventPublisher eventos;
    private final String jitsiBase;

    public ClasesService(ClaseVirtualRepository clases, UsuarioActual actual,
            ApplicationEventPublisher eventos, @Value("${app.jitsi.base-url}") String jitsiBase) {
        this.clases = clases;
        this.actual = actual;
        this.eventos = eventos;
        this.jitsiBase = jitsiBase.endsWith("/") ? jitsiBase : jitsiBase + "/";
    }

    @Transactional
    public ClaseVista iniciar(IniciarClase dto) {
        Profesor profesor = actual.profesor();
        // Una URL impredecible crea una sala Jitsi; no inicia una grabacion ni reserva una sala privada.
        String sala = "Colegio-" + UUID.randomUUID().toString().replace("-", "");
        ClaseVirtual c = clases.save(new ClaseVirtual(profesor,
                AuthService.normalizarGrupo(dto.grupo()), dto.titulo().trim(), jitsiBase + sala));
        eventos.publishEvent(new ClaseIniciadaEvent(c.getId()));
        return ClaseVista.de(c); // Notificaciones aun pendientes: se ejecutan despues del COMMIT.
    }

    @Transactional
    public ClaseVista finalizar(Long claseId) {
        ClaseVirtual c = clasePropia(claseId);
        c.finalizar();
        return ClaseVista.de(c);
    }

    @Transactional
    public ClaseVista registrarGrabacion(Long claseId, UrlGrabacion req) {
        ClaseVirtual c = clasePropia(claseId);
        if (c.getEstado() != EstadoClase.FINALIZADA)
            throw new ApiException(HttpStatus.CONFLICT, "Finaliza la clase antes de registrar la grabacion");
        String url = req.url().trim();
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La URL de grabacion debe ser HTTPS valida");
        }
        c.registrarGrabacion(url);
        return ClaseVista.de(c);
    }

    @Transactional(readOnly = true)
    public List<ClaseVista> clasesDeProfesor() {
        return clases.findByProfesor_IdOrderByIniciadaEnDesc(actual.profesor().getId())
                .stream().map(ClaseVista::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ClaseVista> clasesActivasDeEstudiante() {
        return clases.findByGrupoAndEstadoOrderByIniciadaEnDesc(
                actual.estudiante().getGrupo(), EstadoClase.ACTIVA)
                .stream().map(ClaseVista::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ClaseVista> grabacionesDeEstudiante() {
        return clases.findByGrupoAndUrlGrabacionIsNotNullOrderByIniciadaEnDesc(
                actual.estudiante().getGrupo())
                .stream().map(ClaseVista::de).toList();
    }

    private ClaseVirtual clasePropia(Long claseId) {
        Profesor profesor = actual.profesor();
        ClaseVirtual c = clases.findById(claseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clase no encontrada"));
        if (!c.getProfesor().getId().equals(profesor.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "La clase pertenece a otro profesor");
        return c;
    }
}
