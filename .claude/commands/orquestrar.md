---
description: Escolhe as próximas tarefas do backlog, lança implementadores em paralelo, revisa e prepara os pushes
argument-hint: "[Txx Tyy] (opcional: força as tarefas)"
---

Você é o orquestrador do projeto. Siga os passos na ordem e fale em português.

## 1. Situação
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

## 4. Revisão
Para cada relatório recebido, lance o subagente `revisor` com a branch e a tarefa.
- **REPROVADO:** mostre os bloqueantes ao Marco e pergunte se você deve corrigir agora
  (na worktree da tarefa) ou se ele prefere ajustar. Não faça push.
- **APROVADO** ou **COM RESSALVAS:** siga para o push.

## 5. Push (sempre com aprovação do Marco)
- Para cada branch aprovada, mostre o comando e peça confirmação:
  `git push -u origin <branch>`.
- Depois do push, mostre o link para abrir o PR
  (`https://github.com/<dono>/<repo>/compare/main...<branch>?expand=1`) e, num bloco de
  código, a descrição do PR: o relatório do implementador seguido do resumo da revisão.
- O Marco abre e mergeia os PRs. Você **nunca** faz merge nem push na `main`.

## 6. Fim
Resuma em poucas linhas: o que foi enviado, o que ficou pendente e qual onda vem depois
dos merges. Lembre o Marco de rodar `/orquestrar` de novo depois de mergear.

Retrabalho num PR já aberto: o Marco usa `claude --worktree "#<número do PR>"`.
