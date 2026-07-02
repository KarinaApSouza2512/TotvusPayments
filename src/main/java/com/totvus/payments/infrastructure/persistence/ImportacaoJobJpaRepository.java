package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.ImportacaoJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoJobJpaRepository extends JpaRepository<ImportacaoJob, String> {}
