package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "notas", indexes = {
        @Index(name = "idx_nota_estudiante", columnList = "estudiante_id"),
        @Index(name = "idx_nota_profesor", columnList = "profesor_id")
})
public class Nota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    // Se guarda una instantanea de la materia al momento de registrar la nota.
    @Column(nullable = false, length = 100)
    private String materia;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal valor;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false, updatable = false)
    private Instant creadaEn;

    protected Nota() { }

    public Nota(Estudiante estudiante, Profesor profesor, BigDecimal valor, String observacion) {
        this.estudiante = estudiante;
        this.profesor = profesor;
        this.materia = profesor.getMateria();
        this.valor = valor;
        this.observacion = observacion;
    }

    @PrePersist
    void antesDeCrear() { creadaEn = Instant.now(); }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public Profesor getProfesor() { return profesor; }
    public String getMateria() { return materia; }
    public BigDecimal getValor() { return valor; }
    public String getObservacion() { return observacion; }
    public Instant getCreadaEn() { return creadaEn; }

    public void actualizar(BigDecimal valor, String observacion) {
        this.valor = valor;
        this.observacion = observacion;
    }
}
