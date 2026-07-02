package com.totvus.payments.infrastructure.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.domain.model.SituacaoConta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportacaoLineProcessorTest {

  @Mock private ContaJpaRepository contaRepository;
  @Mock private FornecedorJpaRepository fornecedorRepository;
  @InjectMocks private ImportacaoLineProcessor processor;

  private final Fornecedor fornecedor = new Fornecedor("Acme Corp");

  @Test
  @DisplayName("Linha PAGO usa a dataPagamento informada no CSV, não a data atual")
  void deveUsarDataPagamentoDoCsvQuandoSituacaoPago() {
    when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
    when(contaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    processor.processarLinha("1,2024-02-01,2024-01-30,1500.00,Fornecimento mensal,PAGO");

    var captor = ArgumentCaptor.forClass(Conta.class);
    verify(contaRepository).save(captor.capture());
    var conta = captor.getValue();
    assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.PAGO);
    assertThat(conta.getDataPagamento()).isEqualTo(LocalDate.of(2024, 1, 30));
  }
}
