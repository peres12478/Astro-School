package com.ejemplo.colegio.domain;

/** Catalogo cerrado: no se aceptan cursos inventados desde el cliente. */
public enum CursoCodigo {
    ROBOTICA("Robótica", "Construye prototipos y aprende programacion, sensores y automatizacion."),
    FISICA("Física", "Explora el movimiento, la energia y los experimentos cientificos."),
    ASTRONOMIA("Astronomía", "Descubre planetas, estrellas, galaxias y el universo.");

    private final String nombre;
    private final String descripcion;

    CursoCodigo(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
