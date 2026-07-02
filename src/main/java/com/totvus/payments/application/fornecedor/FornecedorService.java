package com.totvus.payments.application.fornecedor;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FornecedorService {

  private final FornecedorJpaRepository repository;

  public FornecedorService(FornecedorJpaRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public FornecedorResponse criar(FornecedorRequest request) {
    var fornecedor = new Fornecedor(request.nome());
    return FornecedorResponse.from(repository.save(fornecedor));
  }

  @Transactional(readOnly = true)
  public FornecedorResponse buscarPorId(Long id) {
    return repository
        .findById(id)
        .map(FornecedorResponse::from)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
  }

  @Transactional(readOnly = true)
  public List<FornecedorResponse> listar() {
    return repository.findAll().stream().map(FornecedorResponse::from).toList();
  }

  @Transactional
  public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
    var fornecedor =
        repository
            .findById(id)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    fornecedor.atualizarNome(request.nome());
    return FornecedorResponse.from(repository.save(fornecedor));
  }

  @Transactional
  public void deletar(Long id) {
    if (!repository.existsById(id)) {
      throw new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id);
    }
    repository.deleteById(id);
  }
}
