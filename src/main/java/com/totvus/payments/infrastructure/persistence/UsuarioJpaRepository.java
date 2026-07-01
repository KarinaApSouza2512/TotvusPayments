package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}