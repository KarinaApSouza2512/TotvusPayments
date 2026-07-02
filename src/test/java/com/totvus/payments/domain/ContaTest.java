package com.totvus.payments.domain;

import static org.assertj.core.api.Assertions.*;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.domain.exception.TransicaoEstadoInvalidaException;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.domain.model.SituacaoConta;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContaTest {

  private Fornecedor fornecedor;

  @BeforeEach
  void setUp() {
    fornecedor = new Fornecedor("Acme Corp");
  }

  @Test
  @DisplayName("Cria conta com situação PENDENTE por padrão")
  void deveCriarContaComSituacaoPendente() {
    var conta =
        Conta.criar(
            fornecedor, LocalDate.now().plusDays(30), new BigDecimal("1000.00"), "Serviço de TI");
    assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.PENDENTE);
    assertThat(conta.getDataPagamento()).isNull();
  }

  @Test
  @DisplayName("Transição PENDENTE → PAGO é permitida e seta data de pagamento")
  void devePermitirTransicaoPendenteParaPago() {
    var conta =
        Conta.criar(
            fornecedor,
            LocalDate.now().plusDays(30),
            new BigDecimal("500.00"),
            "Material de escritório");
    conta.alterarSituacao(SituacaoConta.PAGO);
    assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.PAGO);
    assertThat(conta.getDataPagamento()).isEqualTo(LocalDate.now());
  }

  @Test
  @DisplayName("Transição PENDENTE → CANCELADO é permitida")
  void devePermitirTransicaoPendenteParaCancelado() {
    var conta =
        Conta.criar(
            fornecedor, LocalDate.now().plusDays(10), new BigDecimal("200.00"), "Consultoria");
    conta.alterarSituacao(SituacaoConta.CANCELADO);
    assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.CANCELADO);
  }

  @Test
  @DisplayName("Transição para PAGO com data explícita usa a data informada, não a data atual")
  void devePermitirTransicaoParaPagoComDataExplicita() {
    var conta =
        Conta.criar(fornecedor, LocalDate.now().plusDays(10), new BigDecimal("300.00"), "Frete");
    var dataPagamentoHistorica = LocalDate.of(2024, 12, 20);

    conta.alterarSituacao(SituacaoConta.PAGO, dataPagamentoHistorica);

    assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.PAGO);
    assertThat(conta.getDataPagamento()).isEqualTo(dataPagamentoHistorica);
  }

  @Test
  @DisplayName("PAGO não pode voltar para PENDENTE")
  void deveLancarExcecaoAoTentarVoltarDePagoParaPendente() {
    var conta =
        Conta.criar(
            fornecedor, LocalDate.now().plusDays(10), new BigDecimal("300.00"), "Manutenção");
    conta.alterarSituacao(SituacaoConta.PAGO);
    assertThatThrownBy(() -> conta.alterarSituacao(SituacaoConta.PENDENTE))
        .isInstanceOf(TransicaoEstadoInvalidaException.class)
        .hasMessageContaining("PAGO")
        .hasMessageContaining("PENDENTE");
  }

  @Test
  @DisplayName("PAGO não pode ser CANCELADO")
  void deveLancarExcecaoAoTentarCancelarContaPaga() {
    var conta =
        Conta.criar(fornecedor, LocalDate.now().plusDays(10), new BigDecimal("300.00"), "Aluguel");
    conta.alterarSituacao(SituacaoConta.PAGO);
    assertThatThrownBy(() -> conta.alterarSituacao(SituacaoConta.CANCELADO))
        .isInstanceOf(TransicaoEstadoInvalidaException.class)
        .hasMessageContaining("PAGO")
        .hasMessageContaining("CANCELADO");
  }

  @Test
  @DisplayName("CANCELADO é estado final — não aceita transições")
  void deveLancarExcecaoAoTentarMudarEstadoDeCancelado() {
    var conta =
        Conta.criar(fornecedor, LocalDate.now().plusDays(10), new BigDecimal("100.00"), "Frete");
    conta.alterarSituacao(SituacaoConta.CANCELADO);
    assertThatThrownBy(() -> conta.alterarSituacao(SituacaoConta.PAGO))
        .isInstanceOf(TransicaoEstadoInvalidaException.class)
        .hasMessageContaining("CANCELADO");
  }

  @Test
  @DisplayName("Valor zero lança DomainException")
  void deveLancarExcecaoParaValorZero() {
    assertThatThrownBy(
            () ->
                Conta.criar(fornecedor, LocalDate.now().plusDays(10), BigDecimal.ZERO, "Descrição"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("positivo");
  }

  @Test
  @DisplayName("Valor negativo lança DomainException")
  void deveLancarExcecaoParaValorNegativo() {
    assertThatThrownBy(
            () ->
                Conta.criar(
                    fornecedor, LocalDate.now().plusDays(10), new BigDecimal("-1"), "Descrição"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("positivo");
  }

  @Test
  @DisplayName("Descrição em branco lança DomainException")
  void deveLancarExcecaoParaDescricaoEmBranco() {
    assertThatThrownBy(
            () ->
                Conta.criar(
                    fornecedor, LocalDate.now().plusDays(10), new BigDecimal("100.00"), "  "))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("descrição");
  }

  @Test
  @DisplayName("Data de vencimento nula lança DomainException")
  void deveLancarExcecaoParaDataVencimentoNula() {
    assertThatThrownBy(() -> Conta.criar(fornecedor, null, new BigDecimal("100.00"), "Descrição"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("vencimento");
  }
}
