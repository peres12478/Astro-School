package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.TipoUsuario;
import java.util.List;
import java.util.Optional;

/** Usuarios ficticios y públicos; nunca usar sus credenciales con datos reales. */
public final class DemoCuentas {
    private DemoCuentas() { }

    public static final String CLAVE = "Demo2026!";
    public record Cuenta(String identificacion, String nombre, TipoUsuario rol,
                         String materiaOGrupo, String correo) { }

    public static final List<Cuenta> CUENTAS = List.of(
            new Cuenta("9001001", "Ana Docente (demo)", TipoUsuario.PROFESOR,
                    "Matemáticas", "profe.mates@demo.colegio.test"),
            new Cuenta("9001002", "Carlos Docente (demo)", TipoUsuario.PROFESOR,
                    "Ciencias", "profe.ciencias@demo.colegio.test"),
            new Cuenta("9002001", "Laura Estudiante (demo)", TipoUsuario.ESTUDIANTE,
                    "11-2", "laura@demo.colegio.test"),
            new Cuenta("9002002", "Mateo Estudiante (demo)", TipoUsuario.ESTUDIANTE,
                    "11-2", "mateo@demo.colegio.test")
    );

    public static Optional<Cuenta> buscar(String identificacion) {
        return CUENTAS.stream().filter(c -> c.identificacion().equals(identificacion)).findFirst();
    }
}
