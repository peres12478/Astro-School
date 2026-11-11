package com.ejemplo.colegio.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Astro School · API REST y manual Swagger", version = "4.0.0",
            description = "AUTORIZACIÓN / AUTHORIZE (arriba): es el lugar donde debes PEGAR EL TOKEN (JWT) que obtienes al iniciar sesión como PROFESOR o ESTUDIANTE. " +
                          "No es para escribir correo, cédula, número de lista ni contraseña. Para rutas de profesor, usa el token del profesor; para rutas de estudiante, usa el token del estudiante. " +
                          "PASOS: 1) Usa POST /api/demo/entrar para un token de prueba o POST /api/auth/login (los estudiantes indican numeroLista). " +
                          "2) Copia token, pulsa Authorize, pégalo y pulsa Authorize otra vez. " +
                          "3) Para poner nota usa numeroLista del estudiante (p. ej. 101), NO el ID interno. " +
                          "4) Solo para EDITAR una nota usa el ID de ESA NOTA. " +
                          "5) NOVEDADES: la seccion 07 permite publicar y consultar noticias/adjuntos (multipart). " +
                          "6) CURSOS: la seccion 08 muestra los tres cursos; para inscribirse necesitas token de ESTUDIANTE y solo puedes tener uno activo. " +
                          "7) NUESTRAS CREACIONES: la seccion 09 es publica y permite filtrar por curso. " +
                          "8) TALLER DE ROBOTICA: seccion 10, requiere token de estudiante INSCRITO en Robotica; guarda robots y programas, sin ejecutarlos. " +
                          "9) FISICA y ASTRONOMIA: secciones 11 y 12, requieren inscripcion en el curso correspondiente. " +
                          "10) MODO DEMO (solo H2 local): inicia con profesores, 3 estudiantes inscritos en los cursos, notas, tarea, clase de prueba, noticia, robot, codigo y 3 proyectos con imagenes. Todas las publicaciones [DEMO] son ficticias. " +
                          "Para la interfaz Astro School abre http://localhost:8080/ ."),
    servers = @Server(url = "/", description = "Servidor actual (local o red)")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "PEGA AQUÍ EL TOKEN JWT devuelto al iniciar sesión como profesor o estudiante. Para probar endpoints de profesor, token del PROFESOR; para endpoints de estudiante, token del ESTUDIANTE. No escribas la contraseña ni el número de lista. Swagger añade 'Bearer' automáticamente.")
public class OpenApiConfig {
    /**
     * Solo se muestra UNA ruta por accion en Swagger.
     * Las rutas antiguas se conservan operativas para no romper clientes existentes.
     */
    @Bean
    public OpenApiCustomizer ocultarRutasDuplicadas() {
        return api -> {
            if (api.getPaths() == null) return;
            for (String ruta : new String[] {
                    "/api/auth/registro/profesor",
                    "/api/auth/registro/estudiante",
                    "/api/profesores/me",
                    "/api/estudiantes/me",
                    "/api/estudiantes/me/qr",
                    "/api/estudiantes/me/qr.png",
                    "/api/estudiantes/me/notas",
                    "/api/estudiantes/me/trabajos",
                    "/api/estudiantes/me/clases/activas",
                    "/api/estudiantes/me/clases/grabadas"
            }) {
                api.getPaths().remove(ruta);
            }
        };
    }
}
