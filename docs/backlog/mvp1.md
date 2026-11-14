# Backlog — MVP 1: substituir a planilha

Objetivo do MVP 1: parar de usar a planilha. Lançamentos pelo formulário web local, cartões com
cálculo de fatura, parcelamentos, orçamento e reserva, importação do histórico da planilha e
relatórios no Power BI Desktop (ADR-007). O bot de WhatsApp fica para o MVP 2.

Fora do MVP 1 (não implementar): exclusão de lançamentos e validação cruzada (RF10), revisão de
fatura (RF15), recorrências (RF03), patrimônio (RF05, RF17), bot e gateway (RF07), auditoria
com Hibernate Envers (RNF06, além das colunas da convenção C7).

## Regras deste arquivo

- Uma tarefa = uma branch (`<tipo>/Txx-descricao-curta`) = um pull request.
- **Status:** `[ ] a fazer` → `[x] concluída`. O implementador marca **apenas a própria tarefa**
  como concluída, na própria branch: a tarefa conta como concluída quando a branch é mergeada
  na `main`. Uma tarefa com branch enviada ao GitHub e ainda não mergeada está "em revisão".
- **Só comece uma tarefa quando todas as dependências estiverem concluídas na `main`.**
- **No máximo duas tarefas em paralelo** (a revisão do Marco é o gargalo).
- Os critérios de aceitação são o contrato: nada fica de fora, e nada além deles entra na
  tarefa. Ideias extras vão para o relatório, como sugestão.
- Dados de exemplo e de teste usam **somente** os nomes fictícios do ERS (Marco, Ana, Banco A,
  Trabalho A...). Dados reais vivem em `config/dados-iniciais.yml`, que não é versionado.

## Ordem sugerida (ondas de até duas tarefas)

| Onda | Tarefas |
|---|---|
| 1 | T01 ∥ T02 |
| 2 | T03 ∥ T04 |
| 3 | T08 ∥ T15 |
| 4 | T05 |
| 5 | T06 |
| 6 | T07 ∥ T10 |
| 7 | T09 ∥ T11 |
| 8 | T12 ∥ T14 |
| — | T13 (Marco, sem agente, depois da T11) |

---

## T01 — Fundação de persistência
- Status: [ ] a fazer
- Tipo: chore · Branch: `chore/T01-fundacao-persistencia`
- Depende de: —
- Objetivo: preparar o `core` para usar o MySQL, com migrações e testes de integração.
- Critérios de aceitação:
  - [ ] Dependências: Spring Data JPA, driver MySQL, Flyway (com `flyway-mysql`) e Testcontainers. Versões gerenciadas pelo Spring Boot sempre que possível; as demais, fixas.
  - [ ] `application.yaml` lê host, porta, banco, usuário e senha de variáveis de ambiente; nenhuma senha no repositório. Um perfil `local` aponta para `127.0.0.1:3307`.
  - [ ] `spring.jpa.hibernate.ddl-auto=validate` (o schema vem só do Flyway) e `spring.jpa.open-in-view=false`.
  - [ ] Datas e horas: o Hibernate grava momentos em UTC (C5); a aplicação usa `America/Sao_Paulo` nas regras de negócio.
  - [ ] `EntidadeAuditavel` (`@MappedSuperclass`, pacote `compartilhado`) com `criado_em`, `criado_por`, `atualizado_em`, `atualizado_por` e `versao` (C7), usando a auditoria do Spring Data. Até a T08, o autor é `"sistema"`.
  - [ ] Conversor JPA `YearMonth` ↔ `DATE` com dia 1 (C5), com testes.
  - [ ] Os testes que sobem o contexto do Spring usam Testcontainers com MySQL 8.4 (`@ServiceConnection`), sem depender do MySQL local; passam no pipeline.
  - [ ] README: seção "Rodando localmente" (variáveis do `.env`, perfil `local`).
- Referências: ADR-004, ADR-005; `docs/design/modelo-dados.md` (convenções C1–C8).

## T02 — CalculadoraFatura completa
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T02-calculadora-fatura`
- Depende de: a branch `feat/calculadora-fatura` (casos 1 e 4) mergeada na `main`.
- Objetivo: completar a RN03 com TDD.
- Critérios de aceitação (cartão que fecha no dia 25 e vence no dia 5, salvo indicação):
  - [ ] Caso 2: compra em 25/09/2026, regra ATUAL → referência out/2026.
  - [ ] Caso 3: compra em 25/09/2026, regra PROXIMA → referência nov/2026.
  - [ ] Caso 5: compra em 26/12/2026 → referência fev/2027 (virada de ano).
  - [ ] Caso 6: cartão que fecha no dia 30 e vence no dia 5; compra em 28/02/2027, regra ATUAL → fechamento 28/02/2027, referência mar/2027 (dia inexistente usa o último dia do mês, RF14).
  - [ ] Caso 7: cartão que fecha no dia 3 e vence no dia 10; compra em 21/09/2026 → fechamento 03/10/2026, vencimento 10/10/2026, referência out/2026.
  - [ ] Dia de vencimento inexistente no mês também usa o último dia do mês, com teste.
  - [ ] `RegraCartao` rejeita dias fora de 1–31 e regra nula, com testes.
  - [ ] Atenção: não encadear `plusMonths` a partir de uma data já ajustada ao fim do mês (31/01 → 28/02 → 28/03 perde o dia 31). Recalcule a partir do dia configurado a cada mês, com teste que prove isso.
  - [ ] A classe continua pura (sem Spring, sem JPA).
- Referências: ERS RF14, RN03; `docs/design/interpretador.md`, seção 6.

## T03 — CalendarioBancario (parte pura)
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T03-calendario-bancario`
- Depende de: —
- Objetivo: calcular o vencimento efetivo (próximo dia útil).
- Critérios de aceitação:
  - [ ] `proximoDiaUtil(LocalDate, Set<LocalDate> feriados)`: sábado, domingo e feriado avançam para o próximo dia útil; dia útil devolve a própria data. Testes com fim de semana, feriado na segunda e feriados emendados.
  - [ ] `feriadosNacionais(int ano)`: feriados nacionais fixos e móveis (a Páscoa calculada por algoritmo, e os dependentes dela) considerados dias sem expediente bancário. O relatório cita a fonte oficial usada (lei ou calendário da Febraban), e os testes verificam pelo menos dois anos contra essa fonte.
  - [ ] Pacote `cartao`, classe pura (sem Spring, sem JPA).
- Referências: ERS RF14, RF16, RN03.

## T04 — Cadastros básicos e dados iniciais
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T04-cadastros-basicos`
- Depende de: T01
- Objetivo: destinatários, categorias e palavras-chave, com carga de dados iniciais local.
- Critérios de aceitação:
  - [ ] Migrações de `destinatario`, `categoria` e `palavra_chave`, fiéis ao `modelo-dados.md` (tipos, `CHECK`s, `UNIQUE`, charset e collation).
  - [ ] Entidades e repositórios **sem** `public`; um serviço público por assunto (M5).
  - [ ] Categoria é desativada, nunca excluída (RF02).
  - [ ] Carga de dados iniciais a partir de `config/dados-iniciais.yml` (não versionado; entra no `.gitignore`), idempotente: rodar duas vezes não duplica nada. Arquivo de exemplo versionado: `config/dados-iniciais.example.yml`, com os nomes fictícios do ERS (Apêndice A).
  - [ ] Testes de integração das restrições, inclusive a collation: `farmacia` e `Farmácia` são a mesma palavra-chave.
- Referências: ERS RF02, RF13, Apêndice A; `modelo-dados.md`, bloco A.

## T05 — Cartões, faturas e feriados
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T05-cartoes-faturas`
- Depende de: T02, T03, T08
- Objetivo: persistir cartões e faturas, com o mês de referência calculado.
- Critérios de aceitação:
  - [ ] Migrações de `cartao`, `fatura` e `feriado`, fiéis ao `modelo-dados.md`, bloco B.
  - [ ] Cartões carregados de `config/dados-iniciais.yml` (exemplo com Banco A/B/C e dias fictícios do Apêndice B).
  - [ ] Feriados nacionais do ano corrente e do seguinte gravados a partir do `CalendarioBancario`, de forma idempotente; feriados locais vêm do arquivo de dados iniciais.
  - [ ] Serviço público que, dado um cartão e uma data de compra, devolve a fatura (criando-a se não existir), com vencimento nominal e efetivo.
  - [ ] Criação concorrente da mesma fatura: o `UNIQUE (cartao_id, mes_referencia)` vence, e o serviço reaproveita a fatura existente. Teste de integração que simula a corrida.
  - [ ] No MVP 1, toda fatura nasce e permanece `ABERTA` (a revisão é do RF15, fora do escopo).
  - [ ] Validação: o apelido do cartão não pode coincidir com palavra-chave nem com marcação de destinatário (regra na aplicação, com teste).
- Referências: ERS RF14, RF16, RN03; `modelo-dados.md`, bloco B.

## T06 — Lançamentos
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T06-lancamentos`
- Depende de: T05
- Objetivo: registrar e editar receitas e despesas.
- Critérios de aceitação:
  - [ ] Migração de `lancamento` (blocos A e B, exceto as colunas de dívida e recorrência), com índices e `CHECK`s do modelo.
  - [ ] Serviço de registro: valor `BigDecimal` > 0, categoria coerente com o tipo, "para quem" obrigatório (RF13), origem `WEB` ou `IMPORTACAO`.
  - [ ] Mês de referência: sem cartão, o mês da compra; com cartão, o da fatura (T05); informado manualmente, prevalece. Nunca anterior ao mês da compra (RN02).
  - [ ] Edição permitida; nenhuma exclusão no MVP 1.
  - [ ] Testes unitários das regras e de integração da gravação.
- Referências: ERS RF01, RF13, RN02; `modelo-dados.md`, blocos A e B.

## T07 — Dívidas e parcelamento
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T07-dividas-parcelamento`
- Depende de: T06
- Objetivo: compras parceladas e financiamentos (RF12).
- Critérios de aceitação:
  - [ ] Migração de `divida` e das colunas `divida_id` e `numero_parcela` em `lancamento`, com os `CHECK`s e o `UNIQUE (divida_id, numero_parcela)` do modelo.
  - [ ] Parcelamento informado pelo valor da parcela: total = parcela × quantidade. Informado pelo total: divisão com `HALF_EVEN` e a diferença na primeira parcela (ADR-005). Teste: a soma das parcelas é **exatamente** o total.
  - [ ] Todas as parcelas na categoria Dívidas; a primeira na fatura calculada, as demais nos meses seguintes.
  - [ ] Saldo devedor calculado, nunca gravado.
- Referências: ERS RF12; ADR-005; `modelo-dados.md`, bloco B; `maquinas-estado.md`, seção 2 (só o estado ATIVA no MVP 1).

## T08 — Login e segurança web
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T08-login-seguranca`
- Depende de: T04
- Objetivo: acesso autenticado à interface web.
- Critérios de aceitação:
  - [ ] Migração de `usuario`, fiel ao modelo (bloco A).
  - [ ] Usuários vêm de `config/dados-iniciais.yml`; a senha inicial é convertida em hash forte (Spring Security) na carga e nunca gravada em texto. Página para o usuário trocar a própria senha.
  - [ ] Login por formulário, logout, CSRF ativo, cookie de sessão `HttpOnly` e `SameSite`.
  - [ ] Bloqueio temporário após tentativas seguidas de senha errada, com teste.
  - [ ] O autor da auditoria (C7) passa a ser o usuário logado.
- Referências: ERS RF06, RNF02; ADR-004.

## T09 — Tela de lançamentos
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T09-tela-lancamentos`
- Depende de: T06, T07, T08
- Objetivo: lançar e editar pelo celular, na rede de casa.
- Critérios de aceitação:
  - [ ] Thymeleaf + HTMX, pensado primeiro para tela de celular. O HTMX é servido pela própria aplicação, em versão fixa (sem CDN).
  - [ ] Formulário: tipo, valor (aceita `1.234,56`), descrição, categoria, para quem (sem valor pré-selecionado), data da compra, cartão (opcional), parcelas (opcional) e mês de referência manual (opcional).
  - [ ] Lista do mês de referência, com edição; sem exclusão.
  - [ ] Mensagens de validação em português.
  - [ ] Testes da camada web (controle de acesso, CSRF e validação).
- Referências: ERS RF01, RF12, RF13, RNF09.

## T10 — Orçamento, meta e reserva (com visões para relatórios)
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T10-orcamento-reserva`
- Depende de: T06
- Objetivo: o planejado e a reserva, prontos para o Power BI.
- Critérios de aceitação:
  - [ ] Migrações de `orcamento`, `meta_economia` e `ajuste_reserva` (bloco D; recorrência fica fora).
  - [ ] Orçamento e meta iniciais carregados de `config/dados-iniciais.yml`.
  - [ ] Visões SQL (migrações Flyway) para o Power BI: lançamentos com nomes legíveis; resumo mensal (receitas, despesas, resultado, % de economia pela fórmula do RF04); planejado × real por categoria, respeitando a vigência; reserva acumulada por mês (RF18).
  - [ ] Testes de integração das visões, com dados fictícios e resultados calculados à mão.
- Referências: ERS RF04, RF09, RF18; ADR-007; `modelo-dados.md`, bloco D.

## T11 — Acesso do Power BI
- Status: [ ] a fazer
- Tipo: chore · Branch: `chore/T11-acesso-power-bi`
- Depende de: T10
- Objetivo: o Power BI lê as visões com o menor privilégio possível.
- Critérios de aceitação:
  - [ ] Script `scripts/mysql/criar-usuario-leitura.sql`, executado pelo Marco com o usuário `root`: cria o usuário de leitura com `SELECT` **apenas** nas visões. A senha não fica no script.
  - [ ] `docs/powerbi.md`: driver necessário, conexão em `127.0.0.1:3307`, uso exclusivo do Power BI Desktop (nunca publicar no serviço online), e o significado de cada visão.
- Referências: ADR-003, ADR-007.

## T12 — Importação da planilha
- Status: [ ] a fazer
- Tipo: feat · Branch: `feat/T12-importacao-planilha`
- Depende de: T07, T09, T10
- Objetivo: trazer o histórico da planilha (RF11).
- Critérios de aceitação:
  - [ ] Envio do `.xlsx` pela interface web; o arquivo é lido em memória e **nunca** é gravado no disco nem no repositório.
  - [ ] Prévia antes de gravar, com: mapeamento das categorias antigas (para ativas, inativas ou renomeadas), escolha do "para quem" com atribuição em lote, sinalização de parcelas descritas em texto e de datas fora do intervalo esperado.
  - [ ] Lançamentos importados com origem `IMPORTACAO`; o saldo inicial vira um `ajuste_reserva`.
  - [ ] Testes com uma planilha **sintética** gerada no próprio teste (Apache POI), com a mesma estrutura de abas e colunas descrita no ERS. Os agentes nunca leem a planilha real.
- Referências: ERS RF11, RF18, Apêndice A.

## T13 — Dashboard no Power BI (Marco)
- Status: [ ] a fazer
- Responsável: **Marco**, sem agente (projeto de portfólio em análise de dados).
- Depende de: T11
- Objetivo: resumo mensal, planejado × real e evolução da reserva no Power BI Desktop.

## T14 — Core em container, com HTTPS na rede local
- Status: [ ] a fazer
- Tipo: chore · Branch: `chore/T14-core-container`
- Depende de: T09
- Objetivo: rodar tudo com `docker compose up -d`, acessível pelo celular na rede de casa.
- Critérios de aceitação:
  - [ ] `core/Dockerfile` em múltiplos estágios, imagem final só com JRE do Java 25, rodando com usuário não-root.
  - [ ] Serviço `core` no `docker-compose.yml`, iniciando só depois do MySQL saudável, com `restart: unless-stopped`.
  - [ ] Porta publicada no IP da rede local definido por variável do `.env` (nunca `0.0.0.0`).
  - [ ] HTTPS com certificado local (`mkcert`), cujos arquivos ficam fora do Git e entram por volume; instruções no README, inclusive para instalar a autoridade certificadora nos celulares.
  - [ ] Pipeline: build da imagem e varredura com Trivy; Dependabot para o ecossistema Docker.
- Referências: ERS RNF01, RNF03, RNF09; ADR-003.

## T15 — Endurecimento do pipeline
- Status: [ ] a fazer
- Tipo: chore · Branch: `chore/T15-endurecimento-pipeline`
- Depende de: T01
- Objetivo: completar as verificações previstas no CLAUDE.md.
- Critérios de aceitação:
  - [ ] JaCoCo no `verify`, reprovando abaixo de 90% nos pacotes `cartao` e nas classes de valores monetários.
  - [ ] Semgrep e SpotBugs com FindSecBugs no pipeline, com as actions fixadas por hash.
  - [ ] Nenhum alerta pendente sem justificativa registrada.
- Referências: CLAUDE.md, seção 6.
