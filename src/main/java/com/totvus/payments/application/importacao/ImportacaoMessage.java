package com.totvus.payments.application.importacao;

public record ImportacaoMessage(String protocolo, String csvContent) {}