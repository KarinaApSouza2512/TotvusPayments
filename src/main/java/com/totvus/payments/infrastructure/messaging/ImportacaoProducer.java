package com.totvus.payments.infrastructure.messaging;

import com.totvus.payments.application.importacao.ImportacaoMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ImportacaoProducer {

  private final RabbitTemplate rabbitTemplate;

  public ImportacaoProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publicar(ImportacaoMessage message) {
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE_IMPORTACAO, RabbitMQConfig.QUEUE_IMPORTACAO, message);
  }
}
