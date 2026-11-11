package com.ejemplo.colegio.config;

import com.ejemplo.colegio.domain.Estudiante;
import com.ejemplo.colegio.repository.EstudianteRepository;
import com.ejemplo.colegio.service.DemoCuentas;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Conserva los alumnos y sus notas de bases antiguas. Solo completa los numeros de
 * lista que antes no existian; nunca reasigna numeros ya registrados.
 */
@Configuration
public class NumeroListaMigracionConfig {
    @Bean
    @Order(10)
    ApplicationRunner completarNumerosDeLista(EstudianteRepository estudiantes) {
        return args -> {
            var todos = estudiantes.findAll().stream()
                    .sorted(Comparator.comparing(Estudiante::getId)).toList();
            Set<Integer> usados = new HashSet<>();
            for (var estudiante : todos) {
                if (estudiante.getNumeroLista() != null) usados.add(estudiante.getNumeroLista());
            }
            int siguiente = 1001;
            for (var estudiante : todos) {
                if (estudiante.getNumeroLista() != null) continue;
                Integer preferido = DemoCuentas.buscar(estudiante.getIdentificacion())
                        .filter(c -> c.correo().equalsIgnoreCase(estudiante.getCorreo()))
                        .map(DemoCuentas.Cuenta::numeroLista).orElse(null);
                int asignado;
                if (preferido != null && !usados.contains(preferido)) {
                    asignado = preferido;
                } else {
                    while (usados.contains(siguiente)) siguiente++;
                    if (siguiente > 999999) throw new IllegalStateException("No quedan numeros de lista disponibles");
                    asignado = siguiente++;
                }
                estudiante.completarNumeroLista(asignado);
                estudiantes.save(estudiante);
                usados.add(asignado);
            }
        };
    }
}
