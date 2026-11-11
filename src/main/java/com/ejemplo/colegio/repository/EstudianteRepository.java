package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByCorreoIgnoreCase(String correo);
    Optional<Estudiante> findByIdentificacion(String identificacion);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByIdentificacion(String identificacion);
    List<Estudiante> findByGrupoOrderByNombreAsc(String grupo);
}
