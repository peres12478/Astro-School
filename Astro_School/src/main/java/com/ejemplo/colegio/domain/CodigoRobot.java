package com.ejemplo.colegio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "codigos_robots", indexes = @Index(name = "idx_codigos_robot", columnList = "robot_id"))
public class CodigoRobot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, length = 20)
    private String lenguaje;
    @Lob @Column(nullable = false)
    private String contenido;
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;
    protected CodigoRobot() { }
    public CodigoRobot(Robot robot, String nombre, String lenguaje, String contenido) {
        this.robot = robot;
        this.nombre = nombre;
        this.lenguaje = lenguaje;
        this.contenido = contenido;
        this.creadoEn = Instant.now();
    }
    public Long getId() { return id; }
    public Robot getRobot() { return robot; }
    public String getNombre() { return nombre; }
    public String getLenguaje() { return lenguaje; }
    public String getContenido() { return contenido; }
    public Instant getCreadoEn() { return creadoEn; }
}
