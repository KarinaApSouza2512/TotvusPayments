package com.totvus.payments.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_IMPORTACAO     = "contas.importacao";
    public static final String QUEUE_IMPORTACAO_DLQ = "contas.importacao.dlq";
    public static final String EXCHANGE_IMPORTACAO  = "contas.importacao.exchange";
    public static final String EXCHANGE_DLQ         = "contas.importacao.dlq.exchange";

    @Bean
    public DirectExchange exchangeImportacao() {
        return new DirectExchange(EXCHANGE_IMPORTACAO);
    }

    @Bean
    public DirectExchange exchangeDlq() {
        return new DirectExchange(EXCHANGE_DLQ);
    }

    @Bean
    public Queue queueImportacao() {
        return QueueBuilder.durable(QUEUE_IMPORTACAO)
            .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
            .withArgument("x-dead-letter-routing-key", QUEUE_IMPORTACAO_DLQ)
            .build();
    }

    @Bean
    public Queue queueDlq() {
        return QueueBuilder.durable(QUEUE_IMPORTACAO_DLQ).build();
    }

    @Bean
    public Binding bindingImportacao(Queue queueImportacao, DirectExchange exchangeImportacao) {
        return BindingBuilder.bind(queueImportacao).to(exchangeImportacao)
            .with(QUEUE_IMPORTACAO);
    }

    @Bean
    public Binding bindingDlq(Queue queueDlq, DirectExchange exchangeDlq) {
        return BindingBuilder.bind(queueDlq).to(exchangeDlq).with(QUEUE_IMPORTACAO_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}