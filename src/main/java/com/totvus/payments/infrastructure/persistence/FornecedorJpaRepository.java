package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorJpaRepository extends JpaRepository<Fornecedor, Long> {
}
