package com.totvus.payments.application.importacao;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.ImportacaoJob;
import com.totvus.payments.infrastructure.messaging.ImportacaoProducer;
import com.totvus.payments.infrastructure.persistence.ImportacaoJobJpaRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportacaoService {

  private final ImportacaoProducer producer;
  private final ImportacaoJobJpaRepository jobRepository;

  public ImportacaoService(ImportacaoProducer producer, ImportacaoJobJpaRepository jobRepository) {
    this.producer = producer;
    this.jobRepository = jobRepository;
  }

  public ImportacaoResponse importar(MultipartFile arquivo) {
    if (arquivo.isEmpty()) {
      throw new DomainException("Arquivo CSV não pode estar vazio");
    }
    String nomeArquivo = arquivo.getOriginalFilename();
    if (nomeArquivo == null || !nomeArquivo.endsWith(".csv")) {
      throw new DomainException("Apenas arquivos .csv são aceitos");
    }
    try {
      String csvContent = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
      String protocolo = UUID.randomUUID().toString();
      jobRepository.save(ImportacaoJob.iniciar(protocolo));
      producer.publicar(new ImportacaoMessage(protocolo, csvContent));
      return new ImportacaoResponse(
          protocolo,
          "PROCESSANDO",
          "Arquivo recebido. Acompanhe o processamento em GET /api/importacao/" + protocolo);
    } catch (IOException e) {
      throw new DomainException("Erro ao ler o arquivo CSV");
    }
  }

  public ImportacaoStatusResponse consultarStatus(String protocolo) {
    return jobRepository
        .findById(protocolo)
        .map(ImportacaoStatusResponse::from)
        .orElseThrow(
            () ->
                new RecursoNaoEncontradoException(
                    "Protocolo de importação não encontrado: " + protocolo));
  }
}
