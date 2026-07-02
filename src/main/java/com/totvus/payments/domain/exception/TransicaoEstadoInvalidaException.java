package com.totvus.payments.domain.exception;

import com.totvus.payments.domain.model.SituacaoConta;

public class TransicaoEstadoInvalidaException extends DomainException {
  public TransicaoEstadoInvalidaException(SituacaoConta de, SituacaoConta para) {
    super("Transição de estado inválida: " + de + " → " + para);
  }
}
