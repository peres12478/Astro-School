package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.CursoCodigo;
import com.ejemplo.colegio.domain.Estudiante;
import com.ejemplo.colegio.domain.InscripcionCurso;
import com.ejemplo.colegio.repository.EstudianteRepository;
import com.ejemplo.colegio.repository.InscripcionCursoRepository;
import com.ejemplo.colegio.security.UsuarioActual;
import com.ejemplo.colegio.support.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Service
public class CursosService {
    public record CursoVista(CursoCodigo codigo, String nombre, String descripcion, long inscritos) { }
    public record InscripcionVista(Long id, CursoCodigo curso, String nombre, java.time.Instant inscritoEn) {
        static InscripcionVista de(InscripcionCurso inscripcion) {
            return new InscripcionVista(inscripcion.getId(), inscripcion.getCurso(),
                    inscripcion.getCurso().getNombre(), inscripcion.getInscritoEn());
        }
    }

    private final UsuarioActual actual;
    private final EstudianteRepository estudiantes;
    private final InscripcionCursoRepository inscripciones;

    public CursosService(UsuarioActual actual, EstudianteRepository estudiantes,
                         InscripcionCursoRepository inscripciones) {
        this.actual = actual;
        this.estudiantes = estudiantes;
        this.inscripciones = inscripciones;
    }

    /** Comprueba el curso real en la base de datos: nunca confia en el curso enviado por el navegador. */
    @Transactional(readOnly = true)
    public Estudiante exigirMiembro(CursoCodigo esperado) {
        Estudiante alumno = actual.estudiante();
        InscripcionCurso inscripcion = inscripciones.findByEstudianteId(alumno.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Debes inscribirte en " + esperado.getNombre() + " para usar este espacio."));
        if (inscripcion.getCurso() != esperado)
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tu curso actual es " + inscripcion.getCurso().getNombre() + ". No puedes publicar en otro grupo.");
        return alumno;
    }

    @Transactional(readOnly = true)
    public List<CursoVista> catalogo() {
        return Arrays.stream(CursoCodigo.values())
                .map(c -> new CursoVista(c, c.getNombre(), c.getDescripcion(), inscripciones.countByCurso(c)))
                .toList();
    }

    @Transactional(readOnly = true)
    public InscripcionVista miInscripcion() {
        return inscripciones.findByEstudianteId(actual.estudiante().getId())
                .map(InscripcionVista::de).orElse(null);
    }

    @Transactional
    public InscripcionVista inscribirme(CursoCodigo curso) {
        // El bloqueo por estudiante y la restriccion UNIQUE en la BD evitan inscripciones simultaneas.
        Long estudianteId = actual.estudiante().getId();
        Estudiante e = estudiantes.bloquearPorId(estudianteId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Estudiante no encontrado"));
        if (inscripciones.findByEstudianteId(estudianteId).isPresent())
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya estas inscrito en un curso. Primero cancela tu inscripcion para elegir otro.");
        return InscripcionVista.de(inscripciones.saveAndFlush(new InscripcionCurso(e, curso)));
    }

    @Transactional
    public void cancelarMiInscripcion() {
        Long estudianteId = actual.estudiante().getId();
        estudiantes.bloquearPorId(estudianteId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Estudiante no encontrado"));
        InscripcionCurso inscripcion = inscripciones.findByEstudianteId(estudianteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes una inscripcion activa"));
        inscripciones.delete(inscripcion);
    }
}
