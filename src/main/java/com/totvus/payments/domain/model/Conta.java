package com.totvus.payments.domain.model;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.domain.exception.TransicaoEstadoInvalidaException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class Conta {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fornecedor_id", nullable = false)
  private Fornecedor fornecedor;

  @Column(name = "data_vencimento", nullable = false)
  private LocalDate dataVencimento;

  @Column(name = "data_pagamento")
  private LocalDate dataPagamento;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false, length = 500)
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SituacaoConta situacao;

  protected Conta() {}

  public static Conta criar(
      Fornecedor fornecedor, LocalDate dataVencimento, BigDecimal valor, String descricao) {
    validarValor(valor);
    validarDescricao(descricao);
    if (dataVencimento == null) {
      throw new DomainException("Data de vencimento é obrigatória");
    }
    var conta = new Conta();
    conta.fornecedor = fornecedor;
    conta.dataVencimento = dataVencimento;
    conta.valor = valor;
    conta.descricao = descricao;
    conta.situacao = SituacaoConta.PENDENTE;
    return conta;
  }

  public void atualizar(LocalDate dataVencimento, BigDecimal valor, String descricao) {
    validarValor(valor);
    validarDescricao(descricao);
    if (dataVencimento == null) {
      throw new DomainException("Data de vencimento é obrigatória");
    }
    this.dataVencimento = dataVencimento;
    this.valor = valor;
    this.descricao = descricao;
  }

  public void alterarSituacao(SituacaoConta novaSituacao) {
    alterarSituacao(novaSituacao, null);
  }

  public void alterarSituacao(SituacaoConta novaSituacao, LocalDate dataPagamento) {
    if (!this.situacao.podeTransicionarPara(novaSituacao)) {
      throw new TransicaoEstadoInvalidaException(this.situacao, novaSituacao);
    }
    this.situacao = novaSituacao;
    if (novaSituacao == SituacaoConta.PAGO) {
      this.dataPagamento = dataPagamento != null ? dataPagamento : LocalDate.now();
    }
  }

  private static void validarValor(BigDecimal valor) {
    if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
      throw new DomainException("O valor da conta deve ser positivo");
    }
  }

  private static void validarDescricao(String descricao) {
    if (descricao == null || descricao.isBlank()) {
      throw new DomainException("A descrição é obrigatória");
    }
  }

  public UUID getId() {
    return id;
  }

  public Fornecedor getFornecedor() {
    return fornecedor;
  }

  public LocalDate getDataVencimento() {
    return dataVencimento;
  }

  public LocalDate getDataPagamento() {
    return dataPagamento;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public String getDescricao() {
    return descricao;
  }

  public SituacaoConta getSituacao() {
    return situacao;
  }
}
