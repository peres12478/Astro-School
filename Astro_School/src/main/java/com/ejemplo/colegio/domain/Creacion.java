package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "creaciones", indexes = {
        @Index(name = "idx_creaciones_curso", columnList = "curso"),
        @Index(name = "idx_creaciones_fecha", columnList = "creada_en")})
public class Creacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CursoCodigo curso;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Column(nullable = false, length = 4000)
    private String descripcion;
    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;
    @Column(length = 200) private String nombreArchivo;
    @Column(length = 120) private String tipoArchivo;
    @Column(length = 80) private String claveArchivo;
    private Long bytesArchivo;

    protected Creacion() { }
    public Creacion(Estudiante estudiante, CursoCodigo curso, String titulo, String descripcion,
                    String nombreArchivo, String tipoArchivo, String claveArchivo, Long bytesArchivo) {
        this.estudiante = estudiante;
        this.curso = curso;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.nombreArchivo = nombreArchivo;
        this.tipoArchivo = tipoArchivo;
        this.claveArchivo = claveArchivo;
        this.bytesArchivo = bytesArchivo;
        this.creadaEn = Instant.now();
    }
    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public CursoCodigo getCurso() { return curso; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Instant getCreadaEn() { return creadaEn; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getTipoArchivo() { return tipoArchivo; }
    public String getClaveArchivo() { return claveArchivo; }
    public Long getBytesArchivo() { return bytesArchivo; }
}
