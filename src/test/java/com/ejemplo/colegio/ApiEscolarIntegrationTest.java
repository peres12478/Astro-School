package com.ejemplo.colegio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.h2.tcp.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:colegiotest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.registro-docente-clave=secreto-pruebas"
})
@AutoConfigureMockMvc
class ApiEscolarIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void swaggerDebeGenerarDocumentoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths").isMap());
    }

    @Test
    void profesorPuedeCalificarConSuMateria_yEstudianteVeSusNotas_yQrEsSeguro() throws Exception {
        JsonNode profesor = enviarYLeer(mvc.perform(post("/api/auth/registro/profesor")
                .header("X-Clave-Docente", "secreto-pruebas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "identificacion", "DOC-123", "nombre", "Ana Gomez",
                        "materia", "Matematicas", "correo", "ana@pruebas.edu",
                        "password", "Password123!"))))
                .andExpect(status().isCreated()).andReturn());
        String jwtProfesor = profesor.get("token").asText();

        JsonNode estudiante = enviarYLeer(mvc.perform(post("/api/auth/registro/estudiante")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "identificacion", "EST-456", "nombre", "Luis Perez",
                        "grupo", "11-2", "correo", "luis@pruebas.edu",
                        "password", "Password456!"))))
                .andExpect(status().isCreated()).andReturn());
        String jwtEstudiante = estudiante.get("token").asText();
        long estudianteId = estudiante.get("usuarioId").asLong();

        mvc.perform(post("/api/notas")
                .header("Authorization", "Bearer " + jwtEstudiante)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("estudianteId", estudianteId, "valor", 4.5))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/notas")
                .header("Authorization", "Bearer " + jwtProfesor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("estudianteId", estudianteId, "valor", 4.5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.materia").value("Matematicas"));

        mvc.perform(get("/api/estudiantes/me/notas")
                .header("Authorization", "Bearer " + jwtEstudiante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value(4.5))
                .andExpect(jsonPath("$[0].materia").value("Matematicas"));

        JsonNode qr = enviarYLeer(mvc.perform(get("/api/estudiantes/me/qr")
                .header("Authorization", "Bearer " + jwtEstudiante))
                .andExpect(status().isOk()).andReturn());
        String textoQr = qr.get("contenido").asText();
        JsonNode lectura = enviarYLeer(mvc.perform(post("/api/profesores/qr/escanear")
                .header("Authorization", "Bearer " + jwtProfesor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("contenido", textoQr))))
                .andExpect(status().isOk()).andReturn());
        org.junit.jupiter.api.Assertions.assertEquals(estudianteId, lectura.get("estudianteId").asLong());

        // El token firmado para un QR nunca puede actuar como token Bearer.
        String jwtQr = textoQr; // El nuevo QR es un token firmado compacto (antes estaba dentro de JSON).
        mvc.perform(get("/api/estudiantes/me")
                .header("Authorization", "Bearer " + jwtQr))
                .andExpect(status().isUnauthorized());

        // Cambiar un carácter de la FIRMA del QR invalida la lectura.
        int separador = textoQr.lastIndexOf('.');
        String qrAlterado = textoQr.substring(0, separador + 1)
                + (textoQr.charAt(separador + 1) == 'A' ? 'B' : 'A')
                + textoQr.substring(separador + 2);
        mvc.perform(post("/api/profesores/qr/escanear")
                .header("Authorization", "Bearer " + jwtProfesor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("contenido", qrAlterado))))
                .andExpect(status().isBadRequest());
    }

    private JsonNode enviarYLeer(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }
}
