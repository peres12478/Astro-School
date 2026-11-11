package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Trabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrabajoRepository extends JpaRepository<Trabajo, Long> {
    List<Trabajo> findByGrupoOrderByCreadaEnDesc(String grupo);
    List<Trabajo> findByProfesor_IdOrderByCreadaEnDesc(Long profesorId);
    boolean existsByProfesor_IdAndTitulo(Long profesorId, String titulo);
}
