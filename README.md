# Controle Financeiro Doméstico

Sistema pessoal de controle financeiro para um domicílio: lançamentos por WhatsApp, cartões de
crédito com cálculo de fatura, orçamento, reserva e patrimônio. Roda localmente, **sem nenhuma
porta exposta à internet**.

Projeto de estudo construído com o **SWEBOK 4.0** como guia e práticas de **DevSecOps**.

> Os nomes de pessoas, bancos e fontes de renda nos documentos são fictícios.

## Stack

| Componente | Tecnologia |
|---|---|
| core | Java 25 (LTS) + Spring Boot, Spring Security, Spring Data JPA, Flyway, Hibernate Envers, Thymeleaf + HTMX |
| gateway | TypeScript + Node.js LTS + Baileys |
| Banco | MySQL 8 |
| Execução | Docker Compose |

## Documentação

| Documento | Conteúdo |
|---|---|
| [ERS](docs/ERS.md) | Especificação de requisitos |
| [ADRs](docs/adr/README.md) | Decisões de arquitetura |
| [Design](docs/design/) | Modelo de dados, máquinas de estado, contrato gateway → core, interpretador, módulos |

## Primeiros passos

1. Copie `.env.example` para `.env`.
2. Gere cada senha e o token com um gerador criptográfico (PowerShell):
   ```powershell
   $b = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
   [Convert]::ToBase64String($b)
   ```
   Não use `Get-Random` para segredos: ele não é criptograficamente seguro.
3. Preencha o `.env`.

## Rodando localmente

### Variáveis que o `core` lê

Nenhuma credencial fica no repositório: a conexão com o banco vem de variáveis de ambiente,
as mesmas usadas pelo serviço `mysql` do Docker Compose.

| Variável | Para que serve | Padrão |
|---|---|---|
| `MYSQL_DATABASE` | nome do banco | obrigatória |
| `MYSQL_USER` | usuário da aplicação | obrigatória |
| `MYSQL_PASSWORD` | senha da aplicação | obrigatória |
| `MYSQL_HOST` | host do MySQL | `mysql` (nome do serviço no Compose); `127.0.0.1` no perfil `local` |
| `MYSQL_PORT` | porta do MySQL | `3306`; `3307` no perfil `local` |

### Passo a passo

1. Suba o banco (ele publica a porta apenas em `127.0.0.1:3307`):
   ```powershell
   docker compose up -d mysql
   ```
2. Carregue as variáveis do `.env` na sessão atual do PowerShell:
   ```powershell
   Get-Content .env | Where-Object { $_ -match '^\s*[^#\s]' } | ForEach-Object {
     $nome, $valor = $_ -split '=', 2
     [Environment]::SetEnvironmentVariable($nome.Trim(), $valor.Trim(), 'Process')
   }
   ```
3. Suba a aplicação com o perfil `local`, que aponta para `127.0.0.1:3307`:
   ```powershell
   cd core
   .\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=local"
   ```

### Banco de dados

Todo o schema vem das migrações do Flyway, em `core/src/main/resources/db/migration`
(o Hibernate roda com `ddl-auto=validate` e nunca cria nem altera tabelas). Ao subir, a
aplicação aplica as migrações que ainda faltam.

### Testes

```powershell
cd core
.\mvnw.cmd verify
```

Os testes de integração sobem um MySQL 8.4 em container com o Testcontainers, então o
**Docker Desktop precisa estar rodando**. Eles não usam o `.env` nem o banco do Compose: cada
execução começa com um banco limpo, o que também permite rodá-los no pipeline.
