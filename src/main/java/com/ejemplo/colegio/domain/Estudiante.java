package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estudiantes", indexes = @Index(name = "idx_estudiante_grupo", columnList = "grupo"))
public class Estudiante {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String identificacion;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String grupo;

    @Column(nullable = false, unique = true, length = 180)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private long authVersion;

    @OneToMany(mappedBy = "estudiante", fetch = FetchType.LAZY)
    private List<Nota> notas = new ArrayList<>();

    protected Estudiante() { }

    public Estudiante(String identificacion, String nombre, String grupo, String correo, String passwordHash) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.grupo = grupo;
        this.correo = correo;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getGrupo() { return grupo; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public long getAuthVersion() { return authVersion; }
    public List<Nota> getNotas() { return notas; }
    public void cambiarPasswordHash(String hash) { this.passwordHash = hash; this.authVersion++; }
}
