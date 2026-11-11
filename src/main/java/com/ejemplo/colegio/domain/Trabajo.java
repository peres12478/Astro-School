package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trabajos", indexes = @Index(name = "idx_trabajo_grupo", columnList = "grupo"))
public class Trabajo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @Column(nullable = false, length = 30)
    private String grupo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String descripcion;

    private Instant fechaEntrega;

    @Column(nullable = false, updatable = false)
    private Instant creadaEn;

    protected Trabajo() { }

    public Trabajo(Profesor profesor, String grupo, String titulo, String descripcion, Instant fechaEntrega) {
        this.profesor = profesor;
        this.grupo = grupo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaEntrega = fechaEntrega;
    }

    @PrePersist
    void antesDeCrear() { creadaEn = Instant.now(); }

    public Long getId() { return id; }
    public Profesor getProfesor() { return profesor; }
    public String getGrupo() { return grupo; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Instant getFechaEntrega() { return fechaEntrega; }
    public Instant getCreadaEn() { return creadaEn; }
}
