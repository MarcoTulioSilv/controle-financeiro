# Controle Financeiro Doméstico

Sistema pessoal de controle financeiro para um domicílio: lançamentos por WhatsApp, cartões de
crédito com cálculo de fatura, orçamento, reserva e patrimônio. Roda localmente, **sem nenhuma
porta exposta à internet**.

Projeto de estudo construído com o **SWEBOK 4.0** como guia e práticas de **DevSecOps**.

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

*(Instruções de execução serão adicionadas ao longo da Sprint 0.)*
