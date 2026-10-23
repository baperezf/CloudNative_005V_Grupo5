package com.example.logging_system;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {

    @RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
    public void processOrder(String message) {
        System.out.println("[PROCESADOR] Recibida: " + message);

        if (message.contains("FAIL")) {
            System.out.println("[ERROR SIMULADO] La orden fue marcada para fallar.");
            throw new RuntimeException("Fallo didáctico solicitado para probar DLQ");
        }

        System.out.println("[ÉXITO] Orden procesada correctamente.");
    }
}