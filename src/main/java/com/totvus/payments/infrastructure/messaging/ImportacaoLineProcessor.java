package com.totvus.payments.infrastructure.messaging;

import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.SituacaoConta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class ImportacaoLineProcessor {

    private final ContaJpaRepository contaRepository;
    private final FornecedorJpaRepository fornecedorRepository;

    public ImportacaoLineProcessor(ContaJpaRepository contaRepository,
                                   FornecedorJpaRepository fornecedorRepository) {
        this.contaRepository = contaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processarLinha(String linha) {
        String[] campos = linha.split(",", -1);
        if (campos.length < 6) {
            throw new IllegalArgumentException("Número de colunas inválido: " + campos.length + " (esperado 6)");
        }

        Long fornecedorId = Long.parseLong(campos[0].trim());
        LocalDate dataVencimento = LocalDate.parse(campos[1].trim());
        BigDecimal valor = new BigDecimal(campos[3].trim());
        String descricao = campos[4].trim();
        SituacaoConta situacao = SituacaoConta.valueOf(campos[5].trim().toUpperCase());

        var fornecedor = fornecedorRepository.findById(fornecedorId)
            .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + fornecedorId));

        var conta = Conta.criar(fornecedor, dataVencimento, valor, descricao);

        if (situacao != SituacaoConta.PENDENTE) {
            conta.alterarSituacao(situacao);
        }

        contaRepository.save(conta);
    }
}
