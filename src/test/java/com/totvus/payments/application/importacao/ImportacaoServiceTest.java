package com.totvus.payments.application.importacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.ImportacaoJob;
import com.totvus.payments.domain.model.ImportacaoStatus;
import com.totvus.payments.infrastructure.messaging.ImportacaoProducer;
import com.totvus.payments.infrastructure.persistence.ImportacaoJobJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImportacaoServiceTest {

  @Mock private ImportacaoProducer producer;
  @Mock private ImportacaoJobJpaRepository jobRepository;
  @InjectMocks private ImportacaoService service;

  @Test
  @DisplayName("Ao importar, cria job com status PROCESSANDO e publica mensagem")
  void deveCriarJobAoImportar() {
    var arquivo =
        new MockMultipartFile("arquivo", "contas.csv", "text/csv", "cabecalho\n".getBytes());

    var response = service.importar(arquivo);

    assertThat(response.status()).isEqualTo("PROCESSANDO");
    verify(jobRepository)
        .save(
            argThat(
                job ->
                    job.getProtocolo().equals(response.protocolo())
                        && job.getStatus() == ImportacaoStatus.PROCESSANDO));
    verify(producer).publicar(argThat(msg -> msg.protocolo().equals(response.protocolo())));
  }

  @Test
  @DisplayName("Consulta status retorna dados do job existente")
  void deveConsultarStatusDoJobExistente() {
    var job = ImportacaoJob.iniciar("abc123");
    job.concluir(3, 1);
    when(jobRepository.findById("abc123")).thenReturn(Optional.of(job));

    var status = service.consultarStatus("abc123");

    assertThat(status.protocolo()).isEqualTo("abc123");
    assertThat(status.status()).isEqualTo(ImportacaoStatus.CONCLUIDO_COM_ERROS);
    assertThat(status.totalSucesso()).isEqualTo(3);
    assertThat(status.totalErros()).isEqualTo(1);
  }

  @Test
  @DisplayName("Consulta status lança exceção quando protocolo não existe")
  void deveLancarExcecaoQuandoProtocoloNaoExiste() {
    when(jobRepository.findById("inexistente")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.consultarStatus("inexistente"))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessageContaining("inexistente");
  }
}
