package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Conta;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ContaSpecification {

  private ContaSpecification() {}

  public static Specification<Conta> comDescricao(String descricao) {
    return (root, query, cb) -> {
      if (descricao == null || descricao.isBlank()) return null;
      return cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%");
    };
  }

  public static Specification<Conta> comDataVencimentoEntre(LocalDate inicio, LocalDate fim) {
    return (root, query, cb) -> {
      if (inicio == null && fim == null) return null;
      if (inicio == null) return cb.lessThanOrEqualTo(root.get("dataVencimento"), fim);
      if (fim == null) return cb.greaterThanOrEqualTo(root.get("dataVencimento"), inicio);
      return cb.between(root.get("dataVencimento"), inicio, fim);
    };
  }
}
