package com.example.logging_system;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    private final RabbitTemplate rabbitTemplate;

    public OrderController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOrder(@RequestBody Map<String, String> payload) {
        String orderId = payload.getOrDefault("orderId", "ORD-" + System.currentTimeMillis());
        String customerName = payload.getOrDefault("customerName", "Unknown");
        boolean forceFail = Boolean.parseBoolean(payload.getOrDefault("forceFail", "false"));

        String message = String.format("orderId=%s customer=%s force=%s marker=%s time=%s",
                orderId, customerName, forceFail, forceFail ? "FAIL" : "OK", LocalDateTime.now());

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ORDERS_ROUTING_KEY, message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ENVIADA");
        response.put("orderId", orderId);
        response.put("forceFail", forceFail);
        response.put("message", message);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String broker = rabbitTemplate.execute(channel -> channel.getConnection().getAddress().getHostAddress());
            result.put("backend", "ONLINE");
            result.put("rabbitmq", "CONNECTED");
            result.put("broker", broker);
        } catch (Exception ex) {
            result.put("backend", "ONLINE");
            result.put("rabbitmq", "DISCONNECTED");
            result.put("error", ex.getMessage());
        }
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long ordersReady = rabbitTemplate.execute(channel -> channel.messageCount(RabbitMQConfig.ORDERS_QUEUE));
        Long dlqReady = rabbitTemplate.execute(channel -> channel.messageCount(RabbitMQConfig.DLQ_QUEUE));

        result.put("ordersReady", ordersReady == null ? 0 : ordersReady);
        result.put("dlqReady", dlqReady == null ? 0 : dlqReady);
        result.put("alert", dlqReady != null && dlqReady > 0);
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}