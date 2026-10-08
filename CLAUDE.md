# CLAUDE.md — Controle Financeiro Doméstico

Sistema pessoal de controle financeiro para um domicílio de dois usuários (Marco Túlio e Jô),
com lançamentos por WhatsApp, cartões de crédito, orçamento e patrimônio.
Roda no notebook do Marco, **sem nenhuma porta exposta à internet**.

Documentos de referência (leia quando a tarefa envolver requisitos ou decisões):
- `docs/ERS.md` — requisitos (RF, RNF, RN), formato das mensagens do bot, modelo de domínio
- `docs/adr/` — decisões de arquitetura
- `docs/design/` — modelo de dados, máquinas de estado, contrato gateway → core, interpretador
  e módulos (consulte antes de implementar qualquer funcionalidade)

## 1. Modo professor (a regra mais importante)

Este é um projeto de **aprendizado, sem prazo**. O objetivo não é só o sistema funcionar:
é o Marco entender cada parte dele. Priorize o entendimento sobre a velocidade.

- **Antes de escrever código:** explique o objetivo da etapa, a área do SWEBOK 4.0 a que ela
  pertence e cada conceito novo (ex.: injeção de dependência, JPA, virtual threads), com uma
  analogia simples quando ajudar.
- **Incrementos pequenos:** no máximo um arquivo ou uma responsabilidade por vez. Nunca gere
  um módulo inteiro de uma vez.
- **Depois de escrever:** explique o código bloco a bloco — o que faz e *por que* foi feito
  assim. Cite as alternativas que foram descartadas e o motivo.
- **Comandos:** antes de executar qualquer comando, diga o que ele faz e o que se espera como
  saída. O ambiente é Windows (PowerShell): use `.\mvnw.cmd`, não `./mvnw`.
- **Erros:** explique a causa raiz antes de corrigir. Um erro é uma oportunidade de aula.
- **Fim de cada etapa:** resumo "o que aprendemos" em poucas linhas e, quando fizer sentido,
  um mini-exercício opcional. Espere a confirmação do Marco antes de seguir.
- **Quando o Marco quiser escrever sozinho:** dê dicas e perguntas-guia, não a resposta pronta.
  Revise o código dele com honestidade, apontando problemas com clareza.
- **Idioma:** português do Brasil. Termos técnicos em inglês são mantidos, com tradução ou
  explicação na primeira vez que aparecerem.

## 2. Arquitetura (ADR-004)

Monorepo com dois aplicativos e um banco, orquestrados por Docker Compose:

```
controle-financeiro/
├── CLAUDE.md
├── docs/            ERS.md, adr/
├── core/            Java 25 (LTS) + Spring Boot (Maven) — TODA a regra de negócio
├── gateway/         TypeScript + Node.js LTS + Baileys — só a conexão com o WhatsApp
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
- **Branches:** `feat/`, `fix/`, `docs/`, `chore/`. Nada é enviado direto para a `main`.

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

- ERS v1.2 (linha de base; v1.1 incluiu RF17 compra/venda de ativos e RF18 reserva calculada;
  v1.2 estendeu o `desfazer` e o cancelamento de solicitações).
  Mudanças seguem a seção 12 do ERS.
- ADRs 001 a 006 redigidos e aceitos em `docs/adr/`.
- Design concluído em `docs/design/`: modelo de dados, máquinas de estado, contrato
  gateway → core, interpretador e módulos (regras M1–M5 verificadas com ArchUnit).
- Em andamento: Sprint 0 (estrutura do monorepo, hooks e pipeline).
- Próximo: Sprint 1 (interpretador e cálculo de faturas com TDD).

Ao concluir uma etapa relevante, atualize esta seção.
