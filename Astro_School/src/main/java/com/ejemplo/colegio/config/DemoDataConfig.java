package com.ejemplo.colegio.config;

import com.ejemplo.colegio.domain.Estudiante;
import com.ejemplo.colegio.domain.Profesor;
import com.ejemplo.colegio.domain.TipoUsuario;
import com.ejemplo.colegio.repository.EstudianteRepository;
import com.ejemplo.colegio.repository.ProfesorRepository;
import com.ejemplo.colegio.service.DemoCuentas;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Semillas idempotentes en H2: no borra ni altera registros existentes. */
@Configuration
@Profile("h2")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoDataConfig {
    @Bean
    @Order(20)
    ApplicationRunner crearCuentasDePrueba(ProfesorRepository profesores,
            EstudianteRepository estudiantes, PasswordEncoder passwords) {
        return args -> {
            for (DemoCuentas.Cuenta c : DemoCuentas.CUENTAS) {
                boolean ocupado = profesores.existsByIdentificacion(c.identificacion())
                        || estudiantes.existsByIdentificacion(c.identificacion())
                        || profesores.existsByCorreoIgnoreCase(c.correo())
                        || estudiantes.existsByCorreoIgnoreCase(c.correo());
                if (ocupado) continue; // Respeta registros de la base de datos actual.
                String hash = passwords.encode(DemoCuentas.CLAVE);
                if (c.rol() == TipoUsuario.PROFESOR) {
                    profesores.save(new Profesor(c.identificacion(), c.nombre(),
                            c.materiaOGrupo(), c.correo(), hash));
                } else {
                    if (estudiantes.existsByNumeroLista(c.numeroLista())) continue;
                    estudiantes.save(new Estudiante(c.identificacion(), c.nombre(),
                            c.materiaOGrupo(), c.correo(), hash, c.numeroLista()));
                }
            }
        };
    }
}
