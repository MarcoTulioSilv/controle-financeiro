# Contrato Gateway → Core (API interna v1)

| Campo | Valor |
|---|---|
| Base | ADR-002 (captura plugável), ADR-003 (sem exposição), ERS v1.2 (RF07, RF07.1, RNF08, RNF13) |
| Situação | Aprovado |
| Rede | Somente a rede interna do Docker; o core não publica esta rota para fora |

## 1. Divisão de responsabilidades

| Responsabilidade | Gateway | Core |
|---|---|---|
| Conexão com o WhatsApp (Baileys) | ✅ | |
| Descartar mensagens `fromMe` e de grupos (características do protocolo) | ✅ | |
| Enviar mensagens em ordem, uma por vez por remetente (RNF08) | ✅ | |
| Reenviar em caso de falha (5xx ou sem resposta) | ✅ | |
| Verificar se o remetente é autorizado (RF07.1) | | ✅ |
| Limite de mensagens por remetente (RNF13) | | ✅ |
| Idempotência por `mensagemId` | | ✅ |
| Interpretar, validar, gravar e montar o texto de resposta | | ✅ |
| Anexar avisos (fatura, aprovação pendente, rascunho a completar) | | ✅ |

O gateway não conhece faturas, categorias nem usuários.

## 2. Requisição

```http
POST /interno/v1/mensagens
Authorization: Bearer <token do gateway>
Content-Type: application/json
```

```json
{
  "mensagemId": "3EB0C767D26A1D2E5F",
  "remetente": "+5564999999999",
  "texto": "mercado 150 @casa",
  "recebidaEm": "2026-10-05T14:32:10Z"
}
```

| Campo | Tipo | Regras |
|---|---|---|
| mensagemId | texto | Obrigatório; identificador único da mensagem no WhatsApp; até 100 caracteres |
| remetente | texto | Obrigatório; formato E.164 |
| texto | texto | Obrigatório; até 1.000 caracteres |
| recebidaEm | data-hora ISO 8601 (UTC) | Obrigatório; momento em que o WhatsApp entregou a mensagem |

## 3. Respostas

| Código | Situação | Ação do gateway |
|---|---|---|
| 200 | Mensagem processada (inclusive reenvio já processado) | Envia `resposta` ao remetente |
| 204 | Remetente não autorizado ou limite excedido | Não envia nada |
| 400 | Requisição malformada | Registra em log; não reenvia |
| 401 | Token inválido ou ausente | Registra em log como alerta; não reenvia |
| 5xx ou sem resposta | Falha do core | Reenvia a mesma requisição (mesmo `mensagemId`) com espera crescente |

Corpo da resposta 200:

```json
{ "resposta": "✅ Despesa · Alimentação · R$ 150,00 · @casa · compra 05/10 · adc out/26" }
```

## 4. Decisões

1. **Idempotência pelo `mensagemId`.** O core registra cada mensagem processada com a resposta enviada (`mensagem_processada`, modelo de dados, bloco C). Um `mensagemId` repetido recebe a mesma resposta, sem novo processamento. Protege contra reenvios do gateway e reentregas do WhatsApp após reconexão.
2. **Silêncio para não autorizados (204).** Responder a um número desconhecido confirmaria que ali existe um bot ativo, aumentaria o risco de banimento (ADR-001) e permitiria usar o bot para amplificar um ataque. Decisão reavaliada e mantida em 05/10/2026.
3. **Versão na URL (`/v1/`).** Mudanças incompatíveis criam `/v2/`, com convivência durante a transição.
4. **Token comparado em tempo constante** (`MessageDigest.isEqual`), contra ataques de temporização. O token fica no `.env` e tem permissão apenas para esta rota (ADR-002).
5. **Ordem garantida pelo gateway:** envio sequencial por remetente, aguardando a resposta da mensagem anterior.
