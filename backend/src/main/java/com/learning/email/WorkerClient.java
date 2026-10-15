package com.learning.email;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
class WorkerClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private final String token;
    WorkerClient(@Value("${app.worker.url}") String url, @Value("${app.worker.token}") String token,
                 @Value("${app.email.mode}") String mode) {
        this.url = url;
        this.token = token;
        if (mode.equals("real")) {
            URI uri = URI.create(url);
            if (token.isBlank()) throw new IllegalArgumentException("WORKER_TOKEN es obligatorio en modo real");
            if (!"https".equals(uri.getScheme()) && !("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost()))) {
                throw new IllegalArgumentException("WORKER_URL debe usar HTTPS (HTTP solo para localhost)");
            }
        }
    }
    void send(EmailRequest email, String id) {
        try {
            String body = mapper.writeValueAsString(Map.of("to", email.to(), "subject", email.subject(),
                "message", email.message(), "requestId", id));
            var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 202) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "El servicio de correo no confirmó la aceptación. No reintentes automáticamente.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Envío interrumpido; estado desconocido.");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No se pudo confirmar el envío. Comprueba el proveedor antes de reintentar.");
        }
    }
}
