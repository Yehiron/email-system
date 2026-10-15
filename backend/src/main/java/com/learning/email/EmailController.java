package com.learning.email;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emails")
class EmailController {
    private final EmailService service;
    EmailController(EmailService service) { this.service = service; }

    @PostMapping(consumes = "application/json")
    ResponseEntity<EmailResult> send(@Valid @RequestBody EmailRequest request) {
        EmailResult result = service.send(request);
        return ResponseEntity.status(result.status().equals("SIMULATED") ? 200 : 202).body(result);
    }
}
