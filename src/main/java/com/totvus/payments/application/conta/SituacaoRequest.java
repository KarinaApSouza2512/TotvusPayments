package com.totvus.payments.application.conta;

import com.totvus.payments.domain.model.SituacaoConta;
import jakarta.validation.constraints.NotNull;

public record SituacaoRequest(
    @NotNull(message = "Situação é obrigatória") SituacaoConta situacao) {}
