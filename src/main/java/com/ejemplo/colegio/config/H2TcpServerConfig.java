package com.ejemplo.colegio.config;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.io.IOException;

/**
 * Ofrece la base H2 abierta por Spring Boot a los clientes JDBC del mismo PC.
 * DBeaver debe utilizar H2 Server: jdbc:h2:tcp://localhost:9092/colegio
 * No se habilitan conexiones desde otros computadores ni la creacion remota.
 */
@Configuration
@Profile("h2")
@ConditionalOnProperty(prefix = "app.h2.tcp", name = "enabled", havingValue = "true")
public class H2TcpServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer(@Value("${app.h2.tcp.port:9092}") int puerto)
            throws SQLException, IOException {
        // Debe coincidir con ./data/colegio del datasource principal.
        Path carpetaDatos = Path.of("data").toAbsolutePath().normalize();
        Files.createDirectories(carpetaDatos);

        return Server.createTcpServer(
                "-tcpPort", Integer.toString(puerto),
                "-tcpDaemon",
                "-baseDir", carpetaDatos.toString(),
                "-ifExists");
    }
}
