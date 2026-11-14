# CLAUDE.md — Controle Financeiro Doméstico

Sistema pessoal de controle financeiro para um domicílio de dois usuários (Marco Túlio e Ana),
com lançamentos por WhatsApp, cartões de crédito, orçamento e patrimônio.
Roda no notebook do Marco, **sem nenhuma porta exposta à internet**.

Documentos de referência (leia quando a tarefa envolver requisitos ou decisões):
- `docs/ERS.md` — requisitos (RF, RNF, RN), formato das mensagens do bot, modelo de domínio
- `docs/adr/` — decisões de arquitetura
- `docs/design/` — modelo de dados, máquinas de estado, contrato gateway → core, interpretador
  e módulos (consulte antes de implementar qualquer funcionalidade)

## 1. Como trabalhamos: agentes implementam, o Marco revisa

O Marco precisa do sistema funcionando logo, sem abrir mão de entender o que foi feito.
Por isso o código é escrito por agentes e **todo** ele passa pela revisão do Marco num PR.

- **Plano de construção:** `docs/backlog/mvp1.md`. Uma tarefa = uma branch = um PR. Não
  trabalhe fora de uma tarefa do backlog sem o Marco pedir.
- **Fluxo:** o comando `/orquestrar` escolhe até duas tarefas prontas e lança o subagente
  `implementador` (em worktree isolada) para cada uma; o subagente `revisor` confere a
  branch; o orquestrador faz o push com aprovação do Marco; o Marco abre, revisa e mergeia
  o PR.
- **Código para ser lido:** simples, com nomes do domínio, sem abstrações "para o futuro".
  Comentários só onde o *porquê* não é óbvio.
- **Toda entrega ensina:** o relatório do PR tem a seção "Para aprender", com os conceitos
  novos e a área do SWEBOK 4.0 envolvida.
- **Conversa direta com o Marco** (fora do fluxo de agentes): continue no modo professor —
  explique o conceito antes do código, diga o que um comando faz antes de executá-lo,
  explique a causa raiz de um erro antes de corrigi-lo e, quando ele quiser escrever
  sozinho, dê dicas em vez da resposta pronta.
- **Ambiente:** Windows com VS Code. No PowerShell use `.\mvnw.cmd`; no Bash, `./mvnw`.
- **Idioma:** português do Brasil, inclusive nas mensagens de commit. Termos técnicos em
  inglês são mantidos, com explicação na primeira vez que aparecerem.

## 2. Arquitetura (ADR-004)

Monorepo com dois aplicativos e um banco, orquestrados por Docker Compose:

```
controle-financeiro/
├── CLAUDE.md
├── .claude/         subagentes (implementador, revisor) e o comando /orquestrar
├── docs/            ERS.md, adr/, design/, backlog/
├── core/            Java 25 (LTS) + Spring Boot (Maven) — TODA a regra de negócio
├── gateway/         TypeScript + Node.js 26 (LTS) + Baileys — só a conexão com o WhatsApp
├── docker-compose.yml
└── .env.example     modelo de variáveis (o .env real nunca é versionado)
```

- **core:** Spring Web, Spring Security, Spring Data JPA, Bean Validation, Flyway (migrações),
  Thymeleaf + HTMX (interface web), agendador de cotações/FIPE.
- **gateway:** recebe a mensagem, envia o texto bruto ao core e devolve a resposta do core ao
  WhatsApp. **Não contém regra de negócio**, não interpreta mensagens, não acessa o banco.
- **Contrato gateway → core:** HTTP na rede interna do Docker, autenticado por token de
  permissão mínima (só pode criar lançamentos). O core interpreta, valida, grava e devolve o
  texto de resposta.
- **mysql:** MySQL 8, porta publicada apenas em `127.0.0.1:3307` (acesso pelo MySQL Workbench).
- **Relatórios (ADR-007):** Power BI Desktop no notebook, lendo **visões SQL** criadas por
  migrações, com um usuário MySQL somente leitura. Nunca publicar no serviço online.

## 3. Regras invioláveis

<important>
Segurança (DevSecOps):
- Nenhum serviço publica porta em `0.0.0.0`. Só a interface web é acessível na rede local.
- O gateway não publica porta nenhuma; só faz conexões de saída.
- Segredos só em `.env` (fora do Git). Os arquivos de sessão do WhatsApp ficam em volume
  Docker e **nunca** são versionados.
- Dependências com versão fixada. Baileys apenas do repositório oficial `WhiskeySockets/Baileys`;
  nunca usar aliases de pacote nem forks. Confira com `npm view baileys repository`.
- O bot ignora mensagens `fromMe`, de grupos e de números não autorizados (RF07.1).
- O bot não exclui nem altera registros. Exclusões são lógicas e seguem a validação cruzada:
  quem solicita não pode aprovar; o outro usuário aprova com a própria senha (RF10).
- Respostas do bot contêm só a confirmação do lançamento — nunca saldos ou totais (RNF12).
- Nunca escreva dados pessoais reais (nomes de familiares, bancos, cartões, fontes de renda,
  cidade) em arquivos versionados. Use os nomes fictícios dos documentos; os dados reais
  ficam em arquivos não versionados, como o `.env`.

Dinheiro e datas:
- Valores monetários sempre `BigDecimal`, escala 2. **Nunca `double` ou `float`.**
  Construa a partir de `String` (`new BigDecimal("150.00")`), nunca de `double`.
- No banco: `DECIMAL(12,2)`. No gateway, valores trafegam só como texto da mensagem.
- Datas com `java.time`, fuso `America/Sao_Paulo`. Mês de referência é `YearMonth`.
  Nunca use `java.util.Date` nem `Calendar`.
</important>

## 4. Nomenclatura

- Código e banco usam os termos do domínio em português: `Lancamento`, `mesReferencia`,
  `dataCompra`, `paraQuem`, `Fatura`, `Cartao`.
- O rótulo **`adc`** aparece **somente** no texto de resposta do bot (ex.: `adc out/26`).
  Em código, banco e documentação o termo é "mês de referência".
- Pacote base Java: `com.marcotulio.financeiro`, organizado por funcionalidade
  (`lancamento`, `cartao`, `interpretador`, `patrimonio`), não por camada técnica.

## 5. Processo

Seguimos o SWEBOK 4.0: requisitos → arquitetura → design → construção → testes, de forma
iterativa. Mudanças de requisito atualizam `docs/ERS.md`; decisões novas viram um ADR.

- **TDD** no interpretador de mensagens, no cálculo de faturas (RN03) e na manipulação de
  valores: o teste é escrito antes do código. Os casos de valor limite do ERS (RF14) são
  obrigatórios.
- **Commits:** Conventional Commits com descrição em português
  (`feat: calcula mês de referência da fatura`). Um commit por passo lógico.
- **Branches:** `feat/`, `fix/`, `docs/`, `chore/`, com o código da tarefa
  (`feat/T02-calculadora-fatura`). Nada é enviado direto para a `main`.
- **Migrações Flyway:** versão com data e hora, `V<AAAAMMDDHHMM>__descricao.sql`, para dois
  agentes em paralelo não colidirem. Uma migração que já está na `main` nunca é alterada:
  corrige-se com uma nova.
- **Dados de exemplo e de teste:** só os nomes fictícios do ERS. Os dados reais ficam em
  `config/dados-iniciais.yml` (não versionado; há um `.example.yml` versionado).

## 6. Testes e qualidade

- core: JUnit 5 + AssertJ; Testcontainers (MySQL real) para testes de integração;
  JaCoCo com mínimo de 90% nos pacotes `interpretador`, `cartao` e de valores.
- gateway: Vitest.
- Pipeline (GitHub Actions): gitleaks, Semgrep, SpotBugs + FindSecBugs, auditoria de
  dependências (Maven e npm), Trivy nas imagens, Dependabot para Maven, npm e Docker.

## 7. Comandos (disponíveis após a criação de cada projeto)

```powershell
# core
cd core
.\mvnw.cmd test               # testes unitários
.\mvnw.cmd verify             # testes + cobertura + análises
.\mvnw.cmd spring-boot:run    # sobe a aplicação localmente

# gateway
cd gateway
npm ci                        # instala exatamente o que está no lockfile
npm test
npm audit

# tudo junto
docker compose up -d
docker compose logs -f core
```

## 8. Estado atual

- ERS v1.3 (linha de base; v1.3 levou os relatórios do RF09 para o Power BI Desktop).
  Mudanças seguem a seção 12 do ERS.
- ADRs 001 a 007 aceitos em `docs/adr/`.
- Design concluído em `docs/design/` (modelo de dados, máquinas de estado, contrato
  gateway → core, interpretador, módulos M1–M5 verificados com ArchUnit).
- Sprint 0 concluída (tag `sprint-0`): monorepo, MySQL 8.4 no Docker Compose, pre-commit com
  gitleaks (`pass_filenames: false` é obrigatório), pipeline, Dependabot, `main` protegida.
- ArchUnit: M1 e M2 ativas com `allowEmptyShould(true)`. Remover o `allowEmptyShould` da M2
  quando o pacote `interpretador` existir.
- `CalculadoraFatura` com os casos 1 e 4 (feito pelo Marco, com TDD).
- **Em andamento: MVP 1 — substituir a planilha**, conduzido por agentes a partir de
  `docs/backlog/mvp1.md`. O progresso fica no próprio backlog; esta seção só muda ao fim de
  um MVP. Bot de WhatsApp e patrimônio ficam para os MVPs seguintes.
