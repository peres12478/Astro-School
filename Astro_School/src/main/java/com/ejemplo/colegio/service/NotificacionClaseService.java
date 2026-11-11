package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.ClaseVirtual;
import com.ejemplo.colegio.domain.Estudiante;
import com.ejemplo.colegio.repository.ClaseVirtualRepository;
import com.ejemplo.colegio.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificacionClaseService {
    private final ClaseVirtualRepository clases;
    private final EstudianteRepository estudiantes;

    /** Proyeccion inmutable: no se conserva ninguna entidad JPA en el hilo SMTP. */
    public record PlanEnvio(Long claseId, String grupo, String titulo,
                            String profesor, String materia, String enlace, List<String> correos) { }

    public NotificacionClaseService(ClaseVirtualRepository clases,
            EstudianteRepository estudiantes) {
        this.clases = clases;
        this.estudiantes = estudiantes;
    }

    @Transactional(readOnly = true)
    public PlanEnvio planificar(Long id) {
        ClaseVirtual clase = clases.findById(id).orElse(null);
        if (clase == null) return null;
        List<String> correos = estudiantes.findByGrupoOrderByNombreAsc(clase.getGrupo())
                .stream().map(Estudiante::getCorreo).toList();
        return new PlanEnvio(id, clase.getGrupo(), clase.getTitulo(),
                clase.getProfesor().getNombre(), clase.getProfesor().getMateria(),
                clase.getEnlace(), correos);
    }

    @Transactional
    public void registrarResultado(Long id, int enviados, int fallidos, int destinatarios) {
        clases.findById(id).ifPresent(clase ->
                clase.actualizarNotificacion(enviados, fallidos, destinatarios));
    }
}
