package com.ejemplo.colegio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de las funciones nuevas, manteniendo la API escolar anterior. */
@SpringBootTest(properties = {
        "app.demo.enabled=true",
        "app.noticias.directorio=./data/astro-test-archivos",
        "spring.datasource.url=jdbc:h2:mem:astroschool_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AstroSchoolIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    String entrar(String documento) throws Exception {
        String response = mvc.perform(post("/api/demo/entrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identificacion", documento))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    @Test
    void estudiantesSoloPuedenInscribirseEnUnCursoALaVez() throws Exception {
        mvc.perform(get("/api/cursos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("ROBOTICA"))
                .andExpect(jsonPath("$[1].codigo").value("FISICA"))
                .andExpect(jsonPath("$[2].codigo").value("ASTRONOMIA"));

        mvc.perform(get("/api/cursos/mi-inscripcion")).andExpect(status().isUnauthorized());
        String profe = entrar("9001001");
        String alumno = entrar("9002001");
        mvc.perform(post("/api/cursos/inscripciones")
                .header("Authorization", "Bearer " + profe)
                .contentType(MediaType.APPLICATION_JSON).content("{\"curso\":\"ROBOTICA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/cursos/inscripciones")
                .header("Authorization", "Bearer " + alumno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"curso\":\"ROBOTICA\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.curso").value("ROBOTICA"));
        mvc.perform(post("/api/cursos/inscripciones")
                .header("Authorization", "Bearer " + alumno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"curso\":\"FISICA\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/cursos/mi-inscripcion")
                .header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.curso").value("ROBOTICA"));

        mvc.perform(delete("/api/cursos/mi-inscripcion")
                .header("Authorization", "Bearer " + alumno)).andExpect(status().isNoContent());
        mvc.perform(post("/api/cursos/inscripciones")
                .header("Authorization", "Bearer " + alumno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"curso\":\"FISICA\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.curso").value("FISICA"));
        mvc.perform(get("/api/cursos/mi-inscripcion")
                .header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.curso").value("FISICA"));
    }

    @Test
    void noticiasPublicasConAdjuntoYBloqueoDeTiposPeligrosos() throws Exception {
        mvc.perform(get("/api/noticias")).andExpect(status().isOk());
        MockMultipartFile pdf = new MockMultipartFile("archivo", "boletin.pdf", "application/pdf",
                "%PDF-1.4\nHola Astro School".getBytes(StandardCharsets.UTF_8));
        String respuesta = mvc.perform(multipart("/api/noticias")
                .file(pdf).param("titulo", "Feria de ciencia")
                .param("descripcion", "Todos los visitantes pueden consultar este boletin")
                .param("autor", "Comunidad Astro"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Feria de ciencia"))
                .andExpect(jsonPath("$.nombreArchivo").value("boletin.pdf"))
                .andReturn().getResponse().getContentAsString();
        JsonNode noticia = json.readTree(respuesta);
        long noticiaId = noticia.get("id").asLong();
        mvc.perform(get("/api/noticias/" + noticiaId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.autor").value("Comunidad Astro"));
        mvc.perform(get("/api/noticias/" + noticiaId + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")));
        MockMultipartFile html = new MockMultipartFile("archivo", "peligro.html", "text/html",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/noticias").file(html)
                .param("titulo", "Otro titulo").param("descripcion", "Descripcion")
                .param("autor", "Anonimo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swaggerMantieneRutasAntiguasYAgregaNuevas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/noticias']").exists())
                .andExpect(jsonPath("$['paths']['/api/cursos']").exists())
                .andExpect(jsonPath("$['paths']['/api/notas']").exists());
    }
}
