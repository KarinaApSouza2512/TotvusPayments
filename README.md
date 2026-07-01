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
