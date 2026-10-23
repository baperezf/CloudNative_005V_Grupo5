package com.example.logging_system;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    @RabbitListener(queues = RabbitMQConfig.ALL_LOGS_QUEUE)
    public void receiveAllLogs(String message) {
        System.out.println("[MONITOR GENERAL] " + message);
    }

    @RabbitListener(queues = RabbitMQConfig.ERRORS_ONLY_QUEUE)
    public void receiveErrorLogs(String message) {
        System.out.println("[ALERTA CRITICA] " + message);
    }
}