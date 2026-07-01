# API de Gestão de Contas a Pagar — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir uma API REST completa para gestão de contas a pagar com importação assíncrona via CSV, autenticação JWT e arquitetura DDD.

**Architecture:** Camadas DDD (domain → application → infrastructure → web). O domínio encapsula as regras de negócio e a máquina de estados de `Conta`. O RabbitMQ desacopla o upload de CSV do processamento, com DLQ para resiliência. JWT protege todos os endpoints via filtro stateless.

**Tech Stack:** Java 17, Spring Boot 3.3.x, PostgreSQL 16, Flyway, Spring Data JPA + Specifications, Spring AMQP (RabbitMQ), Spring Security 6 + JJWT 0.12.x, springdoc-openapi 2.5.x, OpenCSV 5.9, Docker + Docker Compose, JUnit 5 + Mockito.

## Global Constraints

- Java 17: usar Records, Streams, Optional — sem tipos raw
- Spring Boot 3.3.x: namespace `jakarta.*`, não `javax.*`
- PostgreSQL 16 como banco de dados
- IDs de `Conta`: UUID; IDs de `Fornecedor`: Long (auto-increment)
- Flyway gerencia todo DDL — nunca alterar schema manualmente
- Datas: `LocalDate`; persistidas como `DATE` no PostgreSQL
- Valores monetários: `BigDecimal`
- Package base: `com.totvus.payments`
- Porta da aplicação: 8080
- Relacionamento `@ManyToOne` de Conta → Fornecedor sempre `FetchType.LAZY` + `@EntityGraph` nas queries que precisam carregar o relacionamento (previne N+1)

---

## Mapa de Arquivos

```
pom.xml
Dockerfile
docker-compose.yml
README.md
src/
├── main/
│   ├── java/com/totvus/payments/
│   │   ├── PaymentsApplication.java
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── SituacaoConta.java        ← enum com máquina de estados
│   │   │   │   ├── Fornecedor.java            ← entidade JPA
│   │   │   │   ├── Conta.java                 ← entidade JPA, encapsula invariantes
│   │   │   │   └── Usuario.java               ← entidade JPA, para auth
│   │   │   └── exception/
│   │   │       ├── DomainException.java
│   │   │       ├── RecursoNaoEncontradoException.java
│   │   │       └── TransicaoEstadoInvalidaException.java
│   │   ├── infrastructure/
│   │   │   ├── persistence/
│   │   │   │   ├── FornecedorJpaRepository.java
│   │   │   │   ├── ContaJpaRepository.java     ← @EntityGraph + query total pago
│   │   │   │   ├── ContaSpecification.java     ← filtros dinâmicos
│   │   │   │   └── UsuarioJpaRepository.java
│   │   │   ├── messaging/
│   │   │   │   ├── RabbitMQConfig.java         ← filas, DLQ, exchange
│   │   │   │   ├── ImportacaoProducer.java
│   │   │   │   └── ImportacaoConsumer.java     ← processa CSV, resiliência por linha
│   │   │   └── security/
│   │   │       ├── JwtService.java
│   │   │       ├── JwtAuthFilter.java
│   │   │       └── SecurityConfig.java
│   │   ├── application/
│   │   │   ├── fornecedor/
│   │   │   │   ├── FornecedorRequest.java      ← record
│   │   │   │   ├── FornecedorResponse.java     ← record com factory from(Fornecedor)
│   │   │   │   └── FornecedorService.java
│   │   │   ├── conta/
│   │   │   │   ├── ContaRequest.java           ← record
│   │   │   │   ├── ContaResponse.java          ← record com factory from(Conta)
│   │   │   │   ├── SituacaoRequest.java        ← record (apenas campo situacao)
│   │   │   │   ├── TotalPagoResponse.java      ← record
│   │   │   │   └── ContaService.java
│   │   │   ├── importacao/
│   │   │   │   ├── ImportacaoMessage.java      ← record (protocolo + csvContent)
│   │   │   │   ├── ImportacaoResponse.java     ← record
│   │   │   │   └── ImportacaoService.java
│   │   │   └── auth/
│   │   │       ├── LoginRequest.java           ← record
│   │   │       ├── LoginResponse.java          ← record
│   │   │       └── AuthService.java
│   │   └── web/
│   │       ├── FornecedorController.java
│   │       ├── ContaController.java
│   │       ├── ImportacaoController.java
│   │       ├── AuthController.java
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       ├── application.yml
│       └── db/migration/
│           ├── V1__create_fornecedor.sql
│           ├── V2__create_conta.sql
│           └── V3__create_usuario.sql
└── test/
    └── java/com/totvus/payments/
        ├── domain/
        │   └── ContaTest.java
        └── application/
            ├── conta/
            │   └── ContaServiceTest.java
            └── fornecedor/
                └── FornecedorServiceTest.java
```

---

## Task 1: Fundação do Projeto (pom.xml + Docker + application.yml)

**Files:**
- Create: `pom.xml`
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/totvus/payments/PaymentsApplication.java`

**Interfaces:**
- Produces: aplicação Spring Boot inicializável com `mvn spring-boot:run`, conectada ao PostgreSQL e RabbitMQ via Docker Compose

- [ ] **Step 1: Criar pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.totvus</groupId>
    <artifactId>payments</artifactId>
    <version>1.0.0</version>
    <name>payments</name>
    <description>API de Gestão de Contas a Pagar</description>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
        <springdoc.version>2.5.0</springdoc.version>
        <opencsv.version>5.9</opencsv.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>com.opencsv</groupId>
            <artifactId>opencsv</artifactId>
            <version>${opencsv.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Criar PaymentsApplication.java**

```java
package com.totvus.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }
}
```

- [ ] **Step 3: Criar application.yml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:payments}
    username: ${DB_USER:payments}
    password: ${DB_PASS:payments}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2.0

jwt:
  secret: ${JWT_SECRET:3cTr0nh4S3cr3tK3yF0rT0tvusP4ym3nts2024!!MinimumOf256BitsRequired}
  expiration-ms: 86400000

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 4: Criar Dockerfile**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/payments-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 5: Criar docker-compose.yml**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: payments-postgres
    environment:
      POSTGRES_DB: payments
      POSTGRES_USER: payments
      POSTGRES_PASSWORD: payments
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U payments"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: payments-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: payments-app
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      DB_NAME: payments
      DB_USER: payments
      DB_PASS: payments
      RABBITMQ_HOST: rabbitmq
      JWT_SECRET: 3cTr0nh4S3cr3tK3yF0rT0tvusP4ym3nts2024!!MinimumOf256BitsRequired
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

volumes:
  postgres_data:
```

- [ ] **Step 6: Verificar que o projeto compila**

```bash
mvn clean compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add pom.xml Dockerfile docker-compose.yml src/main/resources/application.yml src/main/java/com/totvus/payments/PaymentsApplication.java
git commit -m "feat: project foundation — Spring Boot 3.3 + PostgreSQL + RabbitMQ + Docker"
```

---

## Task 2: Migrações Flyway

**Files:**
- Create: `src/main/resources/db/migration/V1__create_fornecedor.sql`
- Create: `src/main/resources/db/migration/V2__create_conta.sql`
- Create: `src/main/resources/db/migration/V3__create_usuario.sql`

**Interfaces:**
- Produces: schema completo no PostgreSQL após `docker-compose up`

- [ ] **Step 1: Criar V1__create_fornecedor.sql**

```sql
CREATE TABLE fornecedores (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);
```

- [ ] **Step 2: Criar V2__create_conta.sql**

```sql
CREATE TABLE contas (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    fornecedor_id    BIGINT       NOT NULL REFERENCES fornecedores(id),
    data_vencimento  DATE         NOT NULL,
    data_pagamento   DATE,
    valor            NUMERIC(15, 2) NOT NULL,
    descricao        VARCHAR(500) NOT NULL,
    situacao         VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT chk_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_situacao CHECK (situacao IN ('PENDENTE', 'PAGO', 'CANCELADO'))
);

CREATE INDEX idx_contas_data_vencimento ON contas (data_vencimento);
CREATE INDEX idx_contas_situacao        ON contas (situacao);
CREATE INDEX idx_contas_fornecedor_id   ON contas (fornecedor_id);
```

- [ ] **Step 3: Criar V3__create_usuario.sql**

```sql
CREATE TABLE usuarios (
    id    BIGSERIAL    PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role  VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER'
);

-- Usuário padrão: admin@totvus.com / admin123
INSERT INTO usuarios (email, senha, role)
VALUES (
    'admin@totvus.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh32',
    'ROLE_ADMIN'
);
```

- [ ] **Step 4: Subir infraestrutura e validar as migrações**

```bash
docker-compose up postgres -d
# aguardar o healthcheck passar, depois:
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/payments \
    -Dflyway.user=payments -Dflyway.password=payments
```

Expected: `Successfully applied 3 migrations`

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/
git commit -m "feat: Flyway migrations for fornecedores, contas and usuarios tables"
```

---

## Task 3: Camada de Domínio (Entidades + Exceções)

**Files:**
- Create: `src/main/java/com/totvus/payments/domain/model/SituacaoConta.java`
- Create: `src/main/java/com/totvus/payments/domain/model/Fornecedor.java`
- Create: `src/main/java/com/totvus/payments/domain/model/Conta.java`
- Create: `src/main/java/com/totvus/payments/domain/model/Usuario.java`
- Create: `src/main/java/com/totvus/payments/domain/exception/DomainException.java`
- Create: `src/main/java/com/totvus/payments/domain/exception/RecursoNaoEncontradoException.java`
- Create: `src/main/java/com/totvus/payments/domain/exception/TransicaoEstadoInvalidaException.java`

**Interfaces:**
- Produces:
  - `SituacaoConta.podeTransicionarPara(SituacaoConta): boolean`
  - `Conta.criar(Fornecedor, LocalDate, BigDecimal, String): Conta`
  - `Conta.alterarSituacao(SituacaoConta): void` — lança `TransicaoEstadoInvalidaException` se inválido
  - `Conta.atualizar(LocalDate, BigDecimal, String): void` — lança `DomainException` se valores inválidos

- [ ] **Step 1: Criar SituacaoConta.java**

```java
package com.totvus.payments.domain.model;

public enum SituacaoConta {
    PENDENTE {
        @Override
        public boolean podeTransicionarPara(SituacaoConta destino) {
            return destino == PAGO || destino == CANCELADO;
        }
    },
    PAGO {
        @Override
        public boolean podeTransicionarPara(SituacaoConta destino) {
            return false;
        }
    },
    CANCELADO {
        @Override
        public boolean podeTransicionarPara(SituacaoConta destino) {
            return false;
        }
    };

    public abstract boolean podeTransicionarPara(SituacaoConta destino);
}
```

- [ ] **Step 2: Criar exceções de domínio**

`DomainException.java`:
```java
package com.totvus.payments.domain.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
```

`RecursoNaoEncontradoException.java`:
```java
package com.totvus.payments.domain.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
```

`TransicaoEstadoInvalidaException.java`:
```java
package com.totvus.payments.domain.exception;

public class TransicaoEstadoInvalidaException extends DomainException {
    public TransicaoEstadoInvalidaException(SituacaoConta de, SituacaoConta para) {
        super("Transição de estado inválida: " + de + " → " + para);
    }
}
```

Atenção: o construtor referencia `SituacaoConta` — ajustar o import:
```java
import com.totvus.payments.domain.model.SituacaoConta;
```

- [ ] **Step 3: Criar Fornecedor.java**

```java
package com.totvus.payments.domain.model;

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
            throw new com.totvus.payments.domain.exception.DomainException("Nome do fornecedor é obrigatório");
        }
        this.nome = nome;
    }

    public void atualizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new com.totvus.payments.domain.exception.DomainException("Nome do fornecedor é obrigatório");
        }
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
```

- [ ] **Step 4: Criar Conta.java**

```java
package com.totvus.payments.domain.model;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.domain.exception.TransicaoEstadoInvalidaException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoConta situacao;

    protected Conta() {}

    public static Conta criar(Fornecedor fornecedor, LocalDate dataVencimento,
                              BigDecimal valor, String descricao) {
        validarValor(valor);
        validarDescricao(descricao);
        if (dataVencimento == null) {
            throw new DomainException("Data de vencimento é obrigatória");
        }
        var conta = new Conta();
        conta.fornecedor = fornecedor;
        conta.dataVencimento = dataVencimento;
        conta.valor = valor;
        conta.descricao = descricao;
        conta.situacao = SituacaoConta.PENDENTE;
        return conta;
    }

    public void atualizar(LocalDate dataVencimento, BigDecimal valor, String descricao) {
        validarValor(valor);
        validarDescricao(descricao);
        if (dataVencimento == null) {
            throw new DomainException("Data de vencimento é obrigatória");
        }
        this.dataVencimento = dataVencimento;
        this.valor = valor;
        this.descricao = descricao;
    }

    public void alterarSituacao(SituacaoConta novaSituacao) {
        if (!this.situacao.podeTransicionarPara(novaSituacao)) {
            throw new TransicaoEstadoInvalidaException(this.situacao, novaSituacao);
        }
        this.situacao = novaSituacao;
        if (novaSituacao == SituacaoConta.PAGO && this.dataPagamento == null) {
            this.dataPagamento = LocalDate.now();
        }
    }

    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("O valor da conta deve ser positivo");
        }
    }

    private static void validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new DomainException("A descrição é obrigatória");
        }
    }

    public UUID getId() { return id; }
    public Fornecedor getFornecedor() { return fornecedor; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public BigDecimal getValor() { return valor; }
    public String getDescricao() { return descricao; }
    public SituacaoConta getSituacao() { return situacao; }
}
```

- [ ] **Step 5: Criar Usuario.java**

```java
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
```

- [ ] **Step 6: Compilar para verificar**

```bash
mvn clean compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/totvus/payments/domain/
git commit -m "feat: domain layer — entities Conta/Fornecedor/Usuario, state machine SituacaoConta, domain exceptions"
```

---

## Task 4: Repositórios JPA (Infrastructure)

**Files:**
- Create: `src/main/java/com/totvus/payments/infrastructure/persistence/FornecedorJpaRepository.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/persistence/ContaJpaRepository.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/persistence/ContaSpecification.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/persistence/UsuarioJpaRepository.java`

**Interfaces:**
- Produces:
  - `FornecedorJpaRepository` (extend `JpaRepository<Fornecedor, Long>`)
  - `ContaJpaRepository.findAll(Specification<Conta>, Pageable)` com `@EntityGraph` para prevenir N+1
  - `ContaJpaRepository.calcularTotalPago(LocalDate, LocalDate): BigDecimal`
  - `ContaSpecification.comDescricao(String): Specification<Conta>`
  - `ContaSpecification.comDataVencimentoEntre(LocalDate, LocalDate): Specification<Conta>`
  - `UsuarioJpaRepository.findByEmail(String): Optional<Usuario>`

- [ ] **Step 1: Criar FornecedorJpaRepository.java**

```java
package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorJpaRepository extends JpaRepository<Fornecedor, Long> {
}
```

- [ ] **Step 2: Criar ContaJpaRepository.java**

`@EntityGraph` carrega `fornecedor` junto com `Conta` em um único SELECT (JOIN), evitando o problema N+1 na listagem.

```java
package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Conta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ContaJpaRepository extends JpaRepository<Conta, UUID>, JpaSpecificationExecutor<Conta> {

    @Override
    @EntityGraph(attributePaths = {"fornecedor"})
    Page<Conta> findAll(Specification<Conta> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"fornecedor"})
    Optional<Conta> findById(UUID id);

    @Query("""
        SELECT COALESCE(SUM(c.valor), 0)
        FROM Conta c
        WHERE c.situacao = 'PAGO'
          AND c.dataPagamento BETWEEN :inicio AND :fim
        """)
    BigDecimal calcularTotalPago(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
```

- [ ] **Step 3: Criar ContaSpecification.java**

```java
package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Conta;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class ContaSpecification {

    private ContaSpecification() {}

    public static Specification<Conta> comDescricao(String descricao) {
        return (root, query, cb) -> {
            if (descricao == null || descricao.isBlank()) return null;
            return cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%");
        };
    }

    public static Specification<Conta> comDataVencimentoEntre(LocalDate inicio, LocalDate fim) {
        return (root, query, cb) -> {
            if (inicio == null && fim == null) return null;
            if (inicio == null) return cb.lessThanOrEqualTo(root.get("dataVencimento"), fim);
            if (fim == null) return cb.greaterThanOrEqualTo(root.get("dataVencimento"), inicio);
            return cb.between(root.get("dataVencimento"), inicio, fim);
        };
    }
}
```

- [ ] **Step 4: Criar UsuarioJpaRepository.java**

```java
package com.totvus.payments.infrastructure.persistence;

import com.totvus.payments.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}
```

- [ ] **Step 5: Compilar**

```bash
mvn clean compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/totvus/payments/infrastructure/persistence/
git commit -m "feat: JPA repositories with @EntityGraph for N+1 prevention and dynamic Specifications"
```

---

## Task 5: CRUD de Fornecedor (Application + Web)

**Files:**
- Create: `src/main/java/com/totvus/payments/application/fornecedor/FornecedorRequest.java`
- Create: `src/main/java/com/totvus/payments/application/fornecedor/FornecedorResponse.java`
- Create: `src/main/java/com/totvus/payments/application/fornecedor/FornecedorService.java`
- Create: `src/main/java/com/totvus/payments/web/FornecedorController.java`
- Create: `src/main/java/com/totvus/payments/web/GlobalExceptionHandler.java` (versão inicial)

**Interfaces:**
- Consumes: `FornecedorJpaRepository`
- Produces:
  - `FornecedorService.criar(FornecedorRequest): FornecedorResponse`
  - `FornecedorService.buscarPorId(Long): FornecedorResponse`
  - `FornecedorService.listar(): List<FornecedorResponse>`
  - `FornecedorService.atualizar(Long, FornecedorRequest): FornecedorResponse`
  - `FornecedorService.deletar(Long): void`
  - `GET/POST/PUT/DELETE /api/fornecedores`

- [ ] **Step 1: Criar FornecedorRequest.java**

```java
package com.totvus.payments.application.fornecedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String nome
) {}
```

- [ ] **Step 2: Criar FornecedorResponse.java**

```java
package com.totvus.payments.application.fornecedor;

import com.totvus.payments.domain.model.Fornecedor;

public record FornecedorResponse(Long id, String nome) {

    public static FornecedorResponse from(Fornecedor fornecedor) {
        return new FornecedorResponse(fornecedor.getId(), fornecedor.getNome());
    }
}
```

- [ ] **Step 3: Criar FornecedorService.java**

```java
package com.totvus.payments.application.fornecedor;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FornecedorService {

    private final FornecedorJpaRepository repository;

    public FornecedorService(FornecedorJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        var fornecedor = new Fornecedor(request.nome());
        return FornecedorResponse.from(repository.save(fornecedor));
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorId(Long id) {
        return repository.findById(id)
            .map(FornecedorResponse::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listar() {
        return repository.findAll().stream()
            .map(FornecedorResponse::from)
            .toList();
    }

    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        var fornecedor = repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
        fornecedor.atualizarNome(request.nome());
        return FornecedorResponse.from(repository.save(fornecedor));
    }

    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id);
        }
        repository.deleteById(id);
    }
}
```

- [ ] **Step 4: Criar GlobalExceptionHandler.java (versão inicial)**

```java
package com.totvus.payments.web;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.exception.TransicaoEstadoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransicaoEstadoInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoEstadoInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                (msg1, msg2) -> msg1));
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erro de validação");
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno do servidor");
    }
}
```

- [ ] **Step 5: Criar FornecedorController.java**

Por enquanto sem segurança (`SecurityConfig` virá na Task 7). Ao adicionar segurança, os endpoints serão protegidos automaticamente.

```java
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
```

- [ ] **Step 6: Subir a aplicação localmente e testar manualmente**

```bash
docker-compose up postgres rabbitmq -d
mvn spring-boot:run
```

```bash
# Criar fornecedor
curl -s -X POST http://localhost:8080/api/fornecedores \
  -H "Content-Type: application/json" \
  -d '{"nome": "Acme Corp"}' | jq .

# Listar
curl -s http://localhost:8080/api/fornecedores | jq .

# Testar validação (nome vazio deve retornar 400)
curl -s -X POST http://localhost:8080/api/fornecedores \
  -H "Content-Type: application/json" \
  -d '{"nome": ""}' | jq .
```

Expected para POST criação: `{"id": 1, "nome": "Acme Corp"}`
Expected para nome vazio: `{"status": 400, "erros": {"nome": "Nome é obrigatório"}}`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/totvus/payments/application/fornecedor/ \
        src/main/java/com/totvus/payments/web/
git commit -m "feat: Fornecedor CRUD — service, controller, and global exception handler"
```

---

## Task 6: CRUD de Conta + Listagem Paginada + Relatório

**Files:**
- Create: `src/main/java/com/totvus/payments/application/conta/ContaRequest.java`
- Create: `src/main/java/com/totvus/payments/application/conta/ContaResponse.java`
- Create: `src/main/java/com/totvus/payments/application/conta/SituacaoRequest.java`
- Create: `src/main/java/com/totvus/payments/application/conta/TotalPagoResponse.java`
- Create: `src/main/java/com/totvus/payments/application/conta/ContaService.java`
- Create: `src/main/java/com/totvus/payments/web/ContaController.java`

**Interfaces:**
- Consumes: `ContaJpaRepository`, `ContaSpecification`, `FornecedorJpaRepository`
- Produces:
  - `ContaService.criar(ContaRequest): ContaResponse`
  - `ContaService.buscarPorId(UUID): ContaResponse`
  - `ContaService.listar(String descricao, LocalDate inicio, LocalDate fim, Pageable): Page<ContaResponse>`
  - `ContaService.atualizar(UUID, ContaRequest): ContaResponse`
  - `ContaService.alterarSituacao(UUID, SituacaoRequest): ContaResponse`
  - `ContaService.deletar(UUID): void`
  - `ContaService.totalPagoPorPeriodo(LocalDate, LocalDate): TotalPagoResponse`
  - `GET/POST/PUT/PATCH/DELETE /api/contas`

- [ ] **Step 1: Criar ContaRequest.java**

```java
package com.totvus.payments.application.conta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaRequest(
    @NotNull(message = "Fornecedor é obrigatório")
    Long fornecedorId,

    @NotNull(message = "Data de vencimento é obrigatória")
    LocalDate dataVencimento,

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser positivo")
    BigDecimal valor,

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    String descricao
) {}
```

- [ ] **Step 2: Criar ContaResponse.java**

```java
package com.totvus.payments.application.conta;

import com.totvus.payments.application.fornecedor.FornecedorResponse;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.SituacaoConta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContaResponse(
    UUID id,
    LocalDate dataVencimento,
    LocalDate dataPagamento,
    BigDecimal valor,
    String descricao,
    SituacaoConta situacao,
    FornecedorResponse fornecedor
) {
    public static ContaResponse from(Conta conta) {
        return new ContaResponse(
            conta.getId(),
            conta.getDataVencimento(),
            conta.getDataPagamento(),
            conta.getValor(),
            conta.getDescricao(),
            conta.getSituacao(),
            FornecedorResponse.from(conta.getFornecedor())
        );
    }
}
```

- [ ] **Step 3: Criar SituacaoRequest.java e TotalPagoResponse.java**

```java
package com.totvus.payments.application.conta;

import com.totvus.payments.domain.model.SituacaoConta;
import jakarta.validation.constraints.NotNull;

public record SituacaoRequest(
    @NotNull(message = "Situação é obrigatória")
    SituacaoConta situacao
) {}
```

```java
package com.totvus.payments.application.conta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TotalPagoResponse(LocalDate inicio, LocalDate fim, BigDecimal totalPago) {}
```

- [ ] **Step 4: Criar ContaService.java**

```java
package com.totvus.payments.application.conta;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.ContaSpecification;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ContaService {

    private final ContaJpaRepository contaRepository;
    private final FornecedorJpaRepository fornecedorRepository;

    public ContaService(ContaJpaRepository contaRepository,
                        FornecedorJpaRepository fornecedorRepository) {
        this.contaRepository = contaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public ContaResponse criar(ContaRequest request) {
        var fornecedor = fornecedorRepository.findById(request.fornecedorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Fornecedor não encontrado: " + request.fornecedorId()));
        var conta = Conta.criar(fornecedor, request.dataVencimento(),
                                request.valor(), request.descricao());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional(readOnly = true)
    public ContaResponse buscarPorId(UUID id) {
        return contaRepository.findById(id)
            .map(ContaResponse::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ContaResponse> listar(String descricao, LocalDate dataVencimentoInicio,
                                      LocalDate dataVencimentoFim, Pageable pageable) {
        Specification<Conta> spec = Specification
            .where(ContaSpecification.comDescricao(descricao))
            .and(ContaSpecification.comDataVencimentoEntre(dataVencimentoInicio, dataVencimentoFim));
        return contaRepository.findAll(spec, pageable).map(ContaResponse::from);
    }

    @Transactional
    public ContaResponse atualizar(UUID id, ContaRequest request) {
        var conta = contaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
        conta.atualizar(request.dataVencimento(), request.valor(), request.descricao());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional
    public ContaResponse alterarSituacao(UUID id, SituacaoRequest request) {
        var conta = contaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
        conta.alterarSituacao(request.situacao());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional
    public void deletar(UUID id) {
        if (!contaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Conta não encontrada: " + id);
        }
        contaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TotalPagoResponse totalPagoPorPeriodo(LocalDate inicio, LocalDate fim) {
        var total = contaRepository.calcularTotalPago(inicio, fim);
        return new TotalPagoResponse(inicio, fim, total);
    }
}
```

- [ ] **Step 5: Criar ContaController.java**

```java
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
```

- [ ] **Step 6: Testar manualmente**

```bash
# Criar conta (use o ID do fornecedor criado na Task 5)
curl -s -X POST http://localhost:8080/api/contas \
  -H "Content-Type: application/json" \
  -d '{"fornecedorId":1,"dataVencimento":"2024-02-01","valor":1500.00,"descricao":"Fornecimento mensal"}' | jq .

# Listar com filtros
curl -s "http://localhost:8080/api/contas?descricao=fornecimento&page=0&size=5" | jq .

# Alterar situação
curl -s -X PATCH http://localhost:8080/api/contas/{id}/situacao \
  -H "Content-Type: application/json" \
  -d '{"situacao":"PAGO"}' | jq .

# Tentar voltar para PENDENTE (deve retornar 422)
curl -s -X PATCH http://localhost:8080/api/contas/{id}/situacao \
  -H "Content-Type: application/json" \
  -d '{"situacao":"PENDENTE"}' | jq .

# Relatório
curl -s "http://localhost:8080/api/contas/relatorio/total-pago?inicio=2024-01-01&fim=2024-12-31" | jq .
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/totvus/payments/application/conta/ \
        src/main/java/com/totvus/payments/web/ContaController.java
git commit -m "feat: Conta CRUD with pagination, filters, status machine, and total-paid report"
```

---

## Task 7: Segurança — JWT

**Files:**
- Create: `src/main/java/com/totvus/payments/infrastructure/security/JwtService.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/security/JwtAuthFilter.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/security/SecurityConfig.java`
- Create: `src/main/java/com/totvus/payments/application/auth/LoginRequest.java`
- Create: `src/main/java/com/totvus/payments/application/auth/LoginResponse.java`
- Create: `src/main/java/com/totvus/payments/application/auth/AuthService.java`
- Create: `src/main/java/com/totvus/payments/web/AuthController.java`

**Interfaces:**
- Consumes: `UsuarioJpaRepository`
- Produces:
  - `POST /api/auth/login` — retorna JWT
  - Todos os demais endpoints exigem `Authorization: Bearer <token>`
  - `JwtService.gerarToken(String email): String`
  - `JwtService.extrairEmail(String token): String`
  - `JwtService.tokenValido(String token, UserDetails): boolean`

- [ ] **Step 1: Criar JwtService.java**

```java
package com.totvus.payments.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String gerarToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getKey())
            .compact();
    }

    public String extrairEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        return extrairEmail(token).equals(userDetails.getUsername())
            && !getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 2: Criar JwtAuthFilter.java**

```java
package com.totvus.payments.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        var token = authHeader.substring(7);
        var email = jwtService.extrairEmail(token);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userDetails = userDetailsService.loadUserByUsername(email);
            if (jwtService.tokenValido(token, userDetails)) {
                var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Criar SecurityConfig.java**

```java
package com.totvus.payments.infrastructure.security;

import com.totvus.payments.infrastructure.persistence.UsuarioJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final UsuarioJpaRepository usuarioRepository;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(UsuarioJpaRepository usuarioRepository, JwtAuthFilter jwtAuthFilter) {
        this.usuarioRepository = usuarioRepository;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> usuarioRepository.findByEmail(email)
            .map(u -> User.withUsername(u.getEmail())
                .password(u.getSenha())
                .authorities(u.getRole())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 4: Criar AuthService.java, LoginRequest.java e LoginResponse.java**

```java
package com.totvus.payments.application.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String senha
) {}
```

```java
package com.totvus.payments.application.auth;

public record LoginResponse(String token, String tipo) {
    public LoginResponse(String token) {
        this(token, "Bearer");
    }
}
```

```java
package com.totvus.payments.application.auth;

import com.totvus.payments.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authManager, JwtService jwtService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.senha()));
        return new LoginResponse(jwtService.gerarToken(request.email()));
    }
}
```

- [ ] **Step 5: Criar AuthController.java**

```java
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
```

- [ ] **Step 6: Testar autenticação**

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@totvus.com","senha":"admin123"}' | jq -r .token)

echo "Token: $TOKEN"

# Acessar endpoint protegido com token
curl -s http://localhost:8080/api/fornecedores \
  -H "Authorization: Bearer $TOKEN" | jq .

# Acessar sem token (deve retornar 403)
curl -s http://localhost:8080/api/fornecedores | jq .
```

Expected: Login retorna `{"token": "eyJ...", "tipo": "Bearer"}`
Expected: Sem token retorna 403

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/totvus/payments/infrastructure/security/ \
        src/main/java/com/totvus/payments/application/auth/ \
        src/main/java/com/totvus/payments/web/AuthController.java
git commit -m "feat: JWT authentication — stateless security filter, login endpoint, BCrypt passwords"
```

---

## Task 8: Importação Assíncrona via CSV (RabbitMQ)

**Files:**
- Create: `src/main/java/com/totvus/payments/infrastructure/messaging/RabbitMQConfig.java`
- Create: `src/main/java/com/totvus/payments/application/importacao/ImportacaoMessage.java`
- Create: `src/main/java/com/totvus/payments/application/importacao/ImportacaoResponse.java`
- Create: `src/main/java/com/totvus/payments/application/importacao/ImportacaoService.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/messaging/ImportacaoProducer.java`
- Create: `src/main/java/com/totvus/payments/infrastructure/messaging/ImportacaoConsumer.java`
- Create: `src/main/java/com/totvus/payments/web/ImportacaoController.java`

**Interfaces:**
- Consumes: `ContaJpaRepository`, `FornecedorJpaRepository`
- Produces:
  - `POST /api/importacao/contas` (multipart/form-data, campo `arquivo`)
  - Retorna `ImportacaoResponse(protocolo, status)` imediatamente
  - Consumer processa em background, linha a linha, com tolerância a falhas parciais
  - Filas: `contas.importacao` (main) + `contas.importacao.dlq` (dead letter)

**CSV esperado (cabeçalho obrigatório):**
```
fornecedorId,dataVencimento,dataPagamento,valor,descricao,situacao
1,2024-02-01,2024-01-30,1500.00,Fornecimento mensal,PAGO
2,2024-03-01,,500.00,Serviço de limpeza,PENDENTE
```

- [ ] **Step 1: Criar RabbitMQConfig.java**

```java
package com.totvus.payments.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_IMPORTACAO     = "contas.importacao";
    public static final String QUEUE_IMPORTACAO_DLQ = "contas.importacao.dlq";
    public static final String EXCHANGE_IMPORTACAO  = "contas.importacao.exchange";
    public static final String EXCHANGE_DLQ         = "contas.importacao.dlq.exchange";

    @Bean
    public DirectExchange exchangeImportacao() {
        return new DirectExchange(EXCHANGE_IMPORTACAO);
    }

    @Bean
    public DirectExchange exchangeDlq() {
        return new DirectExchange(EXCHANGE_DLQ);
    }

    @Bean
    public Queue queueImportacao() {
        return QueueBuilder.durable(QUEUE_IMPORTACAO)
            .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
            .withArgument("x-dead-letter-routing-key", QUEUE_IMPORTACAO_DLQ)
            .build();
    }

    @Bean
    public Queue queueDlq() {
        return QueueBuilder.durable(QUEUE_IMPORTACAO_DLQ).build();
    }

    @Bean
    public Binding bindingImportacao(Queue queueImportacao, DirectExchange exchangeImportacao) {
        return BindingBuilder.bind(queueImportacao).to(exchangeImportacao)
            .with(QUEUE_IMPORTACAO);
    }

    @Bean
    public Binding bindingDlq(Queue queueDlq, DirectExchange exchangeDlq) {
        return BindingBuilder.bind(queueDlq).to(exchangeDlq).with(QUEUE_IMPORTACAO_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
```

- [ ] **Step 2: Criar ImportacaoMessage.java e ImportacaoResponse.java**

```java
package com.totvus.payments.application.importacao;

public record ImportacaoMessage(String protocolo, String csvContent) {}
```

```java
package com.totvus.payments.application.importacao;

public record ImportacaoResponse(String protocolo, String status, String mensagem) {}
```

- [ ] **Step 3: Criar ImportacaoService.java**

```java
package com.totvus.payments.application.importacao;

import com.totvus.payments.infrastructure.messaging.ImportacaoProducer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ImportacaoService {

    private final ImportacaoProducer producer;

    public ImportacaoService(ImportacaoProducer producer) {
        this.producer = producer;
    }

    public ImportacaoResponse importar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new com.totvus.payments.domain.exception.DomainException("Arquivo CSV não pode estar vazio");
        }
        String nomeArquivo = arquivo.getOriginalFilename();
        if (nomeArquivo == null || !nomeArquivo.endsWith(".csv")) {
            throw new com.totvus.payments.domain.exception.DomainException("Apenas arquivos .csv são aceitos");
        }
        try {
            String csvContent = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
            String protocolo = UUID.randomUUID().toString();
            producer.publicar(new ImportacaoMessage(protocolo, csvContent));
            return new ImportacaoResponse(protocolo, "PROCESSANDO",
                "Arquivo recebido. Acompanhe o processamento pelo protocolo.");
        } catch (IOException e) {
            throw new com.totvus.payments.domain.exception.DomainException("Erro ao ler o arquivo CSV");
        }
    }
}
```

- [ ] **Step 4: Criar ImportacaoProducer.java**

```java
package com.totvus.payments.infrastructure.messaging;

import com.totvus.payments.application.importacao.ImportacaoMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ImportacaoProducer {

    private final RabbitTemplate rabbitTemplate;

    public ImportacaoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(ImportacaoMessage message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMPORTACAO,
            RabbitMQConfig.QUEUE_IMPORTACAO,
            message
        );
    }
}
```

- [ ] **Step 5: Criar ImportacaoConsumer.java**

A estratégia de resiliência: cada linha do CSV é processada individualmente. Se uma linha falhar, o erro é logado e o processamento continua nas próximas linhas. Se a mensagem inteira falhar (ex: CSV corrompido, sem cabeçalho), ela vai para a DLQ após as tentativas de retry configuradas no `application.yml`.

```java
package com.totvus.payments.infrastructure.messaging;

import com.totvus.payments.application.importacao.ImportacaoMessage;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.SituacaoConta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ImportacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoConsumer.class);

    private final ContaJpaRepository contaRepository;
    private final FornecedorJpaRepository fornecedorRepository;

    public ImportacaoConsumer(ContaJpaRepository contaRepository,
                               FornecedorJpaRepository fornecedorRepository) {
        this.contaRepository = contaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_IMPORTACAO)
    @Transactional
    public void processar(ImportacaoMessage message) {
        log.info("Iniciando importação. Protocolo: {}", message.protocolo());
        var reader = new BufferedReader(new StringReader(message.csvContent()));
        var contador = new AtomicInteger(0);
        var erros = new AtomicInteger(0);

        try {
            // Pular linha de cabeçalho
            String cabecalho = reader.readLine();
            if (cabecalho == null) {
                throw new IllegalArgumentException("CSV vazio ou sem cabeçalho");
            }

            String linha;
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                String linhaAtual = linha;
                int numAtual = numeroLinha;
                try {
                    processarLinha(linhaAtual);
                    contador.incrementAndGet();
                } catch (Exception e) {
                    erros.incrementAndGet();
                    log.warn("Protocolo {}: erro na linha {} — {} | Linha: '{}'",
                        message.protocolo(), numAtual, e.getMessage(), linhaAtual);
                }
            }
        } catch (Exception e) {
            log.error("Protocolo {}: falha crítica ao processar CSV — {}", message.protocolo(), e.getMessage());
            throw new RuntimeException("Falha crítica no processamento do CSV", e);
        }

        log.info("Importação concluída. Protocolo: {} | Sucesso: {} | Erros: {}",
            message.protocolo(), contador.get(), erros.get());
    }

    private void processarLinha(String linha) {
        String[] campos = linha.split(",", -1);
        if (campos.length < 6) {
            throw new IllegalArgumentException("Número de colunas inválido: " + campos.length + " (esperado 6)");
        }

        Long fornecedorId = Long.parseLong(campos[0].trim());
        LocalDate dataVencimento = LocalDate.parse(campos[1].trim());
        LocalDate dataPagamento = campos[2].trim().isBlank() ? null : LocalDate.parse(campos[2].trim());
        BigDecimal valor = new BigDecimal(campos[3].trim());
        String descricao = campos[4].trim();
        SituacaoConta situacao = SituacaoConta.valueOf(campos[5].trim().toUpperCase());

        var fornecedor = fornecedorRepository.findById(fornecedorId)
            .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + fornecedorId));

        var conta = Conta.criar(fornecedor, dataVencimento, valor, descricao);

        if (situacao != SituacaoConta.PENDENTE) {
            conta.alterarSituacao(situacao);
        }
        if (dataPagamento != null && conta.getDataPagamento() == null) {
            // dataPagamento já é setada automaticamente ao marcar como PAGO
            // se vier explícita e não foi setada, é ignorada (campo calculado)
        }

        contaRepository.save(conta);
    }
}
```

- [ ] **Step 6: Criar ImportacaoController.java**

```java
package com.totvus.payments.web;

import com.totvus.payments.application.importacao.ImportacaoResponse;
import com.totvus.payments.application.importacao.ImportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacao")
@Tag(name = "Importação")
public class ImportacaoController {

    private final ImportacaoService service;

    public ImportacaoController(ImportacaoService service) {
        this.service = service;
    }

    @PostMapping(value = "/contas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Importar contas via CSV (processamento assíncrono)")
    public ImportacaoResponse importar(@RequestParam("arquivo") MultipartFile arquivo) {
        return service.importar(arquivo);
    }
}
```

- [ ] **Step 7: Criar arquivo CSV de teste**

Criar `src/test/resources/contas_teste.csv`:
```
fornecedorId,dataVencimento,dataPagamento,valor,descricao,situacao
1,2024-02-01,2024-01-30,1500.00,Fornecimento mensal,PAGO
1,2024-03-01,,500.00,Servico de limpeza,PENDENTE
999,2024-04-01,,200.00,Linha com fornecedor invalido,PENDENTE
1,2024-05-01,,abc,Linha com valor invalido,PENDENTE
```

- [ ] **Step 8: Testar importação**

```bash
# Subir RabbitMQ se não estiver rodando
docker-compose up rabbitmq -d

# Importar CSV (com token JWT obtido na Task 7)
curl -s -X POST http://localhost:8080/api/importacao/contas \
  -H "Authorization: Bearer $TOKEN" \
  -F "arquivo=@src/test/resources/contas_teste.csv" | jq .
```

Expected: `{"protocolo": "uuid-aqui", "status": "PROCESSANDO", "mensagem": "..."}`

Verificar nos logs que linhas 3 e 4 (fornecedor inválido e valor inválido) geraram warnings, mas linhas 1 e 2 foram inseridas.

Acessar RabbitMQ Management em http://localhost:15672 (guest/guest) e confirmar que a DLQ `contas.importacao.dlq` está vazia.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/totvus/payments/infrastructure/messaging/ \
        src/main/java/com/totvus/payments/application/importacao/ \
        src/main/java/com/totvus/payments/web/ImportacaoController.java \
        src/test/resources/contas_teste.csv
git commit -m "feat: async CSV import via RabbitMQ — producer/consumer with per-row resilience and DLQ"
```

---

## Task 9: Testes Unitários

**Files:**
- Create: `src/test/java/com/totvus/payments/domain/ContaTest.java`
- Create: `src/test/java/com/totvus/payments/application/conta/ContaServiceTest.java`
- Create: `src/test/java/com/totvus/payments/application/fornecedor/FornecedorServiceTest.java`

**Interfaces:**
- Consumes: `Conta`, `ContaService`, `FornecedorService` (interfaces definidas nas Tasks 3, 5, 6)
- Produces: suite de testes unitários com Mockito cobrindo: invariantes de domínio, regras de fluxo do service, transições de estado

- [ ] **Step 1: Criar ContaTest.java**

```java
package com.totvus.payments.domain;

import com.totvus.payments.domain.exception.DomainException;
import com.totvus.payments.domain.exception.TransicaoEstadoInvalidaException;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.domain.model.SituacaoConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ContaTest {

    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        fornecedor = new Fornecedor("Acme Corp");
    }

    @Test
    @DisplayName("Cria conta com situação PENDENTE por padrão")
    void deveCriarContaComSituacaoPendente() {
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(30),
            new BigDecimal("1000.00"), "Serviço de TI");
        assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.PENDENTE);
        assertThat(conta.getDataPagamento()).isNull();
    }

    @Test
    @DisplayName("Transição PENDENTE → PAGO é permitida e seta data de pagamento")
    void devePermitirTransicaoPendenteParaPago() {
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(30),
            new BigDecimal("500.00"), "Material de escritório");
        conta.alterarSituacao(SituacaoConta.PAGO);
        assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.PAGO);
        assertThat(conta.getDataPagamento()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Transição PENDENTE → CANCELADO é permitida")
    void devePermitirTransicaoPendenteParaCancelado() {
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("200.00"), "Consultoria");
        conta.alterarSituacao(SituacaoConta.CANCELADO);
        assertThat(conta.getSituacao()).isEqualTo(SituacaoConta.CANCELADO);
    }

    @Test
    @DisplayName("PAGO não pode voltar para PENDENTE")
    void deveLancarExcecaoAoTentarVoltarDePagoParaPendente() {
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("300.00"), "Manutenção");
        conta.alterarSituacao(SituacaoConta.PAGO);
        assertThatThrownBy(() -> conta.alterarSituacao(SituacaoConta.PENDENTE))
            .isInstanceOf(TransicaoEstadoInvalidaException.class)
            .hasMessageContaining("PAGO")
            .hasMessageContaining("PENDENTE");
    }

    @Test
    @DisplayName("PAGO não pode ser CANCELADO")
    void deveLancarExcecaoAoTentarCancelarContaPaga() {
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("300.00"), "Aluguel");
        conta.alterarSituacao(SituacaoConta.PAGO);
        assertThatThrownBy(() -> conta.alterarSituacao(SituacaoConta.CANCELADO))
            .isInstanceOf(TransicaoEstadoInvalidaException.class);
    }

    @Test
    @DisplayName("CANCELADO é estado final — não aceita transições")
    void deveLancarExcecaoAoTentarMudarEstadoDeCancelado() {
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("100.00"), "Frete");
        conta.alterarSituacao(SituacaoConta.CANCELADO);
        assertThatThrownBy(() -> conta.alterarSituacao(SituacaoConta.PAGO))
            .isInstanceOf(TransicaoEstadoInvalidaException.class);
    }

    @Test
    @DisplayName("Valor zero lança DomainException")
    void deveLancarExcecaoParaValorZero() {
        assertThatThrownBy(() -> Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            BigDecimal.ZERO, "Descrição"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("positivo");
    }

    @Test
    @DisplayName("Valor negativo lança DomainException")
    void deveLancarExcecaoParaValorNegativo() {
        assertThatThrownBy(() -> Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("-1"), "Descrição"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("positivo");
    }

    @Test
    @DisplayName("Descrição em branco lança DomainException")
    void deveLancarExcecaoParaDescricaoEmBranco() {
        assertThatThrownBy(() -> Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("100.00"), "  "))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("descrição");
    }

    @Test
    @DisplayName("Data de vencimento nula lança DomainException")
    void deveLancarExcecaoParaDataVencimentoNula() {
        assertThatThrownBy(() -> Conta.criar(fornecedor, null,
            new BigDecimal("100.00"), "Descrição"))
            .isInstanceOf(DomainException.class);
    }
}
```

- [ ] **Step 2: Rodar ContaTest**

```bash
mvn test -Dtest=ContaTest -pl . --no-transfer-progress
```

Expected: `Tests run: 9, Failures: 0, Errors: 0`

- [ ] **Step 3: Criar ContaServiceTest.java**

```java
package com.totvus.payments.application.conta;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Conta;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.domain.model.SituacaoConta;
import com.totvus.payments.infrastructure.persistence.ContaJpaRepository;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock private ContaJpaRepository contaRepository;
    @Mock private FornecedorJpaRepository fornecedorRepository;
    @InjectMocks private ContaService service;

    private final Fornecedor fornecedor = new Fornecedor("Acme Corp");

    @Test
    @DisplayName("Cria conta com sucesso quando fornecedor existe")
    void deveCriarContaComSucesso() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(30),
            new BigDecimal("1000.00"), "Serviço");
        when(contaRepository.save(any())).thenReturn(conta);

        var request = new ContaRequest(1L, LocalDate.now().plusDays(30),
            new BigDecimal("1000.00"), "Serviço");
        var response = service.criar(request);

        assertThat(response.valor()).isEqualByComparingTo("1000.00");
        assertThat(response.situacao()).isEqualTo(SituacaoConta.PENDENTE);
        verify(contaRepository).save(any(Conta.class));
    }

    @Test
    @DisplayName("Lança exceção quando fornecedor não existe ao criar conta")
    void deveLancarExcecaoSeFornecedorNaoExistir() {
        when(fornecedorRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new ContaRequest(99L, LocalDate.now().plusDays(30),
            new BigDecimal("500.00"), "Serviço");

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
        verify(contaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lista contas com filtros retorna página correta")
    void deveListarContasComFiltros() {
        var conta = Conta.criar(fornecedor, LocalDate.now(), new BigDecimal("200.00"), "Aluguel");
        var page = new PageImpl<>(List.of(conta));
        when(contaRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(page);

        var resultado = service.listar("aluguel", null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).descricao()).isEqualTo("Aluguel");
    }

    @Test
    @DisplayName("Calcula total pago por período")
    void deveCalcularTotalPagoPorPeriodo() {
        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fim = LocalDate.of(2024, 12, 31);
        when(contaRepository.calcularTotalPago(inicio, fim)).thenReturn(new BigDecimal("5000.00"));

        var response = service.totalPagoPorPeriodo(inicio, fim);

        assertThat(response.totalPago()).isEqualByComparingTo("5000.00");
        assertThat(response.inicio()).isEqualTo(inicio);
        assertThat(response.fim()).isEqualTo(fim);
    }

    @Test
    @DisplayName("Altera situação de PENDENTE para PAGO com sucesso")
    void deveAlterarSituacaoComSucesso() {
        var id = UUID.randomUUID();
        var conta = Conta.criar(fornecedor, LocalDate.now().plusDays(10),
            new BigDecimal("300.00"), "Manutenção");
        when(contaRepository.findById(id)).thenReturn(Optional.of(conta));
        when(contaRepository.save(any())).thenReturn(conta);

        var response = service.alterarSituacao(id, new SituacaoRequest(SituacaoConta.PAGO));

        assertThat(response.situacao()).isEqualTo(SituacaoConta.PAGO);
        verify(contaRepository).save(conta);
    }

    @Test
    @DisplayName("Lança exceção ao buscar conta inexistente")
    void deveLancarExcecaoAoBuscarContaInexistente() {
        var id = UUID.randomUUID();
        when(contaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
```

- [ ] **Step 4: Rodar ContaServiceTest**

```bash
mvn test -Dtest=ContaServiceTest --no-transfer-progress
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Criar FornecedorServiceTest.java**

```java
package com.totvus.payments.application.fornecedor;

import com.totvus.payments.domain.exception.RecursoNaoEncontradoException;
import com.totvus.payments.domain.model.Fornecedor;
import com.totvus.payments.infrastructure.persistence.FornecedorJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock private FornecedorJpaRepository repository;
    @InjectMocks private FornecedorService service;

    @Test
    @DisplayName("Cria fornecedor com sucesso")
    void deveCriarFornecedorComSucesso() {
        var fornecedor = new Fornecedor("Acme Corp");
        when(repository.save(any())).thenReturn(fornecedor);

        var response = service.criar(new FornecedorRequest("Acme Corp"));

        assertThat(response.nome()).isEqualTo("Acme Corp");
        verify(repository).save(any(Fornecedor.class));
    }

    @Test
    @DisplayName("Lança exceção ao buscar fornecedor inexistente")
    void deveLancarExcecaoSeFornecedorNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Lista todos os fornecedores")
    void deveListarFornecedores() {
        when(repository.findAll()).thenReturn(List.of(
            new Fornecedor("Acme"), new Fornecedor("Beta")));

        var lista = service.listar();

        assertThat(lista).hasSize(2);
    }

    @Test
    @DisplayName("Deleta fornecedor existente")
    void deveDeletarFornecedorExistente() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        assertThatCode(() -> service.deletar(1L)).doesNotThrowAnyException();
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Lança exceção ao deletar fornecedor inexistente")
    void deveLancarExcecaoAoDeletarFornecedorInexistente() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deletar(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).deleteById(any());
    }
}
```

- [ ] **Step 6: Rodar todos os testes**

```bash
mvn test --no-transfer-progress
```

Expected: Todos os testes passando. `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/test/
git commit -m "test: unit tests for domain invariants and service business rules"
```

---

## Task 10: Swagger/OpenAPI + README

**Files:**
- Modify: `README.md`
- Create: `src/main/java/com/totvus/payments/infrastructure/config/OpenApiConfig.java`

**Interfaces:**
- Produces: Swagger UI acessível em http://localhost:8080/swagger-ui.html com suporte a JWT; README com instruções completas

- [ ] **Step 1: Criar OpenApiConfig.java**

```java
package com.totvus.payments.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API de Gestão de Contas a Pagar",
        version = "1.0.0",
        description = "API REST para gestão de contas a pagar com importação assíncrona via CSV"
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}
```

- [ ] **Step 2: Atualizar README.md**

```markdown
# API de Gestão de Contas a Pagar

API REST para gestão de contas a pagar com importação assíncrona via CSV.

## Como Executar

**Pré-requisitos:** Docker e Docker Compose instalados.

```bash
docker-compose up --build
```

A aplicação estará disponível em `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`
RabbitMQ Management: `http://localhost:15672` (guest/guest)

## Autenticação

Todas as rotas (exceto `/api/auth/login`) requerem JWT.

```bash
# 1. Obter token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@totvus.com","senha":"admin123"}' | jq -r .token)

# 2. Usar nas requisições
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/fornecedores
```

## Exemplos de Uso

### Fornecedores

```bash
# Criar
curl -X POST http://localhost:8080/api/fornecedores \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome": "Acme Corp"}'

# Listar
curl http://localhost:8080/api/fornecedores \
  -H "Authorization: Bearer $TOKEN"
```

### Contas

```bash
# Criar
curl -X POST http://localhost:8080/api/contas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fornecedorId":1,"dataVencimento":"2024-12-31","valor":1500.00,"descricao":"Fornecimento mensal"}'

# Listar com filtros (paginado)
curl "http://localhost:8080/api/contas?descricao=fornecimento&dataVencimentoInicio=2024-01-01&dataVencimentoFim=2024-12-31&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Alterar situação
curl -X PATCH http://localhost:8080/api/contas/{id}/situacao \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"situacao":"PAGO"}'

# Relatório de total pago por período
curl "http://localhost:8080/api/contas/relatorio/total-pago?inicio=2024-01-01&fim=2024-12-31" \
  -H "Authorization: Bearer $TOKEN"
```

### Importação CSV

```bash
# Formato esperado do CSV:
# fornecedorId,dataVencimento,dataPagamento,valor,descricao,situacao

curl -X POST http://localhost:8080/api/importacao/contas \
  -H "Authorization: Bearer $TOKEN" \
  -F "arquivo=@contas.csv"
# Retorna imediatamente com um protocolo de acompanhamento
```

## Decisões Arquiteturais

### DDD (Domain-Driven Design)
- **Domain**: entidades `Conta` e `Fornecedor` encapsulam regras de negócio. `Conta.criar()` e `Conta.alterarSituacao()` protegem invariantes sem deixar o estado inválido vazar para fora da entidade.
- **Application**: services orquestram use cases, validam existência de recursos externos (fornecedor) e delegam ao domínio as regras de negócio.
- **Infrastructure**: JPA, RabbitMQ e JWT são detalhes de infraestrutura isolados do domínio.

### Prevenção de N+1
`Conta` tem relacionamento `@ManyToOne(fetch = LAZY)` com `Fornecedor`. Os métodos de consulta no `ContaJpaRepository` usam `@EntityGraph(attributePaths = {"fornecedor"})` para carregar o relacionamento em um único JOIN SELECT, evitando N+1 na listagem paginada.

### Resiliência na Importação CSV
O consumer processa cada linha do CSV individualmente em um try-catch. Falhas em linhas específicas (fornecedor inválido, valor incorreto) geram apenas um log de warning — o restante das linhas é processado normalmente. Falhas críticas (CSV corrompido) reenviam a mensagem para a DLQ após 3 tentativas (configurado via `spring.rabbitmq.listener.simple.retry`).

### Máquina de Estados
`SituacaoConta` (enum) define as transições permitidas em cada estado. `PAGO` e `CANCELADO` são estados finais — qualquer tentativa de transição lança `TransicaoEstadoInvalidaException` (HTTP 422).

### Validações em camadas
- **Contrato** (Bean Validation): campos obrigatórios, formatos, tamanhos máximos nos Records de request.
- **Fluxo** (Services): existência de fornecedor, existência da conta.
- **Domínio** (Entities): valores positivos, estados válidos, transições permitidas.
```

- [ ] **Step 3: Verificar Swagger UI**

```bash
# Garantir que a app está rodando
open http://localhost:8080/swagger-ui.html
```

Verificar:
- Todos os grupos de endpoints aparecem (Autenticação, Fornecedores, Contas a Pagar, Importação)
- O botão "Authorize" permite informar o token JWT
- Após autorizar, é possível executar chamadas diretamente pelo Swagger

- [ ] **Step 4: Commit final**

```bash
git add src/main/java/com/totvus/payments/infrastructure/config/OpenApiConfig.java README.md
git commit -m "docs: Swagger/OpenAPI config with JWT support, complete README with architecture decisions"
```

---

## Checklist de Entrega

- [ ] `docker-compose up --build` sobe tudo sem erros
- [ ] `POST /api/auth/login` retorna JWT
- [ ] CRUD de Fornecedor funciona com JWT
- [ ] CRUD de Conta funciona com JWT
- [ ] Listagem paginada com filtros por `descricao` e `dataVencimento` funciona
- [ ] Relatório de total pago por período funciona
- [ ] Transições inválidas (PAGO→PENDENTE) retornam 422
- [ ] Upload CSV retorna protocolo imediatamente (202 Accepted)
- [ ] Consumer processa CSV em background (verificar logs)
- [ ] Linhas inválidas do CSV geram warning, não falham o lote todo
- [ ] `mvn test` passa sem erros
- [ ] Swagger UI acessível em `/swagger-ui.html` com suporte a JWT
- [ ] Código hospedado em repositório público com README completo

## Ordem Recomendada de Desenvolvimento

1. Task 1 → Task 2 → Task 3 → Task 4 (fundação sem lógica de negócio)
2. Task 5 → Task 6 (features principais)
3. Task 7 (adicionar segurança — todos os endpoints ficam protegidos)
4. Task 8 (mensageria + importação assíncrona)
5. Task 9 (testes — confirmar que as regras de negócio estão corretas)
6. Task 10 (documentação e README)
