package com.totvus.payments.application.conta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TotalPagoResponse(LocalDate inicio, LocalDate fim, BigDecimal totalPago) {}
