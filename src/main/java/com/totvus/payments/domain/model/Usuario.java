package com.totvus.payments.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false, length = 50)
    private String role;

    protected Usuario() {}

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getRole() { return role; }
}