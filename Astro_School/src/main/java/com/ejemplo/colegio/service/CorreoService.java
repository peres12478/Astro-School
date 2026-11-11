package com.ejemplo.colegio.service;

import com.ejemplo.colegio.service.NotificacionClaseService.PlanEnvio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private final JavaMailSender remitente;
    private final String desde;

    public CorreoService(JavaMailSender remitente, @Value("${app.correo.desde}") String desde) {
        this.remitente = remitente;
        this.desde = desde;
    }

    public void notificarClase(String emailEstudiante, PlanEnvio clase) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(desde);
        m.setTo(emailEstudiante);
        m.setSubject("Nueva clase virtual: " + clase.titulo());
        m.setText("Se ha iniciado una clase virtual para tu grupo " + clase.grupo() + ".\n" +
                "Materia: " + clase.materia() + "\n" +
                "Profesor: " + clase.profesor() + "\n" +
                "Enlace: " + clase.enlace() + "\n");
        remitente.send(m);
    }

    public void enviarRecuperacion(String email, String token) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(desde);
        m.setTo(email);
        m.setSubject("Recuperacion de acceso - Sistema escolar");
        m.setText("Se solicito restablecer tu contrasena.\n\n" +
                "Abre http://localhost:8080/swagger-ui.html y ejecuta el endpoint " +
                "POST /api/auth/restablecer-password con el siguiente token temporal:\n\n" +
                token + "\n\nEste token vence en 15 minutos y solo se puede usar una vez. " +
                "Si no solicitaste esto, ignora el mensaje.");
        remitente.send(m);
    }
}
