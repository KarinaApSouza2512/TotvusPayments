package com.totvus.payments.application.fornecedor;

import com.totvus.payments.domain.model.Fornecedor;

public record FornecedorResponse(Long id, String nome) {

  public static FornecedorResponse from(Fornecedor fornecedor) {
    return new FornecedorResponse(fornecedor.getId(), fornecedor.getNome());
  }
}
