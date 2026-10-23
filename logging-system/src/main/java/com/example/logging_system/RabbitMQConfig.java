package com.example.logging_system;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "logs_direct_exchange";
    public static final String ALL_LOGS_QUEUE = "all_logs_queue";
    public static final String ERRORS_ONLY_QUEUE = "errors_only_queue";
    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDERS_QUEUE = "orders.queue";
    public static final String ORDERS_ROUTING_KEY = "order.created";
    public static final String DLX_EXCHANGE = "orders.dlx";
    public static final String DLQ_QUEUE = "orders.dlq";
    public static final String DLX_ROUTING_KEY = "order.dead";

    @Bean
    DirectExchange logsExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    Queue allLogsQueue() {
        return QueueBuilder.durable(ALL_LOGS_QUEUE).build();
    }

    @Bean
    Queue errorsOnlyQueue() {
        return QueueBuilder.durable(ERRORS_ONLY_QUEUE).build();
    }

    @Bean
    DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue ordersQueue() {
        return QueueBuilder.durable(ORDERS_QUEUE)
                .withArgument("x-message-ttl", 30000)
                .withArgument("x-max-length", 1000)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    Binding allInfo(Queue allLogsQueue, DirectExchange logsExchange) {
        return BindingBuilder.bind(allLogsQueue).to(logsExchange).with("INFO");
    }

    @Bean
    Binding allWarning(Queue allLogsQueue, DirectExchange logsExchange) {
        return BindingBuilder.bind(allLogsQueue).to(logsExchange).with("WARNING");
    }

    @Bean
    Binding allError(Queue allLogsQueue, DirectExchange logsExchange) {
        return BindingBuilder.bind(allLogsQueue).to(logsExchange).with("ERROR");
    }

    @Bean
    Binding criticalError(Queue errorsOnlyQueue, DirectExchange logsExchange) {
        return BindingBuilder.bind(errorsOnlyQueue).to(logsExchange).with("ERROR");
    }

    @Bean
    Binding ordersBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue).to(ordersExchange).with(ORDERS_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLX_ROUTING_KEY);
    }
}