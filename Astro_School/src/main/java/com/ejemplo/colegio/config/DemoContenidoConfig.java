package com.ejemplo.colegio.config;

import com.ejemplo.colegio.domain.*;
import com.ejemplo.colegio.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contenido FICTICIO de muestra, solo en H2 local y en modo demostracion.
 * Se agrega al arrancar sin borrar ni modificar registros de usuarios.
 * Los titulos marcados [DEMO] ayudan a distinguir los datos de una entrega real.
 */
@Configuration
@Profile("h2")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoContenidoConfig {
    private static final String AUTOR = "Equipo Astro School (DEMO)";

    @Bean
    @Order(30) // Las cuentas de prueba se crean primero, en DemoDataConfig (orden 20).
    ApplicationRunner ejemplosParaTodasLasSecciones(
            ProfesorRepository profesores, EstudianteRepository estudiantes,
            InscripcionCursoRepository inscripciones, NotaRepository notas,
            TrabajoRepository trabajos, ClaseVirtualRepository clases,
            NoticiaRepository noticias, CreacionRepository creaciones,
            RobotRepository robots, CodigoRobotRepository codigos,
            @Value("${app.creaciones.directorio:./data/creaciones}") String carpetaImagenes) {
        return args -> {
            var profe = profesores.findByIdentificacion("9001001").orElse(null);
            var ciencias = profesores.findByIdentificacion("9001002").orElse(null);
            var laura = estudiantes.findByIdentificacion("9002001").orElse(null);
            var mateo = estudiantes.findByIdentificacion("9002002").orElse(null);
            var sofia = estudiantes.findByIdentificacion("9002003").orElse(null);
            if (profe == null || ciencias == null || laura == null || mateo == null || sofia == null) return;

            inscribirSiLibre(inscripciones, laura, CursoCodigo.ROBOTICA);
            inscribirSiLibre(inscripciones, mateo, CursoCodigo.FISICA);
            inscribirSiLibre(inscripciones, sofia, CursoCodigo.ASTRONOMIA);

            String notaEjemplo = "[DEMO] Prueba de matemáticas de muestra";
            if (!notas.existsByEstudiante_IdAndProfesor_IdAndObservacion(laura.getId(), profe.getId(), notaEjemplo))
                notas.save(new Nota(laura, profe, new BigDecimal("4.50"), notaEjemplo));
            String notaEjemplo2 = "[DEMO] Ejercicio de ciencias de muestra";
            if (!notas.existsByEstudiante_IdAndProfesor_IdAndObservacion(mateo.getId(), ciencias.getId(), notaEjemplo2))
                notas.save(new Nota(mateo, ciencias, new BigDecimal("4.20"), notaEjemplo2));

            String tarea = "[DEMO] Diseña un experimento y documenta tus resultados";
            if (!trabajos.existsByProfesor_IdAndTitulo(ciencias.getId(), tarea))
                trabajos.save(new Trabajo(ciencias, "11-2", tarea,
                        "Actividad ficticia: escribe hipótesis, materiales, procedimiento, resultados y conclusiones.", null));

            String clase = "[DEMO] Sala virtual de prueba (no es una clase en vivo)";
            if (!clases.existsByProfesor_IdAndTitulo(profe.getId(), clase))
                clases.save(new ClaseVirtual(profe, "11-2", clase,
                        "https://meet.jit.si/AstroSchoolSalaDemostracionSinSesionReal"));

            String noticia = "[DEMO] ¡Bienvenidos a Astro School!";
            if (!noticias.existsByTituloAndAutor(noticia, AUTOR))
                noticias.save(new Noticia(noticia,
                        "Esta es una publicación ficticia para mostrar el Noticiero. Puedes subir tus noticias y adjuntar documentos o imágenes desde la página o Swagger.",
                        AUTOR, null, null, null, null));

            // Cada ejemplo pertenece a un estudiante del curso correspondiente.
            // Si alguien cambia la inscripción, no se crea una nueva ni se le mueve de curso.
            crearProyectoDemo(creaciones, laura, CursoCodigo.ROBOTICA,
                    "[DEMO] Seguidor de línea AstroBot",
                    "Prototipo escolar de ejemplo con Arduino, sensores infrarrojos y motores. Imagen ilustrativa creada para Astro School, no fotografía de un robot real.",
                    "robotica-demo.png", carpetaImagenes);
            crearProyectoDemo(creaciones, mateo, CursoCodigo.FISICA,
                    "[DEMO] Movimiento de un péndulo",
                    "Ejemplo de investigación: medir cómo varía el período de un péndulo según su longitud. Imagen ilustrativa de Astro School, no un experimento realizado.",
                    "fisica-demo.png", carpetaImagenes);
            crearProyectoDemo(creaciones, sofia, CursoCodigo.ASTRONOMIA,
                    "[DEMO] Observación de la Luna",
                    "Ejemplo de bitácora de observación lunar. La ilustración fue creada para esta demostración; puedes publicar tus propias fotografías astronómicas.",
                    "astronomia-demo.png", carpetaImagenes);

            String robotNombre = "[DEMO] AstroBot de líneas";
            Robot robot = robots.findByEstudianteIdAndNombre(laura.getId(), robotNombre)
                    .orElseGet(() -> robots.save(new Robot(laura, robotNombre, "Arduino Uno",
                            "Robot ficticio de demostración con dos motores DC y sensores infrarrojos.")));
            String programa = "[DEMO] parpadeo.ino";
            if (!codigos.existsByRobotIdAndNombre(robot.getId(), programa))
                codigos.save(new CodigoRobot(robot, programa, "ARDUINO",
                        "// Programa didáctico de demostración: LED integrado\n"
                      + "void setup() { pinMode(LED_BUILTIN, OUTPUT); }\n"
                      + "void loop() {\n"
                      + "  digitalWrite(LED_BUILTIN, HIGH);\n"
                      + "  delay(500);\n"
                      + "  digitalWrite(LED_BUILTIN, LOW);\n"
                      + "  delay(500);\n"
                      + "}\n"));
        };
    }

    private static void inscribirSiLibre(InscripcionCursoRepository repo, Estudiante estudiante, CursoCodigo curso) {
        if (repo.findByEstudianteId(estudiante.getId()).isEmpty())
            repo.save(new InscripcionCurso(estudiante, curso));
    }

    private static void crearProyectoDemo(CreacionRepository repo, Estudiante autor, CursoCodigo curso,
            String titulo, String descripcion, String recurso, String directorio) throws IOException {
        Path carpeta = Path.of(directorio).toAbsolutePath().normalize();
        Files.createDirectories(carpeta);
        Path imagen = carpeta.resolve("astro-" + recurso).normalize();
        if (!imagen.startsWith(carpeta)) throw new IOException("Ruta de demostracion invalida");
        if (Files.notExists(imagen)) {
            try (var origen = new ClassPathResource("demo-images/" + recurso).getInputStream()) {
                Files.copy(origen, imagen);
            }
        }
        if (!repo.existsByTituloAndEstudiante_Id(titulo, autor.getId())) {
            repo.save(new Creacion(autor, curso, titulo, descripcion, recurso, "image/png",
                    imagen.getFileName().toString(), Files.size(imagen)));
        }
    }
}
