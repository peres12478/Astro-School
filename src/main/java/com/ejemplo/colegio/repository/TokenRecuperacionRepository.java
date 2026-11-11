package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.TokenRecuperacion;
import com.ejemplo.colegio.domain.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenRecuperacion t where t.tokenHash = :tokenHash")
    Optional<TokenRecuperacion> findByTokenHashParaUso(@Param("tokenHash") String tokenHash);
    Optional<TokenRecuperacion> findTopByTipoUsuarioAndUsuarioIdOrderByCreadoEnDesc(TipoUsuario tipo, Long usuarioId);
}
