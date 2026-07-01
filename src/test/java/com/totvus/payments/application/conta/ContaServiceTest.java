package com.totvus.payments.application.conta;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.domain.model.SituacaoConta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock private ContaJpaRepository contaRepository;
    @Mock private FornecedorJpaRepository fornecedorRepository;
    @InjectMocks private ContaService service;

    private final Fornecedor fornecedor = new Fornecedor("Acme Corp");

    @Test
    @DisplayName("Cria conta com sucesso quando fornecedor existe")
    void deveCriarContaComSucesso() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(30),
            new BigDecimal("1000.00"), "Serviço");
        when(contaRepository.save(any())).thenReturn(conta);

        var request = new ContaRequest(1L, LocalDate.now().plusDays(30),
            new BigDecimal("1000.00"), "Serviço");
        var response = service.criar(request);

        assertThat(response.valor()).isEqualByComparingTo("1000.00");
        assertThat(response.situacao()).isEqualTo(SituacaoConta.PENDENTE);
        verify(contaRepository).save(any(Conta.class));
    }

    @Test
    @DisplayName("Lança exceção quando fornecedor não existe ao criar conta")
    void deveLancarExcecaoSeFornecedorNaoExistir() {
        when(fornecedorRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new ContaRequest(99L, LocalDate.now().plusDays(30),
            new BigDecimal("500.00"), "Serviço");

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
        verify(contaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lista contas com filtros retorna página correta")
    void deveListarContasComFiltros() {
        var conta = Conta.criar(fornecedor, LocalDate.now(), new BigDecimal("200.00"), "Aluguel");
        var page = new PageImpl<>(List.of(conta));
        when(contaRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(page);

        var resultado = service.listar("aluguel", null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).descricao()).isEqualTo("Aluguel");
    }

    @Test
    @DisplayName("Calcula total pago por período")
    void deveCalcularTotalPagoPorPeriodo() {
        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fim = LocalDate.of(2024, 12, 31);
        when(contaRepository.calcularTotalPago(inicio, fim)).thenReturn(new BigDecimal("5000.00"));

        var response = service.totalPagoPorPeriodo(inicio, fim);

        assertThat(response.totalPago()).isEqualByComparingTo("5000.00");
        assertThat(response.inicio()).isEqualTo(inicio);
        assertThat(response.fim()).isEqualTo(fim);
    }

    @Test
    @DisplayName("Altera situação de PENDENTE para PAGO com sucesso")
    void deveAlterarSituacaoComSucesso() {
        var id = UUID.randomUUID();
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("300.00"), "Manutenção");
        when(contaRepository.findById(id)).thenReturn(Optional.of(conta));
        when(contaRepository.save(any())).thenReturn(conta);

        var response = service.alterarSituacao(id, new SituacaoRequest(SituacaoConta.PAGO));

        assertThat(response.situacao()).isEqualTo(SituacaoConta.PAGO);
        verify(contaRepository).save(conta);
    }

    @Test
    @DisplayName("Lança exceção ao buscar conta inexistente")
    void deveLancarExcecaoAoBuscarContaInexistente() {
        var id = UUID.randomUUID();
        when(contaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("Conta");
    }
}
