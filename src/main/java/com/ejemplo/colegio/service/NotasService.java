package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.dto.Dtos.*;
import com.ejemplo.colegio.repository.*;
import com.ejemplo.colegio.security.UsuarioActual;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotasService {
    private final NotaRepository notas;
    private final EstudianteRepository estudiantes;
    private final UsuarioActual actual;
    private final QrService qr;

    public NotasService(NotaRepository notas, EstudianteRepository estudiantes, UsuarioActual actual, QrService qr) {
        this.notas = notas;
        this.estudiantes = estudiantes;
        this.actual = actual;
        this.qr = qr;
    }

    @Transactional
    public NotaVista asignar(CrearNota dto) {
        Profesor profesor = actual.profesor();
        Estudiante estudiante = estudiantes.findById(dto.estudianteId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
        Nota nota = new Nota(estudiante, profesor, dto.valor(), dto.observacion());
        return NotaVista.de(notas.save(nota));
    }

    @Transactional
    public NotaVista asignarPorQr(CalificarQr dto) {
        // La identidad viene de la firma QR verificada, no de un ID arbitrario del cuerpo.
        EstudianteEscaneado estudiante = qr.escanear(new EscanearQr(dto.contenido()));
        return asignar(new CrearNota(estudiante.estudianteId(), dto.valor(), dto.observacion()));
    }

    @Transactional
    public NotaVista modificar(Long id, ActualizarNota dto) {
        Profesor profesor = actual.profesor();
        Nota nota = notas.findByIdAndProfesor_Id(id, profesor.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nota no encontrada para este profesor"));
        nota.actualizar(dto.valor(), dto.observacion());
        return NotaVista.de(nota);
    }

    @Transactional(readOnly = true)
    public List<NotaVista> notasDelEstudiante() {
        Estudiante estudiante = actual.estudiante();
        return notas.findByEstudiante_IdOrderByCreadaEnDesc(estudiante.getId())
                .stream().map(NotaVista::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ResumenMateria> resumenPorMateria() {
        Map<String, List<NotaVista>> grupos = new LinkedHashMap<>();
        for (NotaVista nota : notasDelEstudiante()) {
            grupos.computeIfAbsent(nota.materia(), k -> new java.util.ArrayList<>()).add(nota);
        }
        return grupos.entrySet().stream().map(entry -> {
            List<NotaVista> lista = entry.getValue();
            BigDecimal suma = lista.stream().map(NotaVista::valor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal media = suma.divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_UP);
            return new ResumenMateria(entry.getKey(), media, lista.size(),
                    media.compareTo(new BigDecimal("3.00")) >= 0);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<NotaVista> notasDelProfesorPara(Long estudianteId) {
        Profesor profesor = actual.profesor();
        if (!estudiantes.existsById(estudianteId))
            throw new ApiException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        return notas.findByEstudiante_IdAndProfesor_IdOrderByCreadaEnDesc(estudianteId, profesor.getId())
                .stream().map(NotaVista::de).toList();
    }
}
