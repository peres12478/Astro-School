package com.ejemplo.colegio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
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

/** Integración de laboratorios (sin exponer codigo privado ni permitir inscribirse en varios cursos). */
@SpringBootTest(properties = {
        "app.demo.enabled=true", "app.creaciones.directorio=./data/laboratorios-test-archivos",
        "spring.datasource.url=jdbc:h2:mem:laboratorios_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LaboratoriosIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    String token(String documento) throws Exception {
        String respuesta = mvc.perform(post("/api/demo/entrar").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identificacion", documento))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(respuesta).get("token").asText();
    }
    void inscribir(String auth, String curso) throws Exception {
        mvc.perform(post("/api/cursos/inscripciones").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"curso\":\"" + curso + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void roboticaRegistraRobotsGuardaCodigoYCreacionesSonPublicas() throws Exception {
        String estudiante = token("9002001");
        String segundo = token("9002002");
        String profesor = token("9001001");
        mvc.perform(get("/api/robotica/robots")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/robotica/robots").header("Authorization", segundo))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/robotica/robots").header("Authorization", profesor))
                .andExpect(status().isForbidden());
        inscribir(estudiante, "ROBOTICA");
        String robotJson = mvc.perform(post("/api/robotica/robots").header("Authorization", estudiante)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Astrobot 1\",\"plataforma\":\"Arduino Uno\",\"descripcion\":\"Robot de clase\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Astrobot 1"))
                .andReturn().getResponse().getContentAsString();
        long robotId = json.readTree(robotJson).get("id").asLong();
        mvc.perform(get("/api/robotica/robots").header("Authorization", estudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(robotId));
        String codeJson = mvc.perform(post("/api/robotica/robots/" + robotId + "/codigos")
                .header("Authorization", estudiante).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "parpadeo", "lenguaje", "ARDUINO",
                        "contenido", "void setup() {}\\nvoid loop() {}"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lenguaje").value("ARDUINO"))
                .andReturn().getResponse().getContentAsString();
        long codeId = json.readTree(codeJson).get("id").asLong();
        mvc.perform(get("/api/robotica/robots/" + robotId + "/codigos/" + codeId + "/descargar")
                .header("Authorization", estudiante))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
        mvc.perform(get("/api/robotica/robots/" + robotId + "/codigos").header("Authorization", segundo))
                .andExpect(status().isForbidden());
        inscribir(segundo, "FISICA");
        mvc.perform(get("/api/robotica/robots/" + robotId + "/codigos").header("Authorization", segundo))
                .andExpect(status().isForbidden());
        MockMultipartFile ino = new MockMultipartFile("archivo", "funciones.ino", "text/plain",
                "void loop() { }".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/robotica/robots/" + robotId + "/codigos/archivo").file(ino)
                .header("Authorization", estudiante)).andExpect(status().isCreated());
        MockMultipartFile peligro = new MockMultipartFile("archivo", "peligro.exe", "application/octet-stream", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/robotica/robots/" + robotId + "/codigos/archivo").file(peligro)
                .header("Authorization", estudiante)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/robotica/proyectos").header("Authorization", estudiante)
                .param("titulo", "Seguidor de linea").param("descripcion", "Mi prototipo con Arduino"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.curso").value("ROBOTICA"));
        mvc.perform(multipart("/api/fisica/proyectos").header("Authorization", estudiante)
                .param("titulo", "No pertenece").param("descripcion", "No debe publicar"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/fisica/proyectos").header("Authorization", segundo)
                .param("titulo", "Experimento de inercia").param("descripcion", "Observaciones de laboratorio"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.curso").value("FISICA"));
        mvc.perform(get("/api/creaciones"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/creaciones?curso=ROBOTICA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Seguidor de linea"));
        mvc.perform(get("/api/robotica/mi-espacio").header("Authorization", estudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.misProyectos.length()").value(1));
        mvc.perform(get("/api/fisica/mi-espacio").header("Authorization", segundo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.misProyectos.length()").value(1));
    }

    @Test
    void astronomiaArchivosYSwaggerConvivenConSistemaAnterior() throws Exception {
        String estudiante = token("9002002");
        inscribir(estudiante, "ASTRONOMIA");
        MockMultipartFile foto = new MockMultipartFile("archivo", "luna.png", "image/png",
                new byte[] {(byte) 0x89, 80, 78, 71, 13, 10, 26, 10, 1, 2});
        String body = mvc.perform(multipart("/api/astronomia/proyectos").file(foto)
                .header("Authorization", estudiante).param("titulo", "Fases de la Luna")
                .param("descripcion", "Observación nocturna"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombreArchivo").value("luna.png"))
                .andReturn().getResponse().getContentAsString();
        JsonNode proyecto = json.readTree(body);
        mvc.perform(get(proyecto.get("urlArchivo").asText()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
        mvc.perform(get("/api/creaciones?curso=ASTRONOMIA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].curso").value("ASTRONOMIA"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/notas']").exists())
                .andExpect(jsonPath("$['paths']['/api/noticias']").exists())
                .andExpect(jsonPath("$['paths']['/api/cursos']").exists())
                .andExpect(jsonPath("$['paths']['/api/creaciones']").exists())
                .andExpect(jsonPath("$['paths']['/api/robotica/robots']").exists())
                .andExpect(jsonPath("$['paths']['/api/fisica/mi-espacio']").exists())
                .andExpect(jsonPath("$['paths']['/api/astronomia/mi-espacio']").exists());
    }
}
