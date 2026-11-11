package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "clases_virtuales", indexes = {
        @Index(name = "idx_clase_grupo_estado", columnList = "grupo, estado"),
        @Index(name = "idx_clase_profesor", columnList = "profesor_id")
})
public class ClaseVirtual {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @Column(nullable = false, length = 30)
    private String grupo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String enlace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoClase estado = EstadoClase.ACTIVA;

    @Column(nullable = false, updatable = false)
    private Instant iniciadaEn;

    private Instant finalizadaEn;

    @Column(length = 2048)
    private String urlGrabacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoNotificacion estadoNotificacion = EstadoNotificacion.PENDIENTE;

    private int correosEnviados;
    private int correosFallidos;

    protected ClaseVirtual() { }

    public ClaseVirtual(Profesor profesor, String grupo, String titulo, String enlace) {
        this.profesor = profesor;
        this.grupo = grupo;
        this.titulo = titulo;
        this.enlace = enlace;
    }

    @PrePersist
    void antesDeCrear() { iniciadaEn = Instant.now(); }

    public void finalizar() {
        if (estado == EstadoClase.FINALIZADA) return;
        estado = EstadoClase.FINALIZADA;
        finalizadaEn = Instant.now();
    }

    public void registrarGrabacion(String url) { urlGrabacion = url; }

    public void actualizarNotificacion(int enviados, int fallidos, int destinatarios) {
        correosEnviados = enviados;
        correosFallidos = fallidos;
        estadoNotificacion = destinatarios == 0 ? EstadoNotificacion.SIN_DESTINATARIOS
                : fallidos == 0 ? EstadoNotificacion.ENVIADA
                : enviados == 0 ? EstadoNotificacion.FALLIDA : EstadoNotificacion.PARCIAL;
    }

    public Long getId() { return id; }
    public Profesor getProfesor() { return profesor; }
    public String getGrupo() { return grupo; }
    public String getTitulo() { return titulo; }
    public String getEnlace() { return enlace; }
    public EstadoClase getEstado() { return estado; }
    public Instant getIniciadaEn() { return iniciadaEn; }
    public Instant getFinalizadaEn() { return finalizadaEn; }
    public String getUrlGrabacion() { return urlGrabacion; }
    public EstadoNotificacion getEstadoNotificacion() { return estadoNotificacion; }
    public int getCorreosEnviados() { return correosEnviados; }
    public int getCorreosFallidos() { return correosFallidos; }
}
