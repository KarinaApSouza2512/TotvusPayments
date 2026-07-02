package com.totvus.payments.infrastructure.messaging;

import com.totvus.payments.application.importacao.ImportacaoMessage;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ImportacaoConsumer {

  private static final Logger log = LoggerFactory.getLogger(ImportacaoConsumer.class);

  private final ImportacaoLineProcessor lineProcessor;

  public ImportacaoConsumer(ImportacaoLineProcessor lineProcessor) {
    this.lineProcessor = lineProcessor;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_IMPORTACAO)
  public void processar(ImportacaoMessage message) {
    log.info("Iniciando importação. Protocolo: {}", message.protocolo());
    var reader = new BufferedReader(new StringReader(message.csvContent()));
    var contador = new AtomicInteger(0);
    var erros = new AtomicInteger(0);

    try {
      String cabecalho = reader.readLine();
      if (cabecalho == null) {
        throw new IllegalArgumentException("CSV vazio ou sem cabeçalho");
      }

      String linha;
      int numeroLinha = 1;
      while ((linha = reader.readLine()) != null) {
        numeroLinha++;
        int numAtual = numeroLinha;
        String linhaAtual = linha;
        try {
          lineProcessor.processarLinha(linhaAtual);
          contador.incrementAndGet();
        } catch (Exception e) {
          erros.incrementAndGet();
          log.warn(
              "Protocolo {}: erro na linha {} — {} | Linha: '{}'",
              message.protocolo(),
              numAtual,
              e.getMessage(),
              linhaAtual);
        }
      }
    } catch (Exception e) {
      log.error(
          "Protocolo {}: falha crítica ao processar CSV — {}", message.protocolo(), e.getMessage());
      throw new RuntimeException("Falha crítica no processamento do CSV", e);
    }

    log.info(
        "Importação concluída. Protocolo: {} | Sucesso: {} | Erros: {}",
        message.protocolo(),
        contador.get(),
        erros.get());
  }
}
