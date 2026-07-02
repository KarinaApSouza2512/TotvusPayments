package com.totvus.payments.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "importacao_jobs")
public class ImportacaoJob {

  @Id
  @Column(length = 36)
  private String protocolo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ImportacaoStatus status;

  @Column(name = "total_linhas", nullable = false)
  private int totalLinhas;

  @Column(name = "total_sucesso", nullable = false)
  private int totalSucesso;

  @Column(name = "total_erros", nullable = false)
  private int totalErros;

  @Column(name = "criado_em", nullable = false)
  private LocalDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private LocalDateTime atualizadoEm;

  protected ImportacaoJob() {}

  public static ImportacaoJob iniciar(String protocolo) {
    var job = new ImportacaoJob();
    job.protocolo = protocolo;
    job.status = ImportacaoStatus.PROCESSANDO;
    job.criadoEm = LocalDateTime.now();
    job.atualizadoEm = job.criadoEm;
    return job;
  }

  public void concluir(int totalSucesso, int totalErros) {
    this.totalSucesso = totalSucesso;
    this.totalErros = totalErros;
    this.totalLinhas = totalSucesso + totalErros;
    this.status =
        totalErros == 0 ? ImportacaoStatus.CONCLUIDO : ImportacaoStatus.CONCLUIDO_COM_ERROS;
    this.atualizadoEm = LocalDateTime.now();
  }

  public void falhar() {
    this.status = ImportacaoStatus.FALHA;
    this.atualizadoEm = LocalDateTime.now();
  }

  public String getProtocolo() {
    return protocolo;
  }

  public ImportacaoStatus getStatus() {
    return status;
  }

  public int getTotalLinhas() {
    return totalLinhas;
  }

  public int getTotalSucesso() {
    return totalSucesso;
  }

  public int getTotalErros() {
    return totalErros;
  }

  public LocalDateTime getCriadoEm() {
    return criadoEm;
  }

  public LocalDateTime getAtualizadoEm() {
    return atualizadoEm;
  }
}
