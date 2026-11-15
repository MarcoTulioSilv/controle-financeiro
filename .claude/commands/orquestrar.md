---
description: Escolhe as próximas tarefas do backlog, lança implementadores em paralelo, revisa e abre os pull requests
argument-hint: "[Txx Tyy] (opcional: força as tarefas)"
---

Você é o orquestrador do projeto. Siga os passos na ordem e fale em português.

## 1. Situação
- Antes de tudo, compare a pasta atual com `git rev-parse --show-toplevel`. Se forem a mesma pasta, mas com diferença de maiúsculas ou minúsculas (ex.: `c:\` × `C:/`), **pare**: as worktrees dos subagentes vão falhar. Oriente o Marco a fechar o VS Code, rodar `cd C:\...` com o caminho exato do Git e abrir com `code .`.
- Confirme que o `gh` (GitHub CLI) está instalado e autenticado: `gh auth status`. Se não estiver, **pare** e peça ao Marco para rodar `winget install --id GitHub.cli`, reabrir o terminal e fazer `gh auth login`. Sem o `gh`, o revisor não abre PR.
- `git fetch origin --prune`.
- Leia o backlog **da main**: `git show origin/main:docs/backlog/mvp1.md`.
- Liste as branches remotas: `git branch -r`.
- Uma tarefa está **pronta** quando: está `[ ] a fazer`, todas as dependências estão
  `[x]` na main, não existe branch remota com o seu código (`Txx`) e não é do Marco (T13).
- Uma tarefa com branch remota e ainda `[ ]` na main está **em revisão**: não mexa nela.

Mostre ao Marco uma tabela curta: concluídas, em revisão, prontas e bloqueadas.

## 2. Escolha
- Se o Marco passou tarefas em `$ARGUMENTS`, use essas (se estiverem prontas).
- Senão, escolha **no máximo duas** prontas, seguindo a ordem das ondas do backlog.
  Somando as em revisão, nunca passe de duas tarefas abertas ao mesmo tempo.
- Duas tarefas em paralelo não podem mexer nos mesmos arquivos de configuração
  (`pom.xml`, `application.yaml`, workflows). Se mexerem, rode uma só.
- Se nada estiver pronto, diga o que está bloqueando (provavelmente PRs a mergear) e pare.

## 3. Implementação
Lance os subagentes `implementador` **em paralelo** (um por tarefa, na mesma mensagem),
passando a cada um: o código e o título da tarefa, a branch e o lembrete de não fazer push.

Quando o relatório de um implementador chegar, **salve-o como está** num arquivo do
diretório temporário da sessão (nunca no repositório): `<scratchpad>/pr-Txx.md`. Esse
arquivo vai virar o corpo do pull request.

## 4. Revisão e abertura do PR
Para cada relatório recebido, lance o subagente `revisor`, passando: a branch, a tarefa,
o caminho da worktree e **o caminho do `pr-Txx.md`**. O revisor revisa, acrescenta a
própria revisão ao fim do arquivo e, se aprovar, faz o push e abre o PR ele mesmo.

- **REPROVADO:** nenhum push, nenhum PR. Mostre os bloqueantes ao Marco e pergunte se você
  deve corrigir agora (na worktree da tarefa) ou se ele prefere ajustar.
- **APROVADO** ou **COM RESSALVAS:** o revisor já abriu o PR. Mostre ao Marco só o número,
  o título e o link do PR, mais os bloqueantes (nenhum) e as ressalvas em uma linha cada.
  **Não repita a descrição do PR no terminal** — ela já está no GitHub, que é onde o Marco
  revisa.

Você **nunca** faz merge nem push na `main`. Quem mergeia é o Marco, no GitHub.

## 5. Conflitos previsíveis
Toda branch marca a própria tarefa como concluída em `docs/backlog/mvp1.md`. Quando dois
PRs estão abertos ao mesmo tempo, o segundo merge vai pedir resolução nesse arquivo.
Avise o Marco disso ao fim, dizendo que é conflito trivial (manter as duas marcações).

## 6. Fim
Resuma em poucas linhas: quais PRs foram abertos (número e link), o que ficou pendente e
qual onda vem depois dos merges. Lembre o Marco de rodar `/orquestrar` de novo depois de
mergear.

Retrabalho num PR já aberto: o Marco usa `claude --worktree "#<número do PR>"`.
