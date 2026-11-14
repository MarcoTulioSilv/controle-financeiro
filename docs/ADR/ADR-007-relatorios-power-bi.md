# ADR-007: Relatórios no Power BI Desktop sobre visões SQL

- **Status:** Aceito
- **Data:** 2026-11
- **Requisitos relacionados:** RF04, RF09, RF18, RNF01, RNF03

## Contexto
O Marco precisa substituir a planilha o quanto antes. Construir telas de relatório em
Thymeleaf (gráficos, filtros, comparações) é trabalho grande e adiaria o MVP. O Marco já
usa o Power BI e quer o dashboard como projeto de portfólio em análise de dados.

Foi avaliada e descartada a hospedagem em uma VM na nuvem (Oracle Cloud), pelo risco de
cobrança no modelo pago por uso: a aplicação continua no notebook (ADR-003).

## Decisão
- Os relatórios do RF09 são feitos no **Power BI Desktop**, no próprio notebook, lendo o
  MySQL em `127.0.0.1:3307`.
- O `core` expõe os dados de relatório como **visões SQL** (criadas por migrações Flyway),
  com a regra de cálculo nelas — o Power BI só agrega e desenha.
- O Power BI usa um **usuário MySQL somente leitura**, com `SELECT` apenas nas visões.
- O relatório **nunca** é publicado no serviço online do Power BI (os dados sairiam do
  notebook).
- A interface web do `core` fica restrita a lançamentos e cadastros no MVP 1.

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| Telas de relatório em Thymeleaf + biblioteca de gráficos | Mais lento para entregar; fica para depois, se fizer falta no celular |
| Power BI lendo as tabelas diretamente | Duplicaria regras (economia, reserva, vigência do orçamento) fora do código testado |
| Publicar no serviço online do Power BI | Envia dados financeiros para fora do notebook (ADR-003) |
| VM na nuvem com painel acessível de qualquer lugar | Risco de custo no modelo pago por uso e superfície exposta à internet |

## Consequências
- **Positivas:** MVP 1 mais rápido; regras de cálculo testadas no repositório (testes de
  integração das visões); portfólio de análise de dados.
- **Negativas:** relatórios só no notebook (Power BI Desktop só existe para Windows), não no
  celular; as visões viram um contrato: mudar uma coluna exige ajustar o `.pbix`.
