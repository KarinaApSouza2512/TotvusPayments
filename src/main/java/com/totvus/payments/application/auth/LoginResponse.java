package com.totvus.payments.application.auth;

public record LoginResponse(String token, String tipo) {
    public LoginResponse(String token) {
        this(token, "Bearer");
    }
}