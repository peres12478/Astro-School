package com.ejemplo.colegio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.h2.tcp.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:colegio_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.registro-docente-clave=solo-test-secreto"
})
class SistemaEscolarIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender; // Los tests nunca dependen del servidor SMTP.

    @Test
    void flujoProfesorQrNotasYPermisos() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String profesorToken = registrarProfesor("profe-" + sufijo + "@example.test", sufijo);
        String alumnoToken = registrarEstudiante("alumno-" + sufijo + "@example.test", sufijo, "11-2");

        // Un estudiante no puede llamar rutas docentes.
        mvc.perform(post("/api/profesores/notas").header("Authorization", "Bearer " + alumnoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("estudianteId", 1, "valor", 5))))
                .andExpect(status().isForbidden());

        String qrBody = mvc.perform(get("/api/estudiantes/qr")
                .header("Authorization", "Bearer " + alumnoToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode qr = json.readTree(qrBody);
        assertThat(qr.path("imagenBase64").asText()).isNotEmpty();
        String contenido = qr.path("contenido").asText();
        assertThat(contenido.split("\\.")).hasSize(3);
        assertThat(qr.path("imagenBase64").asText()).isNotBlank();

        String escaneadoBody = mvc.perform(post("/api/profesores/qr/escanear")
                .header("Authorization", "Bearer " + profesorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("contenido", contenido))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode escaneado = json.readTree(escaneadoBody);
        assertThat(escaneado.path("materiaProfesor").asText()).isEqualTo("Matematicas");

        String nota = mvc.perform(post("/api/profesores/qr/calificar")
                .header("Authorization", "Bearer " + profesorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("contenido", contenido, "valor", 4.5,
                        "observacion", "Buen trabajo"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(nota).path("materia").asText()).isEqualTo("Matematicas");

        String resumen = mvc.perform(get("/api/estudiantes/notas/resumen")
                .header("Authorization", "Bearer " + alumnoToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(resumen).get(0).path("aprobado").asBoolean()).isTrue();
        assertThat(json.readTree(resumen).get(0).path("promedio").asDouble()).isEqualTo(4.5);
    }

    @Test
    void tareasYClasesSeFiltranPorGrupo() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String profesorToken = registrarProfesor("docente-" + sufijo + "@example.test", sufijo);
        String alumno112 = registrarEstudiante("grupo112-" + sufijo + "@example.test", sufijo + "1", "11-2");
        String alumno113 = registrarEstudiante("grupo113-" + sufijo + "@example.test", sufijo + "2", "11-3");

        mvc.perform(post("/api/profesores/trabajos")
                .header("Authorization", "Bearer " + profesorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("grupo", "11-2", "titulo", "Taller 1",
                        "descripcion", "Resolver ecuaciones"))))
                .andExpect(status().isCreated());

        String tareas112 = mvc.perform(get("/api/estudiantes/trabajos")
                .header("Authorization", "Bearer " + alumno112))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String tareas113 = mvc.perform(get("/api/estudiantes/trabajos")
                .header("Authorization", "Bearer " + alumno113))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(tareas112).size()).isGreaterThan(0);
        assertThat(json.readTree(tareas113).size()).isZero();

        mvc.perform(post("/api/profesores/clases")
                .header("Authorization", "Bearer " + profesorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("grupo", "11-2", "titulo", "Clase virtual 1"))))
                .andExpect(status().isCreated());

        String clases112 = mvc.perform(get("/api/estudiantes/clases/activas")
                .header("Authorization", "Bearer " + alumno112))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String clases113 = mvc.perform(get("/api/estudiantes/clases/activas")
                .header("Authorization", "Bearer " + alumno113))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(clases112).size()).isGreaterThan(0);
        assertThat(json.readTree(clases113).size()).isZero();
    }

    private String registrarProfesor(String correo, String sufijo) throws Exception {
        String body = json.writeValueAsString(Map.of("identificacion", "P-" + sufijo,
                "nombre", "Profesora Ana", "materia", "Matematicas", "correo", correo,
                "password", "ClaveSegura123!"));
        String salida = mvc.perform(post("/api/auth/registro/profesores")
                .header("X-Clave-Docente", "solo-test-secreto")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(salida).path("token").asText();
    }

    private String registrarEstudiante(String correo, String sufijo, String grupo) throws Exception {
        String body = json.writeValueAsString(Map.of("identificacion", "E-" + sufijo,
                "nombre", "Estudiante Luis", "grupo", grupo, "correo", correo,
                "password", "ClaveSegura123!"));
        String salida = mvc.perform(post("/api/auth/registro/estudiantes")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(salida).path("token").asText();
    }
}
