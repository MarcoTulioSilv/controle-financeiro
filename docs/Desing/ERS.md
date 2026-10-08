# Especificação de Requisitos de Software (ERS)
## Controle Financeiro Doméstico

| Campo | Valor |
|---|---|
| Versão | 1.2 |
| Data | 01/10/2026 |
| Autor | Marco Túlio Silva Oliveira |
| Referências de processo | SWEBOK 4.0 (KA Software Requirements); estrutura inspirada na ISO/IEC/IEEE 29148 |
| Status | Validado — linha de base dos requisitos. Alterações seguem o controle de mudanças (seção 12) |

---

## 1. Introdução

### 1.1 Propósito
Este documento consolida os requisitos da aplicação de controle financeiro doméstico, levantados na reunião inicial, na análise da planilha atualmente utilizada (`Orçamento_Agosto_2026.xlsx`) e nas sessões de refinamento posteriores. Ele serve de base para a arquitetura (ADRs), o design, a construção e os testes.

### 1.2 Escopo
A aplicação permite que os membros de um mesmo domicílio registrem receitas e despesas (inclusive por mensagem de WhatsApp), controlem cartões de crédito e suas faturas, acompanhem metas e orçamentos por categoria e acompanhem o patrimônio (ativos financeiros e bens físicos). Roda exclusivamente no computador pessoal do desenvolvedor, sem nenhuma porta exposta à internet.

### 1.3 Definições

| Termo | Definição |
|---|---|
| Domicílio | Grupo de usuários que compartilham contas, receitas e despesas |
| Lançamento | Registro de uma receita ou despesa |
| Data da compra | Dia em que o fato financeiro aconteceu |
| Mês de referência | Mês em que o lançamento pesa no orçamento. No bot, é exibido como `adc` |
| Fatura | Conjunto de compras de um cartão que vencem num mesmo mês |
| Para quem | Membro do domicílio (ou a casa) a quem o gasto se destina |
| Bem | Patrimônio físico: veículo, imóvel ou outro objeto de valor |
| Ativo financeiro | Ação ou criptomoeda com cotação de mercado |
| Validação cruzada | Regra pela qual uma ação sensível solicitada por um usuário só se efetiva após a aprovação de **outro** usuário do domicílio |
| Canal de captura | Meio pelo qual o dado bruto chega ao sistema (ex.: bot de WhatsApp) |
| Exclusão lógica | O registro é marcado como excluído, mas permanece no banco para auditoria |

### 1.4 Documentos relacionados

| Documento | Situação |
|---|---|
| ADR-001 — Canal WhatsApp via biblioteca não oficial | Aceito |
| ADR-002 — Camada de captura plugável | Aceito |
| ADR-003 — Servidor sem exposição à internet | Aceito |
| ADR-004 — Stack: núcleo Java + Spring Boot, gateway TypeScript + Baileys, MySQL 8 | Aceito |
| ADR-005 — Valores monetários com `BigDecimal` no código e `DECIMAL(12,2)` no banco | Aceito |
| ADR-006 — Exclusão lógica com validação cruzada | Aceito |

---

## 2. Descrição geral

### 2.1 Perspectiva do produto
Monorepo com dois aplicativos e um banco MySQL, executados via Docker Compose no notebook do usuário:

```
┌──────────────────────── Notebook (Docker Compose) ────────────────────────┐
│                                                                            │
│  [gateway]  TypeScript + Baileys ──HTTP interno + token──▶ [core]          │
│   só conexões de saída                                     Java + Spring   │
│   sem regra de negócio                                     Boot + Thymeleaf│
│                                                            + HTMX          │
│                         navegador (rede local) ──────────▶   │            │
│                                                               ▼            │
│                              [mysql]  127.0.0.1:3307  ◀── MySQL Workbench │
└────────────────────────────────────────────────────────────────────────────┘
```

- **core:** toda a regra de negócio — interpretador de mensagens, lançamentos, cartões, patrimônio, interface web e agendador de cotações.
- **gateway:** apenas mantém a conexão com o WhatsApp, repassa o texto bruto ao core e devolve ao WhatsApp a resposta produzida pelo core. Não interpreta mensagens nem acessa o banco.

### 2.2 Funções principais
1. Registrar receitas e despesas pela plataforma web ou pelo WhatsApp.
2. Controlar cartões de crédito, com cálculo automático da fatura e revisão no fechamento.
3. Controlar compras parceladas e dívidas.
4. Definir orçamento por categoria, despesas fixas e metas.
5. Acompanhar o patrimônio: ativos financeiros, bens físicos e dívidas.
6. Emitir relatórios de planejado × real, fluxo de caixa e evolução patrimonial.

### 2.3 Usuários

| Usuário | Perfil | Uso principal |
|---|---|---|
| Marco Túlio | Usuário e aprovador | Lançamentos, revisões, manutenção do sistema |
| Josane (Jô) | Usuária e aprovadora | Lançamentos pelo WhatsApp, consultas |

Todos os usuários veem e editam todos os dados do domicílio. Não existe um admin único: as ações que exigem autorização seguem a validação cruzada — o que um usuário solicita, o outro aprova.

### 2.4 Ambiente operacional

| Item | Especificação |
|---|---|
| Hardware | Notebook i5-11300H; SSD 500 GB (aplicação e banco); HD 500 GB (backups locais) |
| Sistema | Windows com Docker Desktop |
| Acesso | Navegador no próprio notebook ou em aparelhos na mesma rede Wi-Fi |
| Canal externo | Número de WhatsApp dedicado (chip pré-pago no WhatsApp Business) |

### 2.5 Restrições
- **R01:** Nenhuma porta do servidor pode ser acessível pela internet.
- **R02:** Stack definida no ADR-004: núcleo em Java 25 (LTS) com Spring Boot (Spring Security, Spring Data JPA, Flyway, Thymeleaf + HTMX); gateway em TypeScript sobre Node.js LTS com Baileys; MySQL 8.
- **R03:** Uso exclusivamente pessoal; sem requisitos de escala.

### 2.6 Premissas e dependências
- **D01:** O notebook precisa estar ligado para o bot processar mensagens; as mensagens recebidas com ele desligado são processadas na reconexão.
- **D02:** O chip pré-pago do bot precisa ser recarregado dentro do prazo da operadora.
- **D03:** O WhatsApp Business do número do bot precisa permanecer ativo no celular principal.
- **D04:** Cotações dependem de serviços externos (candidatos: brapi.dev para B3, CoinGecko para cripto, API da tabela FIPE para veículos). Limites e termos a confirmar em ADR.
- **D05:** A biblioteca Baileys não é oficial; o risco de banimento do número está aceito no ADR-001.

---

## 3. Requisitos funcionais

Prioridade (MoSCoW): **M** = Must (essencial), **S** = Should (importante), **C** = Could (desejável), **W** = Won't (fora desta versão).

### RF01 — Registrar lançamentos (M)
**Descrição:** Registrar receitas e despesas com: tipo, valor, descrição, categoria, data da compra, mês de referência, cartão (opcional), para quem e autor.
**Critérios de aceitação:**
- O mês de referência é, por padrão, o mês da data da compra.
- Se houver cartão, o mês de referência é calculado conforme RF14.
- O mês de referência pode ser informado manualmente e prevalece sobre o cálculo.
- O autor é registrado automaticamente a partir do usuário autenticado ou do número remetente.
**Origem:** Reunião inicial; planilha.

### RF02 — Gerenciar categorias (M)
**Descrição:** Cadastrar categorias de receita e de despesa, com palavras-chave associadas para uso pelo bot.
**Critérios de aceitação:**
- Categorias não carregam o nome de pessoas (ver RF13).
- Uma palavra-chave pertence a uma única categoria.
- Quando o bot não reconhece uma palavra, oferece as categorias em lista numerada e registra a nova associação.
- Categorias com lançamentos **nunca são excluídas**: são **desativadas**. Uma categoria inativa não aparece nas opções do bot nem nos formulários de novos lançamentos, mas continua nos lançamentos antigos e nos relatórios.
- Uma categoria pode ser renomeada; os lançamentos já existentes passam a exibir o novo nome.
**Origem:** Reunião inicial; planilha (ver Apêndice A).

### RF03 — Despesas e receitas fixas (M)
**Descrição:** Cadastrar lançamentos recorrentes (mensais ou anuais), que o sistema gera como previstos a cada período.
**Critérios de aceitação:**
- O lançamento previsto aparece no mês correspondente e é confirmado pelo usuário quando efetivado.
- A recarga do chip do bot é cadastrável como despesa fixa, servindo de lembrete.
**Origem:** Reunião inicial.

### RF04 — Orçamento e metas (S)
**Descrição:** Definir o valor planejado por categoria e mês, e metas de economia.
**Critérios de aceitação:**
- Exibir planejado × real × diferença por categoria, como na planilha atual.
- Permitir meta de economia como percentual da renda real do mês (a planilha usa 10%).
- A economia do mês é calculada como **(resultado do mês + compras de ativos − vendas de ativos) ÷ receitas do mês**, para que a troca de dinheiro por ativo não distorça a meta (RF17).
- Sinalizar categorias que atingirem 80% e 100% do planejado.
**Origem:** Reunião inicial; planilha.

### RF05 — Patrimônio (S)
**Descrição:** Controlar ativos financeiros e bens físicos, com histórico de valor.
**Critérios de aceitação:**
- Ações e criptomoedas: cadastro de código; quantidade e preço médio calculados a partir das compras e vendas (RF17); cotação atualizada automaticamente.
- Veículos: valor atualizado mensalmente pela tabela FIPE a partir do código do modelo.
- Imóveis: valor informado manualmente.
- Outros bens: depreciação linear a partir do valor de compra e da vida útil.
- Um bem pode ser vinculado a uma dívida (RF12).
- Exibir o patrimônio líquido: **reserva (RF18)** + ativos financeiros + bens − dívidas.
- Nas consultas externas, só saem códigos (ticker ou código FIPE), nunca quantidades ou valores do usuário.
**Origem:** Reunião inicial; refinamento (bens físicos como diferencial do sistema).

### RF06 — Domicílio com múltiplos usuários (M)
**Descrição:** Vários usuários pertencem a um mesmo domicílio e compartilham todos os dados.
**Critérios de aceitação:**
- Cada usuário tem login e senha próprios.
- Cada usuário está vinculado a um ou mais números de WhatsApp autorizados.
- Relatórios somam todos os usuários, com filtro por "para quem".
**Origem:** Reunião inicial.

### RF07 — Lançamento por WhatsApp (M)
**Descrição:** Receber mensagens em linguagem simples e transformá-las em lançamentos (formato na seção 4).
**Critérios de aceitação:**
- O bot responde a cada lançamento com uma confirmação curta.
- O bot só cria lançamentos; não exclui nem altera registros existentes.
- O bot nunca inicia conversas.
**Origem:** Reunião inicial; ADR-001.

### RF07.1 — Filtro de remetentes (M)
**Descrição:** O bot processa apenas mensagens recebidas de números autorizados.
**Critérios de aceitação:**
- Mensagens enviadas pelo próprio número do bot (`fromMe`) são ignoradas.
- Mensagens de números não autorizados são descartadas sem resposta.
- Mensagens de grupos são ignoradas.
- O cadastro de um novo número autorizado segue a **validação cruzada** (RN07): fica pendente até ser aprovado, com senha, por um usuário diferente de quem o cadastrou. Enquanto pendente, o número continua não autorizado.
**Origem:** Refinamento; STRIDE (spoofing); P07.

### RF08 — Integração bancária (W)
**Descrição:** Importação automática de transações bancárias. Adiada; ver seção 9.

### RF09 — Relatórios (S)
**Descrição:** Emitir relatórios de acompanhamento.
**Critérios de aceitação:**
- Resumo mensal: saldo inicial, receitas, despesas, saldo final e percentual de economia, como na planilha.
- O sistema controla apenas o **saldo do mês**, sem saldo por conta bancária (P05). O saldo inicial de um mês é a **reserva no início do mês** (RF18).
- Visão por mês de referência (padrão) e por data da compra.
- Planejado × real por categoria.
- Filtro por "para quem".
- Evolução do patrimônio líquido.
**Origem:** Planilha; refinamento.

### RF10 — Exclusão com validação cruzada (M)
**Descrição:** Todos editam tudo; uma exclusão solicitada por um usuário só se efetiva após a aprovação de outro usuário do domicílio.
**Critérios de aceitação:**
- A exclusão é lógica e registrada na auditoria (quem solicitou, quem aprovou, quando).
- Quem solicita uma exclusão **não pode aprová-la**, mesmo tendo acesso à tela de aprovação.
- O aprovador confirma com a própria senha na plataforma web (reautenticação), mesmo já estando logado.
- O aprovador pode recusar a solicitação; nesse caso o registro volta ao estado normal e a recusa fica na auditoria.
- O próprio solicitante pode cancelar uma solicitação ainda pendente; o cancelamento não altera nenhum dado e fica na auditoria.
- O comando `desfazer` do bot, quando não há rascunho aberto, marca o último lançamento do remetente como "exclusão pendente", aguardando a aprovação do outro usuário.
- Solicitações pendentes aparecem na tela inicial da plataforma para o aprovador. Como o bot nunca inicia conversas, o aviso também é anexado à próxima resposta do bot ao aprovador (ex.: `📋 1 exclusão aguardando sua aprovação`).
- Senhas nunca trafegam pelo WhatsApp.
**Origem:** Refinamento; ADR-006.

### RF11 — Importação da planilha atual (S)
**Descrição:** Importar o histórico da planilha `.xlsx` como carga inicial.
**Critérios de aceitação:**
- Categorias com nome de pessoa (ex.: "Moto MT") são convertidas em categoria + "para quem" (RF13).
- Parcelas descritas em texto (ex.: "11 de 16 celular") são sinalizadas para revisão manual.
- Datas fora de um intervalo razoável (ex.: 2025 numa planilha de 2026) são sinalizadas.
- Linhas sem pessoa identificável exigem a escolha do "para quem" na prévia, com opção de atribuição em lote (ex.: todas as linhas de Alimentação → Casa).
- Categorias da planilha que não correspondem a uma categoria ativa são mapeadas pelo usuário na prévia: para uma categoria ativa, para uma categoria inativa (histórico) ou para uma categoria renomeada (ver Apêndice A).
- A importação exibe uma prévia antes de gravar.
**Origem:** Análise da planilha.

### RF12 — Compras parceladas e dívidas (S)
**Descrição:** Controlar parcelamentos e dívidas como entidades próprias.
**Critérios de aceitação:**
- Uma compra parcelada é informada uma vez (ex.: `celular 16x 106,18 nupjmt`) e gera as parcelas nos meses corretos.
- Todas as parcelas, de compras parceladas e de financiamentos, são lançadas na categoria **Dívidas**. A descrição do item (ex.: "celular 3/16") é preservada em cada parcela.
- A categoria **Compras** é reservada para compras à vista.
- Uma dívida mantém o saldo devedor atualizado a cada parcela paga.
- Uma dívida pode ser vinculada a um bem (RF05).
**Origem:** Análise da planilha (categoria "Débito").

### RF13 — Campo "para quem" (M)
**Descrição:** Todo lançamento indica a quem se destina: um membro do domicílio ou a casa.
**Critérios de aceitação:**
- Substitui as categorias duplicadas por pessoa.
- O campo é **obrigatório e não tem valor padrão**: o sistema nunca supõe a quem o lançamento se destina.
- No bot, é indicado com `@mt`, `@jo` ou `@casa`. Se a mensagem não indicar, o bot pergunta antes de gravar (seção 4.4).
- Na plataforma web, o campo vem sem opção pré-selecionada.
**Origem:** Análise da planilha; P02.

### RF14 — Cartões de crédito (M)
**Descrição:** Cadastrar cartões com apelido, titular, dia de fechamento, dia de vencimento e a regra das compras feitas no dia do fechamento (fatura atual ou próxima). Cartões iniciais no Apêndice B.
**Critérios de aceitação:**
- O apelido do cartão funciona como palavra-chave no bot (ex.: `nupjmt`) e é único.
- O titular do cartão é independente do "para quem": um cartão do Marco pode pagar um gasto da casa.
- O mês de referência de uma compra é o mês do vencimento **nominal** da fatura em que ela cai (ver RN03).
- Quando o vencimento nominal cai em dia não útil (sábado, domingo ou feriado bancário), o sistema calcula o **vencimento efetivo** no próximo dia útil (Lei nº 7.089/1983). O vencimento efetivo é usado nos avisos de pagamento; o mês de referência continua o do vencimento nominal.
- Dias de fechamento inexistentes no mês (ex.: 30 em fevereiro) usam o último dia do mês.
- O pagamento da fatura não gera lançamento de despesa (RN01).
**Origem:** Refinamento.

### RF15 — Revisão de fatura (S)
**Descrição:** Cada fatura passa por revisão no fechamento, antes de ser confirmada.
**Critérios de aceitação:**
- Compras feitas no dia do fechamento ou na véspera recebem a marca "revisar".
- No dia do fechamento, a próxima resposta do bot a qualquer mensagem inclui o aviso da fatura; a tela inicial da plataforma também exibe o aviso.
- Na revisão é possível: mover compras entre faturas; ajustar a data de fechamento daquela fatura; informar o total exibido pelo banco e ver a diferença em relação à soma dos lançamentos.
- Após a confirmação, as compras da fatura ficam travadas.
- Estados da fatura: aberta → em revisão → conferida → paga.
**Origem:** Refinamento.

### RF16 — Calendário de dias não úteis (S)
**Descrição:** Manter o calendário de feriados bancários usado no cálculo do vencimento efetivo (RF14).
**Critérios de aceitação:**
- O calendário vem pré-carregado com os feriados nacionais, inclusive os móveis (ex.: Sexta-feira Santa), e com os dias sem expediente bancário definidos pela Febraban.
- O usuário pode cadastrar feriados locais (ex.: municipais de Jataí).
- Sábados e domingos são sempre dias não úteis.
**Origem:** P06.


### RF17 — Compra e venda de ativos (S)
**Descrição:** Registrar compras e vendas de ativos financeiros pela plataforma web.
**Critérios de aceitação:**
- A compra gera um lançamento de **despesa** na categoria Investimentos, que entra no resultado do mês, e aumenta a posição do ativo.
- A venda gera um lançamento de **receita** na categoria Investimentos, que entra no resultado do mês, e diminui a posição do ativo.
- A quantidade e o preço médio de cada ativo são calculados a partir das compras e vendas.
- A posição existente antes do uso do sistema é registrada como posição inicial, sem lançamento.
- Compras e vendas não alteram o patrimônio líquido no momento da operação: o valor sai da reserva e entra nos ativos (ou o inverso).
- O registro pelo bot fica fora desta versão.
**Origem:** Controle de mudanças (v1.1).

### RF18 — Reserva (M)
**Descrição:** Manter a reserva do domicílio, que acumula o resultado de todos os meses.
**Critérios de aceitação:**
- Reserva = ajustes manuais + soma dos resultados (receitas − despesas) de todos os meses até o mês atual, inclusive o mês em andamento.
- O superávit de um mês aumenta a reserva; o déficit a diminui, podendo deixá-la **negativa**.
- A reserva é **calculada**, nunca gravada: correções em meses passados se refletem automaticamente.
- Lançamentos com mês de referência futuro só afetam a reserva quando o mês deles chegar.
- Ajustes manuais (positivos ou negativos) registram data, valor e motivo; o primeiro ajuste corresponde ao saldo existente antes do uso do sistema (ou vem da importação da planilha, RF11).
**Origem:** Controle de mudanças (v1.1).

---

## 4. Interface do bot (formato das mensagens)

### 4.1 Formatos

| Tipo | Exemplo | Resultado |
|---|---|---|
| Despesa | `mercado 150` | Alimentação, R$ 150,00, hoje |
| Despesa com centavos | `gasolina moto 65,34` | Moto, R$ 65,34 |
| Receita | `+ pacote 100` | Receita Big Rock, R$ 100,00 |
| Para quem | `unha 90 @jo` | Pessoal, R$ 90,00, para Jô |
| Outra data | `gás 130 ontem` ou `gás 130 27/09` | Data da compra ajustada |
| Cartão | `mercado 250 nupjmt` | Mês de referência calculado pela fatura |
| Mês de referência manual | `mercado prox-mes 250` ou `mercado outubro 250` | Referência informada prevalece |
| Data + referência | `gás 27/09 outubro 130` | Compra em 27/09, referência out |
| Parcelado | `celular 16x 106,18 nupjmt` | 16 parcelas na categoria Dívidas, a partir da fatura calculada |
| Ajuda | `ajuda` | Lista os formatos |
| Desfazer | `desfazer` | Com rascunho aberto: descarta o rascunho. Sem rascunho: o último lançamento do remetente fica com exclusão pendente |

### 4.2 Regras de interpretação
1. A ordem das palavras é livre. O interpretador identifica: valor (último número da mensagem), data, mês de referência, cartão, pessoa (`@`), parcelamento (`Nx`) e prefixo de receita (`+`). O restante vira a descrição.
2. Valores aceitam vírgula ou ponto como separador decimal.
3. Palavras de mês de referência: `prox-mes`, `próximo mês`, nome por extenso (`outubro`), abreviado (`out`) ou com ano (`out/27`).
4. Mês sem ano indica a próxima ocorrência a partir do mês da compra.
5. A categoria é deduzida pelas palavras-chave (RF02).
6. A marcação de parcelamento (`Nx`) define a categoria como **Dívidas**, prevalecendo sobre as palavras-chave (RF12).
7. "Para quem" é obrigatório (RF13). Os exemplos da tabela 4.1 sem `@` disparam a pergunta de complemento (4.4).

### 4.3 Respostas do bot

```
✅ Despesa · Alimentação · R$ 250,00 · @casa · compra 21/09 · adc out/26
✅ Despesa · Alimentação · R$ 250,00 · @casa · compra 25/09 · adc nov/26 · ⚠️ revisar
📋 Fatura Nubank fecha hoje — 3 compras para revisar na plataforma
```

As respostas não exibem saldos, totais ou orçamentos (RNF12). O rótulo `adc` é usado **somente** nas respostas do bot; nos demais artefatos o termo é "mês de referência".

### 4.4 Complemento de informações

Quando falta uma informação obrigatória (para quem) ou o bot não reconhece a categoria (RF02), o lançamento vira um **rascunho** e o bot faz **uma pergunta por vez**:

```
Você:  mercado 150
Bot:   Para quem é "mercado R$ 150,00"?  1 Marco · 2 Jô · 3 Casa
Você:  3
Bot:   ✅ Despesa · Alimentação · R$ 150,00 · @casa · compra 01/10 · adc out/26
```

Regras:
1. A resposta aceita o número da opção ou a marcação (`3` ou `@casa`).
2. Cada remetente tem no máximo um rascunho aberto. Enquanto ele existir, uma mensagem que não seja resposta não é processada: o bot repete a pergunta, citando o rascunho, e o usuário reenvia a nova mensagem depois.
3. Um rascunho sem resposta por 30 minutos não é descartado: vai para a lista "lançamentos a completar" da plataforma web, e o aviso é anexado à próxima resposta do bot.
4. Se faltarem a categoria e o "para quem", as perguntas vêm em sequência, uma por mensagem.
5. O comando `desfazer` descarta o rascunho aberto. Descartar um rascunho não exige validação cruzada, pois nenhum lançamento foi criado. Rascunhos enviados à plataforma web também podem ser descartados lá.

---

## 5. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | O pagamento de fatura de cartão e as transferências entre contas bancárias do domicílio não são lançados: não são receita nem despesa (evita dupla contagem) |
| RN02 | O mês de referência não pode ser anterior ao mês da compra. Se estiver a mais de dois meses de distância, o bot pede confirmação |
| RN03 | Cálculo da fatura: o fechamento é a próxima data com o dia de fechamento a partir da compra (se a compra cair no dia do fechamento e a regra do cartão for "próxima", avança um ciclo); o vencimento nominal é a primeira data com o dia de vencimento após o fechamento; o mês de referência é o mês do vencimento nominal; o vencimento efetivo é o primeiro dia útil a partir do nominal (RF16) |
| RN04 | Todas as datas são tratadas no fuso America/Sao_Paulo |
| RN05 | O bot não exclui nem altera registros (RF07, RF10) |
| RN06 | Compras de uma fatura conferida não podem ser alteradas sem reabrir a fatura |
| RN07 | Validação cruzada: exclusões (que são lógicas) e o cadastro de novos números autorizados no bot só se efetivam com a aprovação de um usuário diferente do solicitante. Nenhuma outra ação exige aprovação |

---

## 6. Requisitos não funcionais

### 6.1 Segurança

| ID | Requisito |
|---|---|
| RNF01 | Nenhuma porta do servidor fica acessível pela internet. Conexões de saída só para destinos autorizados, levando o mínimo de dados |
| RNF02 | Autenticação individual via Spring Security, com senha armazenada por hash forte (BCrypt ou Argon2) |
| RNF03 | Disco do notebook criptografado (BitLocker ou criptografia do dispositivo); HTTPS na rede local com certificado local |
| RNF04 | Backup diário do banco no HD e cópia semanal criptografada em mídia externa, com teste de restauração periódico |
| RNF06 | Auditoria de criação, alteração e exclusão: autor, data, origem (web ou bot) e mensagem original |
| RNF11 | Dependências com versão fixada (Maven e npm) e lockfile do gateway versionado; apenas pacotes oficiais (ex.: Baileys do repositório WhiskeySockets); sem aliases de pacotes nem forks |
| RNF12 | Respostas do bot contêm apenas a confirmação do lançamento |
| RNF13 | Limite de mensagens processadas por remetente por minuto |
| RNF14 | Arquivos de sessão do WhatsApp tratados como segredo: volume no disco criptografado, nunca versionados |
| RNF15 | Pipeline com varredura de segredos, análise estática, auditoria de dependências e de imagem a cada commit |

### 6.2 Correção e confiabilidade

| ID | Requisito |
|---|---|
| RNF05 | Valores monetários como `BigDecimal` com escala 2 no código (nunca `double` ou `float`, e sempre construídos a partir de texto) e `DECIMAL(12,2)` no banco |
| RNF07 | Regras de data implementadas com `java.time` no fuso America/Sao_Paulo; mês de referência representado como `YearMonth`; proibido o uso de `java.util.Date` e `Calendar` |
| RNF08 | Mensagens recebidas com o notebook desligado são processadas na reconexão, na ordem de chegada |
| RNF10 | Cobertura de testes de no mínimo 90% no interpretador, no cálculo de faturas e na manipulação de valores (medida com JaCoCo) |

### 6.3 Usabilidade

| ID | Requisito |
|---|---|
| RNF09 | Interface web responsiva, utilizável no celular pela rede local |
| RNF16 | Um lançamento completo (com `@` e categoria conhecida) exige uma única mensagem; cada informação ausente acrescenta no máximo uma pergunta (4.4) |

---

## 7. Modelo de domínio (conceitual)

```
Domicilio        1──N Usuario (login, senha, números autorizados)
SolicitacaoAprovacao → ação, registro alvo, solicitante, aprovador, status (pendente | aprovada | recusada), datas
Rascunho         → remetente, mensagem original, campos já identificados, campo pendente, criado_em, status (aberto | completado | a completar na web)
Domicilio        1──N Categoria (tipo, palavras-chave)
Domicilio        1──N Cartao (apelido, titular, dia_fechamento, dia_vencimento, compra_no_fechamento)
Cartao           1──N Fatura (mes_referencia, data_fechamento, data_vencimento, data_vencimento_efetiva, status, total_informado_banco)
Feriado          → data, descrição, abrangência (nacional | local)
Lancamento       → tipo, valor, descricao, Categoria, data_compra, mes_referencia,
                   para_quem, autor, origem, mensagem_original, revisar,
                   Cartao?, Fatura?, CompraParcelada?, status_exclusao
CompraParcelada  1──N Lancamento (parcelas)
Recorrencia      → gera Lancamentos previstos
Orcamento        → Categoria + mês + valor planejado
MetaEconomia     → percentual da renda
Divida           → saldo devedor, parcelas, Bem?
Bem              → tipo, método de avaliação (FIPE | manual | depreciação)
Bem              1──N AvaliacaoDeValor (data, valor, origem)
AtivoFinanceiro  → código (quantidade e preço médio calculados)
AtivoFinanceiro  1──N MovimentacaoAtivo (compra | venda | posição inicial; Lancamento?)
AjusteReserva    → data, valor, motivo
AtivoFinanceiro  1──N Cotacao (data, preço)
Auditoria        → entidade, ação, usuário, data, detalhes
```

---

## 8. Riscos e ameaças

### 8.1 STRIDE do bot

| Ameaça | Cenário | Mitigação |
|---|---|---|
| Spoofing | Desconhecido envia lançamentos | RF07.1 |
| Tampering | Texto malicioso tenta injeção | Validação no interpretador; ORM com consultas parametrizadas |
| Repudiation | Autor nega o lançamento | RNF06 |
| Information disclosure | Respostas ou sessão vazam dados | RNF12, RNF14 |
| Denial of service | Inundação de mensagens | RF07.1, RNF13 |
| Elevation of privilege | Exclusão pelo bot | RN05, RF10 |

### 8.2 Riscos operacionais

| Risco | Impacto | Mitigação |
|---|---|---|
| Banimento do número do bot | Canal indisponível | Canal isolado (ADR-002); alternativas na seção 9 |
| Cancelamento do chip | Perda do número | Recarga cadastrada como despesa fixa (RF03) |
| Roubo ou falha do notebook | Perda ou vazamento de dados | RNF03, RNF04 |
| Mudança no protocolo do WhatsApp | Bot para de funcionar | Dependabot; atualização da biblioteca |
| Mudança nas APIs de cotação | Valores desatualizados | Última cotação válida mantida, com data exibida |

---

## 9. Fora do escopo desta versão

| Item | Observação |
|---|---|
| Integração bancária (RF08) | Open Finance restrito a instituições autorizadas; agregadores enviam dados a terceiros |
| Acesso de fora de casa | Futuro: VPN (Tailscale ou WireGuard), sem porta pública |
| Canais alternativos de captura | PWA offline, Telegram, exportação de conversa, notificações do banco, QR code da NFC-e — possíveis graças ao ADR-002 |
| Importação de extrato OFX/CSV | Candidata a conferência mensal |

---

## 10. Rastreabilidade

| Requisito | ADR | Módulo previsto | Testes |
|---|---|---|---|
| RF01, RN02 | ADR-005 | Lançamentos | A definir |
| RF07, RF07.1, RN05 | ADR-001, ADR-002 | gateway (canal) + core/interpretador | A definir |
| RF10, RN07 | ADR-006 | Lançamentos / Auditoria | A definir |
| RF14, RN03 | — | Cartões e faturas | Casos de valor limite (seção 3, RF14) |
| RNF01 | ADR-003 | Infraestrutura | A definir |
| RF17, RF18 | ADR-005 | Patrimônio / Reserva | A definir |
| RNF05 | ADR-005 | Núcleo (valores) | A definir |
| Todos | ADR-004 | — | — |

---

## 11. Pendências para validação

| ID | Pendência |
|---|---|
| P01 | ~~Quem é admin além de Marco Túlio?~~ **Resolvida (v0.3):** não há admin único; um usuário valida as ações do outro (RF10, RN07) |
| P02 | ~~Qual o valor padrão de "para quem"?~~ **Resolvida (v0.4):** não há valor padrão; o campo é obrigatório e o bot pergunta quando não for informado (RF13, seção 4.4) |
| P03 | ~~A lista inicial de categorias (Apêndice A) está correta?~~ **Resolvida (v0.8):** lista validada pelo usuário |
| P04 | ~~Parcelas vão para a categoria do item ou para dívidas?~~ **Resolvida (v0.9):** todas as parcelas vão para Dívidas; Compras fica para compras à vista (RF12, regra 4.2.6) |
| P05 | ~~Saldo por conta bancária ou só saldo do mês?~~ **Resolvida (v0.10):** apenas o saldo do mês, como na planilha (RF09) |
| P06 | ~~Quais cartões serão cadastrados?~~ **Resolvida (v0.11):** cinco cartões, todos com a regra "próxima" (Apêndice B); vencimento em dia não útil passa ao próximo dia útil (RF14, RF16) |
| P07 | ~~Além das exclusões, quais ações exigem validação cruzada?~~ **Resolvida (v0.12):** apenas o cadastro de novo número autorizado no bot (RF07.1, RN07) |

---

## 12. Controle de mudanças

A partir da versão 1.0, este documento é a **linha de base** (*baseline*) dos requisitos:
1. Toda mudança começa como uma solicitação registrada (issue no GitHub), com o motivo.
2. Analisa-se o impacto: requisitos, ADRs, código e testes afetados (seção 10).
3. Aprovada a mudança, o ERS recebe nova versão (1.1, 1.2…) e uma linha no histórico.
4. Mudanças que alterem uma decisão de arquitetura geram um novo ADR.

---

## Apêndice A — Categorias iniciais (derivadas da planilha)

**Despesas:** Alimentação, Compras, Saúde/Farmácia, Internet, Moto, Pessoal, Animais de estimação, Academia, Viagens, Carro, Lazer, Casa, Presentes, Operacional, Dívidas, Investimentos.

- "Moto MT", "Moto Jô", "Pessoal MT" e "Pessoal Jô" viram "Moto" e "Pessoal" com o campo "para quem".
- "Débito" é renomeada para **Dívidas** e recebe todas as parcelas de compras parceladas e financiamentos (RF12). "Compras" fica reservada para compras à vista.
- "Operacional" demarca os custos de desenvolvimento e manutenção de projetos de software: assinaturas de ferramentas (ex.: Claude), armazenamento, hospedagem, domínios. Palavras-chave iniciais: `claude`, `hospedagem`, `dominio`, `armazenamento`, `servidor`. Por serem em sua maioria assinaturas, devem ser cadastrados como despesas fixas (RF03).

**Receitas:** Big Rock, Rádio, Clínica, Quíron Equi, Bicos, Outros, iFood, Investimentos.

**Investimentos** existe como categoria de despesa (compras de ativos) e de receita (vendas de ativos), conforme o RF17.

- "Bicos Josane" vira "Bicos" com "para quem" = Jô.
- "Projeto veterinário" é o mesmo projeto, agora com o nome definido: seus lançamentos são importados como **Quíron Equi**.
- **Carvalhos** (empresa em que Marco Túlio trabalhava) é criada como categoria **inativa**: o histórico é preservado, mas ela não recebe novos lançamentos.

---

## Apêndice B — Cartões iniciais

| Apelido no bot | Cartão | Titular | Fechamento | Vencimento | Compra no dia do fechamento |
|---|---|---|---|---|---|
| `mpmt` | Mercado Pago | Marco | 18 | 23 | Próxima fatura |
| `mpjo` | Mercado Pago | Jô | 11 | 16 | Próxima fatura |
| `nupjmt` | Nubank | Marco | 01 | 08 | Próxima fatura |
| `bbmt` | Banco do Brasil | Marco | 17 | 28 | Próxima fatura |
| `bbjo` | Banco do Brasil | Jô | 17 | 28 | Próxima fatura |

Observações:
- **Nubank:** política oficial — compras feitas até a véspera do fechamento entram na fatura aberta; o fechamento ocorre sempre 7 dias corridos antes do vencimento.
- **Mercado Pago e Banco do Brasil:** sem regra oficial explícita; compras no dia do fechamento podem cair em qualquer das faturas conforme o horário de processamento. A regra "próxima" é a hipótese inicial, e as compras desse dia são sempre conferidas na revisão (RF15).

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 24/09/2026 | Consolidação da reunião inicial, análise da planilha e sessões de refinamento |
| 0.2 | 01/10/2026 | Revisão do ADR-004: núcleo em Java + Spring Boot e gateway em TypeScript; ajustes em 2.1, R02, RNF02, RNF05, RNF07, RNF10, RNF11 e na rastreabilidade |
| 0.3 | 01/10/2026 | P01 resolvida: admin único substituído por validação cruzada; ajustes em 1.3, 2.3, RF10, RN07, modelo de domínio; nova pendência P07 |
| 0.4 | 01/10/2026 | P02 resolvida: "para quem" obrigatório, sem valor padrão; nova seção 4.4 (complemento de informações pelo bot); ajustes em RF11, RF13, RNF16 e nas respostas do bot |
| 0.5 | 01/10/2026 | Apêndice A: novas categorias de despesa Presentes e Operacional (custos de desenvolvimento) |
| 0.6 | 01/10/2026 | Apêndice A: categorias de receita revisadas (saem Carvalhos e Projeto veterinário; entram Rádio e Quíron Equi); RF11 passa a prever o mapeamento de categorias antigas |
| 0.7 | 01/10/2026 | RF02: categorias são desativadas, nunca excluídas, e podem ser renomeadas; Carvalhos como categoria inativa; Projeto veterinário importado como Quíron Equi; ajuste no RF11 |
| 0.8 | 01/10/2026 | P03 resolvida: lista de categorias do Apêndice A validada |
| 0.9 | 01/10/2026 | P04 resolvida: parcelas na categoria Dívidas (antiga Débito); Compras reservada para compras à vista; ajustes em RF12, 4.1, 4.2 e Apêndice A |
| 0.10 | 01/10/2026 | P05 resolvida: apenas saldo do mês, sem saldo por conta bancária; ajustes em RF09 e RN01 |
| 0.11 | 01/10/2026 | P06 resolvida: cinco cartões (Apêndice B); RF14 com titular, vencimento nominal e efetivo; novo RF16 (calendário de dias não úteis); RN03 e modelo de domínio ajustados |
| 0.12 | 01/10/2026 | P07 resolvida: validação cruzada estendida apenas ao cadastro de novos números autorizados; ajustes em RF07.1 e RN07 |
| 1.0 | 01/10/2026 | Validação concluída: titular e apelidos dos cartões confirmados (Nubank → `nupjmt`, titular Marco); vencimento nominal aprovado para o mês de referência; nova seção 12 (controle de mudanças). Linha de base dos requisitos |
| 1.1 | 04/10/2026 | Controle de mudanças (issue "tratamento de aportes em ativos e destino do resultado mensal"): novos RF17 (compra e venda de ativos) e RF18 (reserva calculada, podendo ser negativa); ajustes em RF04 (fórmula da economia), RF05 (patrimônio inclui a reserva), RF09 (saldo inicial = reserva), modelo conceitual e Apêndice A (categoria Investimentos) |
| 1.2 | 05/10/2026 | Controle de mudanças (máquinas de estado): `desfazer` descarta o rascunho aberto (4.1, 4.4); solicitante pode cancelar solicitação pendente (RF10) |
