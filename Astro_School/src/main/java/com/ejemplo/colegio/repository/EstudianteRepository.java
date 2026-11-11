package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estudiante e where e.id = :id")
    Optional<Estudiante> bloquearPorId(@Param("id") Long id);

    Optional<Estudiante> findByCorreoIgnoreCase(String correo);
    Optional<Estudiante> findByIdentificacion(String identificacion);
    Optional<Estudiante> findByNumeroLista(Integer numeroLista);
    boolean existsByNumeroLista(Integer numeroLista);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByIdentificacion(String identificacion);
    List<Estudiante> findByGrupoOrderByNombreAsc(String grupo);
}
