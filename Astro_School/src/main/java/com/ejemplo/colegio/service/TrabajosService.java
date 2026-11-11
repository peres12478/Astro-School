package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.TrabajoRepository;
import com.ejemplo.colegio.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TrabajosService {
    private final TrabajoRepository trabajos;
    private final UsuarioActual actual;

    public TrabajosService(TrabajoRepository trabajos, UsuarioActual actual) {
        this.trabajos = trabajos;
        this.actual = actual;
    }

    @Transactional
    public TrabajoVista crear(CrearTrabajo dto) {
        Profesor profesor = actual.profesor();
        Trabajo t = new Trabajo(profesor, AuthService.normalizarGrupo(dto.grupo()),
                dto.titulo().trim(), dto.descripcion().trim(), dto.fechaEntrega());
        return TrabajoVista.de(trabajos.save(t));
    }

    @Transactional(readOnly = true)
    public List<TrabajoVista> misTrabajosComoProfesor() {
        return trabajos.findByProfesor_IdOrderByCreadaEnDesc(actual.profesor().getId())
                .stream().map(TrabajoVista::de).toList();
    }

    @Transactional(readOnly = true)
    public List<TrabajoVista> misTrabajosComoEstudiante() {
        return trabajos.findByGrupoOrderByCreadaEnDesc(actual.estudiante().getGrupo())
                .stream().map(TrabajoVista::de).toList();
    }
}
