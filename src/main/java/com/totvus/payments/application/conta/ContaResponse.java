package com.totvus.payments.application.conta;

import com.totvus.payments.application.fornecedor.FornecedorResponse;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.SituacaoConta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContaResponse(
    UUID id,
    LocalDate dataVencimento,
    LocalDate dataPagamento,
    BigDecimal valor,
    String descricao,
    SituacaoConta situacao,
    FornecedorResponse fornecedor
) {
    public static ContaResponse from(Conta conta) {
        return new ContaResponse(
            conta.getId(),
            conta.getDataVencimento(),
            conta.getDataPagamento(),
            conta.getValor(),
            conta.getDescricao(),
            conta.getSituacao(),
            FornecedorResponse.from(conta.getFornecedor())
        );
    }
}