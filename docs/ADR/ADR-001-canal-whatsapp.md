# ADR-001: Canal de entrada via WhatsApp com biblioteca não oficial

- **Status:** Aceito
- **Data:** 2026-09
- **Requisitos relacionados:** RF07, RF07.1, RNF01, RNF14

## Contexto
O principal desejo dos usuários é registrar um lançamento com uma única mensagem, pelo
aplicativo que já usam o dia todo: o WhatsApp. Ao mesmo tempo, o servidor (notebook pessoal)
não pode ter nenhuma porta acessível pela internet (ADR-003).

A API oficial (WhatsApp Cloud API) entrega as mensagens por webhook, o que exige um endereço
HTTPS público. Isso é incompatível com o ADR-003.

## Decisão
Usar a biblioteca não oficial **Baileys**, que se conecta ao WhatsApp como um dispositivo
conectado ("WhatsApp Web") e só realiza conexões de saída.

- Número dedicado: chip pré-pago registrado no WhatsApp Business, no celular do dia a dia.
- Confirmação em duas etapas ativada no número do bot.
- O bot só responde a números autorizados; nunca inicia conversas.

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| WhatsApp Cloud API (oficial) | Exige webhook público |
| Bot do Telegram | Atende à restrição de rede, mas não é o aplicativo que os usuários usam |
| Ler um grupo pelo WhatsApp Web (extensão ou exportação) | Sem retorno imediato ao usuário; sincronização manual |
| Celular dedicado | Desnecessário: o WhatsApp Business no celular atual cumpre o papel |

## Consequências
- **Positivas:** lançamento com uma mensagem; nenhuma porta aberta.
- **Negativas:** viola os termos de uso do WhatsApp (**risco de banimento aceito**);
  dependência de uma biblioteca que acompanha mudanças de protocolo; o chip precisa de recarga
  periódica; os arquivos de sessão passam a ser segredos a proteger.
- **Mitigação:** o canal é isolado (ADR-002), permitindo trocar por Telegram ou outro canal
  sem alterar o núcleo.
