package com.totvus.payments.web;

import com.totvus.payments.application.auth.AuthService;
import com.totvus.payments.application.auth.LoginRequest;
import com.totvus.payments.application.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @Operation(summary = "Autenticar e obter JWT")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
