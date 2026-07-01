package com.totvus.payments.domain.model;

import com.totvus.payments.domain.exception.DomainException;
import jakarta.persistence.*;

@Entity
@Table(name = "fornecedores")
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    protected Fornecedor() {}

    public Fornecedor(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do fornecedor é obrigatório");
        }
        this.nome = nome;
    }

    public void atualizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do fornecedor é obrigatório");
        }
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}