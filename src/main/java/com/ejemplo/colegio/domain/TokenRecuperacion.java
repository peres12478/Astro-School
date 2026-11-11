package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tokens_recuperacion", indexes = {
        @Index(name = "idx_recuperacion_usuario", columnList = "tipo_usuario, usuario_id")
})
public class TokenRecuperacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private TipoUsuario tipoUsuario;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private Instant expiraEn;

    private Instant utilizadoEn;

    protected TokenRecuperacion() { }

    public TokenRecuperacion(TipoUsuario tipoUsuario, Long usuarioId, String tokenHash, Instant expiraEn) {
        this.tipoUsuario = tipoUsuario;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = Instant.now();
        this.expiraEn = expiraEn;
    }

    public boolean esValido(Instant ahora) {
        return utilizadoEn == null && expiraEn.isAfter(ahora);
    }

    public void marcarUtilizado(Instant ahora) { utilizadoEn = ahora; }
    public Long getId() { return id; }
    public TipoUsuario getTipoUsuario() { return tipoUsuario; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getExpiraEn() { return expiraEn; }
    public Instant getUtilizadoEn() { return utilizadoEn; }
}
