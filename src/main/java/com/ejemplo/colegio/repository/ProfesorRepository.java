package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    Optional<Profesor> findByCorreoIgnoreCase(String correo);
    Optional<Profesor> findByIdentificacion(String identificacion);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByIdentificacion(String identificacion);
}
