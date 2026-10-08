# ADR-004: Stack da aplicação

- **Status:** Aceito
- **Data:** 2026-10
- **Requisitos relacionados:** R02, RNF02, RNF05, RNF07, RNF10, RNF11

## Contexto
Aplicação de uso pessoal (dois usuários), sem prazo, desenvolvida como projeto de
**aprendizado e portfólio**. O desenvolvedor domina Python e quer ganhar experiência em
outras linguagens. Já usa o MySQL Workbench. O canal WhatsApp exige uma biblioteca não
oficial (ADR-001), e a lógica fica separada da captura (ADR-002).

## Decisão
| Componente | Tecnologia |
|---|---|
| core | Java 25 (LTS) + Spring Boot: Web, Security, Data JPA, Validation; Flyway; Thymeleaf + HTMX; build com Maven |
| gateway | TypeScript sobre Node.js LTS, com Baileys (repositório oficial WhiskeySockets) |
| Banco | MySQL 8 |
| Execução | Docker Compose no notebook |

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| Python + Django + neonize | Mais rápida de construir, mas sem ganho de portfólio em outra linguagem |
| TypeScript + AdonisJS (monólito) | Chegou a ser aprovada e foi revista antes de qualquer código: sem decimal nativo e sem `java.time`; menor ganho de aprendizado |
| Java + Spring Boot + Cobalt (tudo em Java) | Biblioteca de WhatsApp pré-1.0, com um único mantenedor e mudanças incompatíveis frequentes |
| FastAPI, Express, Fastify, NestJS | Segurança e cadastros precisariam ser montados manualmente |

## Consequências
- **Positivas:** `BigDecimal` e `java.time` nativos para dinheiro e datas; Spring Security
  entrega autenticação, sessões e CSRF prontos; biblioteca de WhatsApp madura; portfólio
  com Java e TypeScript.
- **Negativas:** duas linguagens, duas cadeias de dependências e duas imagens a manter e
  escanear; curva de aprendizado do Spring; maior consumo de memória da JVM (aceitável no
  hardware disponível).
