package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByEstudiante_IdOrderByCreadaEnDesc(Long estudianteId);
    List<Nota> findByEstudiante_IdAndProfesor_IdOrderByCreadaEnDesc(Long estudianteId, Long profesorId);
    Optional<Nota> findByIdAndProfesor_Id(Long id, Long profesorId);
    boolean existsByEstudiante_IdAndProfesor_IdAndObservacion(Long estudianteId, Long profesorId, String observacion);
}
