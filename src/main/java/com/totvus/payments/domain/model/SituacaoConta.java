package com.totvus.payments.domain.model;

public enum SituacaoConta {
  PENDENTE {
    @Override
    public boolean podeTransicionarPara(SituacaoConta destino) {
      return destino == PAGO || destino == CANCELADO;
    }
  },
  PAGO {
    @Override
    public boolean podeTransicionarPara(SituacaoConta destino) {
      return false;
    }
  },
  CANCELADO {
    @Override
    public boolean podeTransicionarPara(SituacaoConta destino) {
      return false;
    }
  };

  public abstract boolean podeTransicionarPara(SituacaoConta destino);
}
