package com.learning.email;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class EmailService {
    private final WorkerClient client;
    private final String mode;
    private long lastAttemptNanos;
    EmailService(WorkerClient client, @Value("${app.email.mode}") String mode) {
        if (!mode.equals("simulation") && !mode.equals("real")) throw new IllegalArgumentException("EMAIL_MODE debe ser simulation o real");
        this.client = client;
        this.mode = mode;
    }
    public EmailResult send(EmailRequest request) {
        enforceRateLimit();
        String id = UUID.randomUUID().toString();
        if (mode.equals("simulation")) {
            return new EmailResult(id, "SIMULATED", "Simulación completada: no se envió ningún correo.");
        }
        client.send(request, id);
        return new EmailResult(id, "ACCEPTED", "Correo aceptado por el proveedor. La entrega aún no está confirmada.");
    }
    private synchronized void enforceRateLimit() {
        long now = System.nanoTime();
        if (lastAttemptNanos != 0 && now - lastAttemptNanos < 5_000_000_000L) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Espera 5 segundos entre intentos.");
        }
        lastAttemptNanos = now;
    }
}
