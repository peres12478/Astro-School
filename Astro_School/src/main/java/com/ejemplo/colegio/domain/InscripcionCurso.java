package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inscripciones_cursos", uniqueConstraints =
        @UniqueConstraint(name = "uq_inscripcion_estudiante", columnNames = "estudiante_id"))
public class InscripcionCurso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, unique = true)
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CursoCodigo curso;

    @Column(nullable = false, updatable = false)
    private Instant inscritoEn;

    protected InscripcionCurso() { }

    public InscripcionCurso(Estudiante estudiante, CursoCodigo curso) {
        this.estudiante = estudiante;
        this.curso = curso;
        this.inscritoEn = Instant.now();
    }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public CursoCodigo getCurso() { return curso; }
    public Instant getInscritoEn() { return inscritoEn; }
}
