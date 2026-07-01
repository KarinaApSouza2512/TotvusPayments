package com.totvus.payments.application.conta;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.ContaSpecification;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ContaService {

    private final ContaJpaRepository contaRepository;
    private final FornecedorJpaRepository fornecedorRepository;

    public ContaService(ContaJpaRepository contaRepository,
                        FornecedorJpaRepository fornecedorRepository) {
        this.contaRepository = contaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public ContaResponse criar(ContaRequest request) {
        var fornecedor = fornecedorRepository.findById(request.fornecedorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Fornecedor não encontrado: " + request.fornecedorId()));
        var conta = Conta.criar(fornecedor, request.dataVencimento(),
                                request.valor(), request.descricao());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional(readOnly = true)
    public ContaResponse buscarPorId(UUID id) {
        return contaRepository.findById(id)
            .map(ContaResponse::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ContaResponse> listar(String descricao, LocalDate dataVencimentoInicio,
                                      LocalDate dataVencimentoFim, Pageable pageable) {
        Specification<Conta> spec = Specification
            .where(ContaSpecification.comDescricao(descricao))
            .and(ContaSpecification.comDataVencimentoEntre(dataVencimentoInicio, dataVencimentoFim));
        return contaRepository.findAll(spec, pageable).map(ContaResponse::from);
    }

    @Transactional
    public ContaResponse atualizar(UUID id, ContaRequest request) {
        var conta = contaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
        conta.atualizar(request.dataVencimento(), request.valor(), request.descricao());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional
    public ContaResponse alterarSituacao(UUID id, SituacaoRequest request) {
        var conta = contaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
        conta.alterarSituacao(request.situacao());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional
    public void deletar(UUID id) {
        if (!contaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Conta não encontrada: " + id);
        }
        contaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TotalPagoResponse totalPagoPorPeriodo(LocalDate inicio, LocalDate fim) {
        var total = contaRepository.calcularTotalPago(inicio, fim);
        return new TotalPagoResponse(inicio, fim, total);
    }
}