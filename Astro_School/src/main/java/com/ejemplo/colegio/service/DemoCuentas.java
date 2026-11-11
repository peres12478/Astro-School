package com.ejemplo.colegio.service;

import com.ejemplo.colegio.domain.TipoUsuario;
import java.util.List;
import java.util.Optional;

/** Usuarios ficticios y públicos; nunca usar sus credenciales con datos reales. */
public final class DemoCuentas {
    private DemoCuentas() { }

    public static final String CLAVE = "Demo2026!";
    public record Cuenta(String identificacion, String nombre, TipoUsuario rol,
                         String materiaOGrupo, String correo, Integer numeroLista) { }

    public static final List<Cuenta> CUENTAS = List.of(
            new Cuenta("9001001", "Ana Docente (demo)", TipoUsuario.PROFESOR,
                    "Matemáticas", "profe.mates@demo.colegio.test", null),
            new Cuenta("9001002", "Carlos Docente (demo)", TipoUsuario.PROFESOR,
                    "Ciencias", "profe.ciencias@demo.colegio.test", null),
            new Cuenta("9002001", "Laura Estudiante (demo)", TipoUsuario.ESTUDIANTE,
                    "11-2", "laura@demo.colegio.test", 101),
            new Cuenta("9002002", "Mateo Estudiante (demo)", TipoUsuario.ESTUDIANTE,
                    "11-2", "mateo@demo.colegio.test", 102),
            new Cuenta("9002003", "Sofía Estudiante (demo)", TipoUsuario.ESTUDIANTE,
                    "11-2", "sofia@demo.colegio.test", 103)
    );

    public static Optional<Cuenta> buscar(String identificacion) {
        return CUENTAS.stream().filter(c -> c.identificacion().equals(identificacion)).findFirst();
    }
}
