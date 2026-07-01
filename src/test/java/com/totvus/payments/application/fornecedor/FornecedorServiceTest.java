package com.totvus.payments.application.fornecedor;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock private FornecedorJpaRepository repository;
    @InjectMocks private FornecedorService service;

    @Test
    @DisplayName("Cria fornecedor com sucesso")
    void deveCriarFornecedorComSucesso() {
        var fornecedor = new Fornecedor("Acme Corp");
        when(repository.save(any())).thenReturn(fornecedor);

        var response = service.criar(new FornecedorRequest("Acme Corp"));

        assertThat(response.nome()).isEqualTo("Acme Corp");
        verify(repository).save(any(Fornecedor.class));
    }

    @Test
    @DisplayName("Lança exceção ao buscar fornecedor inexistente")
    void deveLancarExcecaoSeFornecedorNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Lista todos os fornecedores")
    void deveListarFornecedores() {
        when(repository.findAll()).thenReturn(List.of(
            new Fornecedor("Acme"), new Fornecedor("Beta")));

        var lista = service.listar();

        assertThat(lista).hasSize(2);
    }

    @Test
    @DisplayName("Deleta fornecedor existente")
    void deveDeletarFornecedorExistente() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        assertThatCode(() -> service.deletar(1L)).doesNotThrowAnyException();
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Lança exceção ao deletar fornecedor inexistente")
    void deveLancarExcecaoAoDeletarFornecedorInexistente() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deletar(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
        verify(repository, never()).deleteById(any());
    }
}
