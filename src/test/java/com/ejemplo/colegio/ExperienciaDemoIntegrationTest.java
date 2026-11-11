package com.ejemplo.colegio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Asegura el recorrido más sencillo: probar con un clic, ver QR y poner nota. */
@SpringBootTest(properties = {
        "app.h2.tcp.enabled=false",
        "app.demo.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:colegio_demo_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ExperienciaDemoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void entrarPorDocumentoOCorreoYMostrarConfirmacion() throws Exception {
        mvc.perform(get("/api/demo/cuentas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        for (String documento : new String[]{"9001001", "9001002", "9002001", "9002002"}) {
            assertThat(entrarDemo(documento)).isNotBlank();
        }
        String docente = entrarDemo("9001001");
        mvc.perform(get("/api/profesores/estudiantes").param("grupo", "11-2")
                .header("Authorization", "Bearer " + docente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/profesores/yo").header("Authorization", "Bearer " + docente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materia").value("Matemáticas"));

        for (String identificador : new String[]{"9001001", "profe.mates@demo.colegio.test"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("usuario", identificador, "password", "Demo2026!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Sesión iniciada correctamente como profesor."))
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }
        // Compatibilidad: clientes antiguos todavía pueden enviar la propiedad correo.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo", "9002001", "password", "Demo2026!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("usuario", "9001001", "password", "incorrecta"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elQrGeneradoEsPngLegibleYPermiteCalificar() throws Exception {
        String alumno = entrarDemo("9002001");
        String docente = entrarDemo("9001001");
        JsonNode qr = json.readTree(mvc.perform(get("/api/estudiantes/qr")
                .header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenBase64").isNotEmpty())
                .andReturn().getResponse().getContentAsString());
        String contenido = qr.path("contenido").asText();
        byte[] png = Base64.getDecoder().decode(qr.path("imagenBase64").asText());
        assertThat(png.length).isGreaterThan(100);
        assertThat(png[0]).isEqualTo((byte) 0x89); // Encabezado PNG.
        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
        String leido = new MultiFormatReader().decode(new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();
        assertThat(leido).isEqualTo(contenido);

        mvc.perform(get("/api/estudiantes/qr.png").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));

        JsonNode identificado = json.readTree(mvc.perform(post("/api/profesores/qr/escanear")
                .header("Authorization", "Bearer " + docente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("contenido", contenido))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupo").value("11-2"))
                .andReturn().getResponse().getContentAsString());
        assertThat(identificado.path("estudianteId").asLong()).isPositive();

        mvc.perform(post("/api/profesores/qr/calificar")
                .header("Authorization", "Bearer " + docente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("contenido", contenido,
                        "valor", 4.7, "observacion", "Prueba de QR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Nota agregada correctamente desde el código QR."));
        mvc.perform(get("/api/estudiantes/notas").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].materia").value("Matemáticas"));
    }

    private String entrarDemo(String identificacion) throws Exception {
        String salida = mvc.perform(post("/api/demo/entrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identificacion", identificacion))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(salida).path("token").asText();
    }
}
