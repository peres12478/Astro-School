package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.CursoCodigo;
import com.ejemplo.colegio.domain.InscripcionCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InscripcionCursoRepository extends JpaRepository<InscripcionCurso, Long> {
    Optional<InscripcionCurso> findByEstudianteId(Long estudianteId);
    long countByCurso(CursoCodigo curso);
}
