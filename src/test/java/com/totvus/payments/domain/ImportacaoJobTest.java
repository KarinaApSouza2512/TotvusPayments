package com.totvus.payments.domain;

import static org.assertj.core.api.Assertions.*;

import com.totvus.payments.domain.model.ImportacaoJob;
import com.totvus.payments.domain.model.ImportacaoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImportacaoJobTest {

  @Test
  @DisplayName("Inicia job com status PROCESSANDO")
  void deveIniciarComStatusProcessando() {
    var job = ImportacaoJob.iniciar("abc123");

    assertThat(job.getProtocolo()).isEqualTo("abc123");
    assertThat(job.getStatus()).isEqualTo(ImportacaoStatus.PROCESSANDO);
  }

  @Test
  @DisplayName("Concluir sem erros marca status CONCLUIDO")
  void deveConcluirSemErros() {
    var job = ImportacaoJob.iniciar("abc123");

    job.concluir(5, 0);

    assertThat(job.getStatus()).isEqualTo(ImportacaoStatus.CONCLUIDO);
    assertThat(job.getTotalLinhas()).isEqualTo(5);
    assertThat(job.getTotalSucesso()).isEqualTo(5);
    assertThat(job.getTotalErros()).isZero();
  }

  @Test
  @DisplayName("Concluir com erros marca status CONCLUIDO_COM_ERROS")
  void deveConcluirComErros() {
    var job = ImportacaoJob.iniciar("abc123");

    job.concluir(3, 2);

    assertThat(job.getStatus()).isEqualTo(ImportacaoStatus.CONCLUIDO_COM_ERROS);
    assertThat(job.getTotalLinhas()).isEqualTo(5);
    assertThat(job.getTotalErros()).isEqualTo(2);
  }

  @Test
  @DisplayName("Falha crítica marca status FALHA")
  void deveMarcarFalhaCritica() {
    var job = ImportacaoJob.iniciar("abc123");

    job.falhar();

    assertThat(job.getStatus()).isEqualTo(ImportacaoStatus.FALHA);
  }
}
