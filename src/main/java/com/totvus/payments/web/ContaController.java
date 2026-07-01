package com.totvus.payments.web;

import com.totvus.payments.application.conta.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/contas")
@Tag(name = "Contas a Pagar")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar conta")
    public ContaResponse criar(@Valid @RequestBody ContaRequest request) {
        return service.criar(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar conta por ID")
    public ContaResponse buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    @Operation(summary = "Listar contas com filtros e paginação")
    public Page<ContaResponse> listar(
        @RequestParam(required = false) String descricao,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimentoInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimentoFim,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return service.listar(descricao, dataVencimentoInicio, dataVencimentoFim, pageable);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar conta")
    public ContaResponse atualizar(@PathVariable UUID id,
                                   @Valid @RequestBody ContaRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(summary = "Alterar situação da conta")
    public ContaResponse alterarSituacao(@PathVariable UUID id,
                                         @Valid @RequestBody SituacaoRequest request) {
        return service.alterarSituacao(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletar conta")
    public void deletar(@PathVariable UUID id) {
        service.deletar(id);
    }

    @GetMapping("/relatorio/total-pago")
    @Operation(summary = "Total pago por período")
    public TotalPagoResponse totalPagoPorPeriodo(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return service.totalPagoPorPeriodo(inicio, fim);
    }
}