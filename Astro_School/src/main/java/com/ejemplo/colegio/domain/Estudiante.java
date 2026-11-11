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

    // Nullable durante la migracion de registros antiguos, antes sin numero de lista.
    // La aplicacion rellena esos registros al arrancar, sin modificar sus notas.
    @Column(name = "numero_lista", unique = true)
    private Integer numeroLista;

    @Column(nullable = false, unique = true, length = 180)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private long authVersion;

    @OneToMany(mappedBy = "estudiante", fetch = FetchType.LAZY)
    private List<Nota> notas = new ArrayList<>();

    protected Estudiante() { }

    public Estudiante(String identificacion, String nombre, String grupo, String correo, String passwordHash,
                      Integer numeroLista) {
        this.numeroLista = numeroLista;
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
    public Integer getNumeroLista() { return numeroLista; }

    /** Solo para completar estudiantes registrados antes de esta version. */
    public void completarNumeroLista(int numero) {
        if (numero < 1 || numero > 999999 || numeroLista != null)
            throw new IllegalStateException("El numero de lista no puede reasignarse");
        this.numeroLista = numero;
    }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public long getAuthVersion() { return authVersion; }
    public List<Nota> getNotas() { return notas; }
    public void cambiarPasswordHash(String hash) { this.passwordHash = hash; this.authVersion++; }
}
