package com.learning.email;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.util.Base64;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class EmailApiTest {
    static ConfigurableApplicationContext context;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();
    static final String auth = "Basic " + Base64.getEncoder().encodeToString("tester:test-password".getBytes());
    @BeforeAll static void start() {
        context = SpringApplication.run(EmailApplication.class, "--server.port=0", "--app.username=tester",
            "--app.password=test-password", "--app.email.mode=simulation");
        base = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
    }
    @AfterAll static void stop() { if (context != null) context.close(); }
    static HttpResponse<String> post(String body, boolean authenticated) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(base + "/api/v1/emails"))
            .header("Content-Type", "application/json");
        if (authenticated) req.header("Authorization", auth);
        return http.send(req.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void healthIsPublic() throws Exception {
        var res = http.send(HttpRequest.newBuilder(URI.create(base + "/actuator/health")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode()); assertTrue(res.body().contains("UP"));
    }
    @Test void sendingRequiresAuthentication() throws Exception {
        assertEquals(401, post("{}", false).statusCode());
    }
    @Test void validatesFieldsAndMalformedJson() throws Exception {
        assertEquals(400, post("""
            {"to":"invalid","subject":" ","message":""}
            """, true).statusCode());
        assertEquals(400, post("{broken", true).statusCode());
    }
    @Test void simulatesWithoutProviderAndThrottlesRepeatedRequests() throws Exception {
        String body = "{\"to\":\"test@example.com\",\"subject\":\"Hola\",\"message\":\"Prueba de aprendizaje\"}";
        var result = post(body, true);
        assertEquals(200, result.statusCode()); assertTrue(result.body().contains("SIMULATED"));
        assertEquals(429, post(body, true).statusCode());
    }
    @Test void rejectsOtherOrigins() throws Exception {
        var req = HttpRequest.newBuilder(URI.create(base + "/api/v1/emails"))
            .header("Origin", "https://untrusted.example")
            .header("Access-Control-Request-Method", "POST").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(403, http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
