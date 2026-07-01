package com.totvus.payments.infrastructure.messaging;

import com.totvus.payments.application.importacao.ImportacaoMessage;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.SituacaoConta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ImportacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoConsumer.class);

    private final ContaJpaRepository contaRepository;
    private final FornecedorJpaRepository fornecedorRepository;

    public ImportacaoConsumer(ContaJpaRepository contaRepository,
                               FornecedorJpaRepository fornecedorRepository) {
        this.contaRepository = contaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_IMPORTACAO)
    @Transactional
    public void processar(ImportacaoMessage message) {
        log.info("Iniciando importação. Protocolo: {}", message.protocolo());
        var reader = new BufferedReader(new StringReader(message.csvContent()));
        var contador = new AtomicInteger(0);
        var erros = new AtomicInteger(0);

        try {
            // Pular linha de cabeçalho
            String cabecalho = reader.readLine();
            if (cabecalho == null) {
                throw new IllegalArgumentException("CSV vazio ou sem cabeçalho");
            }

            String linha;
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                String linhaAtual = linha;
                int numAtual = numeroLinha;
                try {
                    processarLinha(linhaAtual);
                    contador.incrementAndGet();
                } catch (Exception e) {
                    erros.incrementAndGet();
                    log.warn("Protocolo {}: erro na linha {} — {} | Linha: '{}'",
                        message.protocolo(), numAtual, e.getMessage(), linhaAtual);
                }
            }
        } catch (Exception e) {
            log.error("Protocolo {}: falha crítica ao processar CSV — {}", message.protocolo(), e.getMessage());
            throw new RuntimeException("Falha crítica no processamento do CSV", e);
        }

        log.info("Importação concluída. Protocolo: {} | Sucesso: {} | Erros: {}",
            message.protocolo(), contador.get(), erros.get());
    }

    private void processarLinha(String linha) {
        String[] campos = linha.split(",", -1);
        if (campos.length < 6) {
            throw new IllegalArgumentException("Número de colunas inválido: " + campos.length + " (esperado 6)");
        }

        Long fornecedorId = Long.parseLong(campos[0].trim());
        LocalDate dataVencimento = LocalDate.parse(campos[1].trim());
        LocalDate dataPagamento = campos[2].trim().isBlank() ? null : LocalDate.parse(campos[2].trim());
        BigDecimal valor = new BigDecimal(campos[3].trim());
        String descricao = campos[4].trim();
        SituacaoConta situacao = SituacaoConta.valueOf(campos[5].trim().toUpperCase());

        var fornecedor = fornecedorRepository.findById(fornecedorId)
            .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + fornecedorId));

        var conta = Conta.criar(fornecedor, dataVencimento, valor, descricao);

        if (situacao != SituacaoConta.PENDENTE) {
            conta.alterarSituacao(situacao);
        }
        if (dataPagamento != null && conta.getDataPagamento() == null) {
            // dataPagamento já é setada automaticamente ao marcar como PAGO
            // se vier explícita e não foi setada, é ignorada (campo calculado)
        }

        contaRepository.save(conta);
    }
}