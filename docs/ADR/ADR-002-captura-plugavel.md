# ADR-002: Camada de captura plugável

- **Status:** Aceito
- **Data:** 2026-09
- **Requisitos relacionados:** RF07, RF10; riscos da seção 8 do ERS

## Contexto
O canal WhatsApp tem risco real de deixar de funcionar (ADR-001). Outros canais foram
levantados como alternativas ou complementos: Telegram, PWA offline, exportação de conversa,
notificações do banco e QR code da NFC-e. Todos fazem a mesma coisa: entregam um texto ou
dado bruto que precisa ser interpretado, validado e gravado.

## Decisão
Separar **captura** de **processamento**:

- Os canais (hoje, só o gateway WhatsApp) apenas recebem o dado bruto, enviam ao core e
  devolvem a resposta do core ao usuário. **Não contêm regra de negócio** e não acessam o banco.
- O core recebe o texto bruto num endpoint interno, junto com a identificação do canal e do
  remetente; interpreta, valida, grava e devolve o texto de resposta.
- O gateway se autentica no core com um token de **permissão mínima**: só pode enviar
  mensagens para interpretação. Não pode consultar, alterar nem excluir dados.

```
Canal (gateway)  ──texto bruto + remetente──▶  core: interpretador → validação → gravação
                 ◀──────texto de resposta─────
```

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| Interpretar as mensagens no gateway | Regra de negócio duplicada a cada novo canal; código em TypeScript sem `BigDecimal` |
| Gateway gravando direto no banco | Ignora as validações e a auditoria do core; amplia o acesso de um componente exposto |

## Consequências
- **Positivas:** trocar ou adicionar um canal não toca no núcleo; o interpretador pode ser
  testado sem WhatsApp; um gateway comprometido só consegue criar lançamentos, que ficam
  registrados na auditoria.
- **Negativas:** um salto de rede a mais por mensagem (irrelevante para dois usuários);
  um contrato HTTP interno a versionar.
