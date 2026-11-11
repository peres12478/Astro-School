package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {
    List<Robot> findByEstudianteIdOrderByCreadoEnDescIdDesc(Long estudianteId);
    Optional<Robot> findByIdAndEstudianteId(Long id, Long estudianteId);
    Optional<Robot> findByEstudianteIdAndNombre(Long estudianteId, String nombre);
}
