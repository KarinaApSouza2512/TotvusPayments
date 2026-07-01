package com.totvus.payments.web;

import com.totvus.payments.application.fornecedor.FornecedorRequest;
import com.totvus.payments.application.fornecedor.FornecedorResponse;
import com.totvus.payments.application.fornecedor.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
@Tag(name = "Fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar fornecedor")
    public FornecedorResponse criar(@Valid @RequestBody FornecedorRequest request) {
        return service.criar(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar fornecedor por ID")
    public FornecedorResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    @Operation(summary = "Listar fornecedores")
    public List<FornecedorResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar fornecedor")
    public FornecedorResponse atualizar(@PathVariable Long id,
                                        @Valid @RequestBody FornecedorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletar fornecedor")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}