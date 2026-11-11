package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Creacion;
import com.ejemplo.colegio.domain.CursoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreacionRepository extends JpaRepository<Creacion, Long> {
    List<Creacion> findTop100ByOrderByCreadaEnDescIdDesc();
    List<Creacion> findTop100ByCursoOrderByCreadaEnDescIdDesc(CursoCodigo curso);
    List<Creacion> findByEstudianteIdAndCursoOrderByCreadaEnDescIdDesc(Long estudianteId, CursoCodigo curso);
    boolean existsByTituloAndEstudiante_Id(String titulo, Long estudianteId);
}
