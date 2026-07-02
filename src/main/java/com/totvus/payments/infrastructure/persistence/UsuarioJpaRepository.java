package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmail(String email);
}
