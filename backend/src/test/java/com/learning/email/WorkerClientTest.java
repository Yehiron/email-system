package com.learning.email;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WorkerClientTest {
    @Test void sendsAuthenticatedPayloadAndRequiresProviderAcceptance() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        var body = new AtomicReference<String>();
        var authorization = new AtomicReference<String>();
        server.createContext("/accepted", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1); exchange.close();
        });
        server.createContext("/rejected", exchange -> { exchange.sendResponseHeaders(500, -1); exchange.close(); });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            var mail = new EmailRequest("user@example.com", "Hola", "Contenido");
            new WorkerClient(url + "/accepted", "secret", "real").send(mail, "test-id");
            assertEquals("Bearer secret", authorization.get());
            assertTrue(body.get().contains("user@example.com"));
            assertTrue(body.get().contains("test-id"));
            var ex = assertThrows(ResponseStatusException.class,
                () -> new WorkerClient(url + "/rejected", "secret", "real").send(mail, "test-id"));
            assertEquals(502, ex.getStatusCode().value());
        } finally { server.stop(0); }
    }
    @Test void realModeRequiresSecretAndSecureUrl() {
        assertThrows(IllegalArgumentException.class, () -> new WorkerClient("https://example.com", "", "real"));
        assertThrows(IllegalArgumentException.class, () -> new WorkerClient("http://example.com", "secret", "real"));
    }
}
