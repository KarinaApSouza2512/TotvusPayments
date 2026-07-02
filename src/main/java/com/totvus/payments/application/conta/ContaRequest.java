package com.totvus.payments.application.conta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaRequest(
    @NotNull(message = "Fornecedor é obrigatório") Long fornecedorId,
    @NotNull(message = "Data de vencimento é obrigatória") LocalDate dataVencimento,
    @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser positivo")
        BigDecimal valor,
    @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao) {}
