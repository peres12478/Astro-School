package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "noticias", indexes = @Index(name = "idx_noticias_fecha", columnList = "creada_en"))
public class Noticia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String titulo;
    @Column(nullable = false, length = 5000)
    private String descripcion;
    @Column(nullable = false, length = 100)
    private String autor;
    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;
    @Column(length = 200)
    private String nombreArchivo;
    @Column(length = 80)
    private String tipoArchivo;
    @Column(length = 80)
    private String claveArchivo;
    private Long bytesArchivo;

    protected Noticia() { }

    public Noticia(String titulo, String descripcion, String autor,
                   String nombreArchivo, String tipoArchivo, String claveArchivo, Long bytesArchivo) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.autor = autor;
        this.creadaEn = Instant.now();
        this.nombreArchivo = nombreArchivo;
        this.tipoArchivo = tipoArchivo;
        this.claveArchivo = claveArchivo;
        this.bytesArchivo = bytesArchivo;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getAutor() { return autor; }
    public Instant getCreadaEn() { return creadaEn; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getTipoArchivo() { return tipoArchivo; }
    public String getClaveArchivo() { return claveArchivo; }
    public Long getBytesArchivo() { return bytesArchivo; }
}
