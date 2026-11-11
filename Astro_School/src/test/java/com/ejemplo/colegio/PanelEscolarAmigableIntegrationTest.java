package com.ejemplo.colegio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Evita regresiones en el panel público y en los mensajes de éxito. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:colegio_panel_es;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.registro-docente-clave=clave-de-prueba"
})
@AutoConfigureMockMvc
class PanelEscolarAmigableIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void paginaPublicaYRecursosAccesiblesSinLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(get("/app.css"))
                .andExpect(status().isOk());
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk());
        String documentacion = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andReturn().getResponse().getContentAsString();
        JsonNode rutas = json.readTree(documentacion).path("paths");
        assertThat(rutas.has("/api/auth/registro/estudiantes")).isTrue();
        assertThat(rutas.has("/api/auth/registro/estudiante")).isFalse();
        assertThat(rutas.has("/api/auth/registro/profesores")).isTrue();
        assertThat(rutas.has("/api/auth/registro/profesor")).isFalse();
    }

    @Test
    void crearYEditarNotaDevuelveMensajeYConservaDatosOriginales() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        JsonNode prof = json.readTree(mvc.perform(post("/api/auth/registro/profesores")
                .header("X-Clave-Docente", "clave-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "identificacion", "P-" + unique, "nombre", "Profe Ana", "materia", "Matematicas",
                        "correo", "prof-" + unique + "@example.test", "password", "Clave1234!"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        JsonNode alumno = json.readTree(mvc.perform(post("/api/auth/registro/estudiantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "identificacion", "E-" + unique, "nombre", "Luis Alumno", "grupo", "11-2",
                        "numeroLista", 2233,
                        "correo", "alum-" + unique + "@example.test", "password", "Clave1234!"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        long estudianteId = alumno.path("usuarioId").asLong();
        String bearer = "Bearer " + prof.path("token").asText();
        JsonNode nota = json.readTree(mvc.perform(post("/api/notas")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("numeroLista", 2233, "valor", 4.2,
                        "observacion", "Buen trabajo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Nota agregada correctamente."))
                .andExpect(jsonPath("$.materia").value("Matematicas"))
                .andReturn().getResponse().getContentAsString());
        long notaId = nota.path("id").asLong();
        assertThat(notaId).isPositive();
        assertThat(nota.path("estudianteId").asLong()).isEqualTo(estudianteId);
        assertThat(nota.path("numeroLista").asInt()).isEqualTo(2233);

        mvc.perform(get("/api/profesores/estudiantes/numero-lista/2233")
                .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Luis Alumno"))
                .andExpect(jsonPath("$.numeroLista").value(2233));

        mvc.perform(put("/api/notas/{id}", notaId)
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("valor", 4.9, "observacion", "Muy bien"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Nota modificada correctamente."))
                .andExpect(jsonPath("$.valor").value(4.9))
                .andExpect(jsonPath("$.id").value(notaId));

        mvc.perform(get("/api/notas/estudiante/{estudianteId}", estudianteId)
                .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notaId))
                .andExpect(jsonPath("$[0].valor").value(4.9));
    }
}
