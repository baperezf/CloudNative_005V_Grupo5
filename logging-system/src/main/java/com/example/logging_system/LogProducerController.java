package com.example.logging_system;

import java.util.Set;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class LogProducerController {

    private static final Set<String> LEVELS = Set.of("INFO", "WARNING", "ERROR");
    private final RabbitTemplate rabbitTemplate;

    public LogProducerController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/log")
    public ResponseEntity<String> sendLog(@RequestBody LogMessage log) {
        String level = log.level() == null ? null : log.level().trim().toUpperCase();

        if (!LEVELS.contains(level)) {
            return ResponseEntity.badRequest().body("level debe ser INFO, WARNING o ERROR");
        }

        if (log.message() == null || log.message().isBlank()) {
            return ResponseEntity.badRequest().body("message no puede estar vacio");
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                level,
                log.message());

        return ResponseEntity.ok("Log enviado con nivel " + level);
    }

    public record LogMessage(String level, String message) {
    }
}