# Registros de Decisão de Arquitetura (ADRs)

Cada arquivo registra **uma** decisão: o contexto em que foi tomada, o que foi decidido,
as alternativas descartadas e as consequências.

Regras:
- Um ADR aceito e versionado **não é editado**. Se a decisão mudar, cria-se um novo ADR
  que o substitui, e o antigo recebe o status "Substituído por ADR-XXX".
- A numeração é sequencial e nunca é reaproveitada.
- Status possíveis: Proposto, Aceito, Substituído, Descartado.

| ADR | Decisão | Status |
|---|---|---|
| [001](ADR-001-canal-whatsapp.md) | Canal WhatsApp via biblioteca não oficial | Aceito |
| [002](ADR-002-captura-plugavel.md) | Camada de captura plugável (gateway sem regra de negócio) | Aceito |
| [003](ADR-003-sem-exposicao-internet.md) | Servidor sem exposição à internet | Aceito |
| [004](ADR-004-stack.md) | Stack: Java + Spring Boot, gateway TypeScript, MySQL 8 | Aceito |
| [005](ADR-005-valores-monetarios.md) | Valores monetários com BigDecimal e DECIMAL(12,2) | Aceito |
| [006](ADR-006-exclusao-logica.md) | Exclusão lógica com validação cruzada | Aceito |
