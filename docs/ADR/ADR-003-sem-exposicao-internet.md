# ADR-003: Servidor sem exposição à internet

- **Status:** Aceito
- **Data:** 2026-09
- **Requisitos relacionados:** RNF01, RNF03, RNF12, R01

## Contexto
A aplicação guarda dados financeiros de uma família e roda no notebook pessoal do
desenvolvedor. Expor qualquer porta à internet transformaria o notebook em alvo de
varreduras e ataques automatizados. Ainda assim, três funções precisam de rede: o bot de
WhatsApp, as cotações de ativos e a tabela FIPE.

## Decisão
Distinguir **conexões de entrada** (proibidas a partir da internet) de **conexões de saída**
(permitidas para destinos autorizados, levando o mínimo de dados):

| Serviço | Porta publicada | Conexões de saída |
|---|---|---|
| core (interface web) | Somente no IP da rede local | APIs de cotação e FIPE: envia apenas códigos de ativos e de modelos |
| gateway | Nenhuma | Servidores do WhatsApp |
| mysql | Somente `127.0.0.1:3307` | Nenhuma |

- O roteador doméstico não terá redirecionamento de portas para o notebook.
- Acesso de fora de casa, se um dia for desejado, será por VPN (Tailscale ou WireGuard),
  em novo ADR — nunca por porta pública.

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| Expor a aplicação com HTTPS e senha | Superfície de ataque permanente para um uso que não exige acesso externo |
| Túnel reverso (ex.: Cloudflare Tunnel) | Desnecessário com a decisão do ADR-001 |
| Nenhuma conexão de saída | Inviabilizaria o bot e as cotações automáticas |

## Consequências
- **Positivas:** a aplicação não é alcançável pela internet; o risco se concentra no
  conteúdo das conexões de saída, que é controlado.
- **Negativas:** consulta aos dados só pela rede de casa; o texto das mensagens do bot
  passa pelos servidores da Meta (inevitável em qualquer bot de mensagens).
