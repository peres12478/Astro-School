package com.ejemplo.colegio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ClaseIniciadaNotificador {
    private static final Logger log = LoggerFactory.getLogger(ClaseIniciadaNotificador.class);
    private final NotificacionClaseService servicio;
    private final CorreoService correo;

    public ClaseIniciadaNotificador(NotificacionClaseService servicio, CorreoService correo) {
        this.servicio = servicio;
        this.correo = correo;
    }

    @Async("correoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alIniciar(ClaseIniciadaEvent evento) {
        try {
            NotificacionClaseService.PlanEnvio plan = servicio.planificar(evento.claseId());
            if (plan == null) return;
            int enviados = 0;
            int fallidos = 0;
            for (String destino : plan.correos()) {
                try {
                    // La llamada de red SMTP sucede FUERA de una transaccion con la base de datos.
                    correo.notificarClase(destino, plan);
                    enviados++;
                } catch (Exception ex) {
                    fallidos++;
                    log.warn("Fallo email para clase {}: {}", plan.claseId(), ex.getClass().getSimpleName());
                }
            }
            servicio.registrarResultado(plan.claseId(), enviados, fallidos, plan.correos().size());
            log.info("Clase {}: enviados={}, fallidos={}", plan.claseId(), enviados, fallidos);
        } catch (Exception ex) {
            log.error("Error procesando avisos de clase {}: {}",
                    evento.claseId(), ex.getClass().getSimpleName());
        }
    }
}
