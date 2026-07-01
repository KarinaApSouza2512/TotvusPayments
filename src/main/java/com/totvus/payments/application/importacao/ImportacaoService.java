package com.totvus.payments.application.importacao;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.infrastructure.messaging.ImportacaoProducer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ImportacaoService {

    private final ImportacaoProducer producer;

    public ImportacaoService(ImportacaoProducer producer) {
        this.producer = producer;
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
            producer.publicar(new ImportacaoMessage(protocolo, csvContent));
            return new ImportacaoResponse(protocolo, "PROCESSANDO",
                "Arquivo recebido. Acompanhe o processamento pelo protocolo.");
        } catch (IOException e) {
            throw new DomainException("Erro ao ler o arquivo CSV");
        }
    }
}