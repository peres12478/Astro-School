package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profesores", indexes = @Index(name = "idx_profesor_materia", columnList = "materia"))
public class Profesor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String identificacion;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String materia;

    @Column(nullable = false, unique = true, length = 180)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private long authVersion;

    @OneToMany(mappedBy = "profesor", fetch = FetchType.LAZY)
    private List<Nota> notas = new ArrayList<>();

    @OneToMany(mappedBy = "profesor", fetch = FetchType.LAZY)
    private List<Trabajo> trabajos = new ArrayList<>();

    @OneToMany(mappedBy = "profesor", fetch = FetchType.LAZY)
    private List<ClaseVirtual> clases = new ArrayList<>();

    protected Profesor() { }

    public Profesor(String identificacion, String nombre, String materia, String correo, String passwordHash) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.materia = materia;
        this.correo = correo;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getMateria() { return materia; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public long getAuthVersion() { return authVersion; }
    public List<Nota> getNotas() { return notas; }
    public List<Trabajo> getTrabajos() { return trabajos; }
    public List<ClaseVirtual> getClases() { return clases; }
    public void cambiarPasswordHash(String hash) { this.passwordHash = hash; this.authVersion++; }
}
