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
    info = @Info(title = "Sistema Escolar · Manual de funciones", version = "1.3.0",
            description = "GUÍA RÁPIDA: 1) Si usas H2 local, elige una cuenta en «00. Prueba rápida» y ejecuta POST /api/demo/entrar. " +
                          "2) Copia el token y pulsa «Authorize» en Swagger, o usa el panel http://localhost:8080/ sin contraseñas de prueba. " +
                          "También puedes entrar con correo O identificación en «01. Acceso y registro». " +
                          "3) Para agregar una nota, busca primero al estudiante en «02. Profesores». " +
                          "4) Para EDITAR una nota usa el id de la NOTA devuelto al crearla, NO el id del alumno. " +
                          "La pantalla más sencilla para el uso diario está en http://localhost:8080/ ."),
    servers = @Server(url = "http://localhost:8080", description = "Servidor local")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
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
