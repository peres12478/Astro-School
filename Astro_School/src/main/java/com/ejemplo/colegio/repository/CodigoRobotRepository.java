package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.CodigoRobot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CodigoRobotRepository extends JpaRepository<CodigoRobot, Long> {
    List<CodigoRobot> findByRobotIdOrderByCreadoEnDescIdDesc(Long robotId);
    Optional<CodigoRobot> findByIdAndRobotId(Long id, Long robotId);
    boolean existsByRobotIdAndNombre(Long robotId, String nombre);
}
