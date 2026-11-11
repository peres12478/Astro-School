package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "robots_estudiantiles", indexes = @Index(name = "idx_robots_estudiante", columnList = "estudiante_id"))
public class Robot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, length = 100)
    private String plataforma;
    @Column(nullable = false, length = 1500)
    private String descripcion;
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;
    protected Robot() { }
    public Robot(Estudiante estudiante, String nombre, String plataforma, String descripcion) {
        this.estudiante = estudiante;
        this.nombre = nombre;
        this.plataforma = plataforma;
        this.descripcion = descripcion;
        this.creadoEn = Instant.now();
    }
    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public String getNombre() { return nombre; }
    public String getPlataforma() { return plataforma; }
    public String getDescripcion() { return descripcion; }
    public Instant getCreadoEn() { return creadoEn; }
}
