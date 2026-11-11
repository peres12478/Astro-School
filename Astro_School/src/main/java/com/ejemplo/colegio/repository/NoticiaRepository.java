package com.ejemplo.colegio.repository;

import com.ejemplo.colegio.domain.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {
    List<Noticia> findTop50ByOrderByCreadaEnDescIdDesc();
    boolean existsByTituloAndAutor(String titulo, String autor);
}
