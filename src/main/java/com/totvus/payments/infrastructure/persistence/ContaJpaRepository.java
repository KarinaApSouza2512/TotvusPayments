package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Conta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ContaJpaRepository extends JpaRepository<Conta, UUID>, JpaSpecificationExecutor<Conta> {

    @Override
    @EntityGraph(attributePaths = {"fornecedor"})
    Page<Conta> findAll(Specification<Conta> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"fornecedor"})
    Optional<Conta> findById(UUID id);

    @Query("""
        SELECT COALESCE(SUM(c.valor), 0)
        FROM Conta c
        WHERE c.situacao = 'PAGO'
          AND c.dataPagamento BETWEEN :inicio AND :fim
        """)
    BigDecimal calcularTotalPago(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
