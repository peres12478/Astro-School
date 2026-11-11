package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.ClaseVirtual;
import com.ejemplo.colegio.domain.EstadoClase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaseVirtualRepository extends JpaRepository<ClaseVirtual, Long> {
    List<ClaseVirtual> findByGrupoAndEstadoOrderByIniciadaEnDesc(String grupo, EstadoClase estado);
    List<ClaseVirtual> findByGrupoAndUrlGrabacionIsNotNullOrderByIniciadaEnDesc(String grupo);
    List<ClaseVirtual> findByProfesor_IdOrderByIniciadaEnDesc(Long profesorId);
}
