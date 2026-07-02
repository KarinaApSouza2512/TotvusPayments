package com.totvus.payments.application.importacao;

import com.totvus.payments.domain.model.ImportacaoJob;
import com.totvus.payments.domain.model.ImportacaoStatus;
import java.time.LocalDateTime;

public record ImportacaoStatusResponse(
    String protocolo,
    ImportacaoStatus status,
    int totalLinhas,
    int totalSucesso,
    int totalErros,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm) {

  public static ImportacaoStatusResponse from(ImportacaoJob job) {
    return new ImportacaoStatusResponse(
        job.getProtocolo(),
        job.getStatus(),
        job.getTotalLinhas(),
        job.getTotalSucesso(),
        job.getTotalErros(),
        job.getCriadoEm(),
        job.getAtualizadoEm());
  }
}
