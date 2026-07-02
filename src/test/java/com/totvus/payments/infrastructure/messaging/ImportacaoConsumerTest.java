package com.totvus.payments.infrastructure.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.totvus.payments.application.importacao.ImportacaoMessage;
import com.totvus.payments.domain.model.ImportacaoJob;
import com.totvus.payments.domain.model.ImportacaoStatus;
import com.totvus.payments.infrastructure.persistence.ImportacaoJobJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportacaoConsumerTest {

  @Mock private ImportacaoLineProcessor lineProcessor;
  @Mock private ImportacaoJobJpaRepository jobRepository;
  @InjectMocks private ImportacaoConsumer consumer;

  @Test
  @DisplayName("Ao concluir processamento, atualiza job com contagem de sucesso e erros")
  void deveAtualizarJobAoConcluirProcessamento() {
    var job = ImportacaoJob.iniciar("abc123");
    when(jobRepository.findById("abc123")).thenReturn(Optional.of(job));
    lenient()
        .doThrow(new IllegalArgumentException("linha ruim"))
        .when(lineProcessor)
        .processarLinha("linha2");

    var csv = "cabecalho\nlinha1\nlinha2\n";
    consumer.processar(new ImportacaoMessage("abc123", csv));

    var captor = ArgumentCaptor.forClass(ImportacaoJob.class);
    verify(jobRepository).save(captor.capture());
    var jobAtualizado = captor.getValue();
    assertThat(jobAtualizado.getStatus()).isEqualTo(ImportacaoStatus.CONCLUIDO_COM_ERROS);
    assertThat(jobAtualizado.getTotalSucesso()).isEqualTo(1);
    assertThat(jobAtualizado.getTotalErros()).isEqualTo(1);
  }

  @Test
  @DisplayName("Ao falhar criticamente, marca job como FALHA e propaga exceção")
  void deveMarcarJobComoFalhaEmErroCritico() {
    var job = ImportacaoJob.iniciar("abc123");
    when(jobRepository.findById("abc123")).thenReturn(Optional.of(job));

    var csvVazio = "";
    assertThatThrownBy(() -> consumer.processar(new ImportacaoMessage("abc123", csvVazio)))
        .isInstanceOf(RuntimeException.class);

    var captor = ArgumentCaptor.forClass(ImportacaoJob.class);
    verify(jobRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(ImportacaoStatus.FALHA);
  }
}
