package com.ejemplo.colegio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecuperacionNotificador {
    private static final Logger log = LoggerFactory.getLogger(RecuperacionNotificador.class);
    private final CorreoService correo;

    public RecuperacionNotificador(CorreoService correo) { this.correo = correo; }

    @Async("correoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alSolicitar(RecuperacionEvent evento) {
        try { correo.enviarRecuperacion(evento.correo(), evento.token()); }
        catch (Exception ex) {
            // Nunca registrar el token o el contenido del mensaje.
            log.error("Fallo el envio de recuperacion: {}", ex.getClass().getSimpleName());
        }
    }
}
