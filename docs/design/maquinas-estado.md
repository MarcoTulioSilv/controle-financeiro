# Máquinas de Estado

| Campo | Valor |
|---|---|
| Base | ERS v1.4, `modelo-dados.md` |
| Situação | **Completo** — fatura, dívida, rascunho, solicitação de aprovação e adiantamento de dívida |

Uma máquina de estados define, para uma entidade com ciclo de vida: os **estados** possíveis, as **transições** permitidas, os **eventos** que as disparam, as **guardas** (condições) e os **efeitos**. Toda transição que não está no diagrama é **proibida**: o código deve lançar uma exceção em vez de gravar um estado inconsistente.

Princípio para transições disparadas pelo sistema (agendador): devem ser **idempotentes**. Como o notebook pode estar desligado na data prevista, o agendador processa ao iniciar tudo o que estiver atrasado, e uma segunda execução não produz efeito adicional.

---

## 1. Fatura (RF14, RF15, RN06)

```mermaid
stateDiagram-v2
    [*] --> ABERTA: primeira compra do ciclo
    ABERTA --> EM_REVISAO: chegou a data de fechamento (sistema)
    EM_REVISAO --> CONFERIDA: usuário confirma a revisão
    CONFERIDA --> EM_REVISAO: usuário reabre
    CONFERIDA --> PAGA: usuário registra o pagamento
    PAGA --> [*]
```

| Transição | Evento | Guarda | Efeito |
|---|---|---|---|
| — → ABERTA | Primeira compra cujo cálculo (RN03) aponta para esta fatura | Não existe fatura do cartão no mês de referência | Fatura criada com datas de fechamento, vencimento nominal e efetivo |
| ABERTA → EM_REVISAO | Agendador (origem SISTEMA) | Data atual ≥ data de fechamento | Aviso de revisão na tela inicial e na próxima resposta do bot |
| EM_REVISAO → CONFERIDA | Usuário confirma | — (a diferença em relação ao total do banco é exibida, mas não impede) | Compras da fatura travadas (RN06) |
| CONFERIDA → EM_REVISAO | Usuário reabre | — (sem validação cruzada, conforme RN07) | Compras destravadas |
| CONFERIDA → PAGA | Usuário registra o pagamento | — | Estado final |

| Estado | Compras da fatura |
|---|---|
| ABERTA | Podem ser adicionadas e alteradas |
| EM_REVISAO | Podem ser adicionadas (processamento tardio do banco), alteradas e movidas entre faturas |
| CONFERIDA | Travadas |
| PAGA | Travadas, sem possibilidade de reabertura |

Compra recebida pelo bot para uma fatura CONFERIDA ou PAGA: nada é gravado; o bot informa que a fatura está fechada e orienta a reabertura na plataforma.

---

## 2. Dívida (RF12)

```mermaid
stateDiagram-v2
    [*] --> ATIVA: compra parcelada ou financiamento registrado
    ATIVA --> ATIVA: adiantamento aplicado, sobra saldo (usuário)
    ATIVA --> QUITADA: mês da última parcela chegou (sistema)
    ATIVA --> QUITADA: adiantamento aplicado zera o saldo (usuário)
    QUITADA --> [*]
```

| Transição | Evento | Guarda | Efeito |
|---|---|---|---|
| — → ATIVA | Registro da dívida (web ou bot com `Nx`) | — | Parcelas geradas como lançamentos nos meses correspondentes |
| ATIVA → ATIVA | Adiantamento aplicado (seção 5) | Valor abatido < saldo devedor | Abatimento das últimas parcelas (seção 5) |
| ATIVA → QUITADA (normal) | Agendador (origem SISTEMA) | Mês atual ≥ mês de referência da última parcela | `data_quitacao` preenchida |
| ATIVA → QUITADA (antecipada) | Adiantamento aplicado (seção 5) | Valor abatido = saldo devedor | Abatimento de todas as parcelas abatíveis; `data_quitacao` preenchida |

**Saldo devedor**, para o adiantamento: soma das parcelas não excluídas cujo mês de referência é **posterior** ao mês de referência do pagamento do adiantamento. A parcela do próprio mês é paga normalmente e não entra no abatimento.

**Quitação antecipada é um adiantamento.** Antes da v1.4 do ERS ela tinha um lançamento próprio (`lancamento_quitacao_id`); agora é o adiantamento cujo valor abatido zera o saldo, e o mesmo código atende os dois casos.

---

## 3. Rascunho (ERS 4.4)

```mermaid
stateDiagram-v2
    [*] --> ABERTO: mensagem com informação faltando
    ABERTO --> ABERTO: resposta completa um campo, mas falta outro
    ABERTO --> COMPLETADO: última informação recebida
    ABERTO --> A_COMPLETAR_WEB: 30 minutos sem resposta (sistema)
    ABERTO --> DESCARTADO: usuário envia "desfazer"
    A_COMPLETAR_WEB --> COMPLETADO: usuário completa na plataforma
    A_COMPLETAR_WEB --> DESCARTADO: usuário descarta na plataforma
    COMPLETADO --> [*]
    DESCARTADO --> [*]
```

| Transição | Evento | Guarda | Efeito |
|---|---|---|---|
| — → ABERTO | Mensagem sem "para quem" ou com categoria desconhecida | Remetente não tem outro rascunho ABERTO | Bot faz a pergunta do campo pendente |
| ABERTO → ABERTO | Resposta válida | Ainda falta um campo | `dados_parciais` e `campo_pendente` atualizados; próxima pergunta |
| ABERTO → COMPLETADO | Resposta válida | Nenhum campo falta | Lançamento criado na mesma transação; confirmação enviada |
| ABERTO → A_COMPLETAR_WEB | Prazo de 30 minutos esgotado | — | Aviso na tela inicial e na próxima resposta do bot |
| ABERTO → DESCARTADO | Comando `desfazer` | — | Nenhum lançamento criado |
| A_COMPLETAR_WEB → COMPLETADO | Usuário completa na web | Campos obrigatórios preenchidos | Lançamento criado na mesma transação |
| A_COMPLETAR_WEB → DESCARTADO | Usuário descarta na web | — | Nenhum lançamento criado |

**Descartar não exige validação cruzada:** um rascunho ainda não é um lançamento.

**Verificação do prazo em dois pontos:** o agendador faz a varredura periódica e, ao chegar uma nova mensagem do remetente, o prazo do rascunho dele é verificado antes do processamento. A regra de tempo não depende só do agendador estar em dia.

---

## 4. Solicitação de aprovação (RF10, RF07.1, RN07)

```mermaid
stateDiagram-v2
    [*] --> PENDENTE: usuário solicita (web ou "desfazer" no bot)
    PENDENTE --> APROVADA: outro usuário aprova com senha
    PENDENTE --> RECUSADA: outro usuário recusa
    PENDENTE --> CANCELADA: o próprio solicitante desiste
    APROVADA --> [*]
    RECUSADA --> [*]
    CANCELADA --> [*]
```

| Transição | Evento | Guarda | Efeito |
|---|---|---|---|
| — → PENDENTE | Pedido de exclusão (web ou `desfazer` sem rascunho aberto) ou cadastro de número | Sem outra solicitação pendente para o alvo; na exclusão, a fatura do lançamento não está CONFERIDA nem PAGA | Aviso ao outro usuário na tela inicial e na próxima resposta do bot |
| PENDENTE → APROVADA | Aprovação na web | Aprovador ≠ solicitante; senha confirmada (reautenticação); guarda da fatura **verificada novamente** | Exclusão: `excluido_em` do lançamento preenchido. Número: status APROVADO. Mesma transação |
| PENDENTE → RECUSADA | Recusa na web | Aprovador ≠ solicitante | Nenhum efeito sobre o alvo |
| PENDENTE → CANCELADA | Cancelamento na web | Quem cancela é o solicitante | Nenhum efeito sobre o alvo |

**Cancelar não enfraquece os quatro olhos:** só a aprovação produz efeito sobre os dados, e ela continua exigindo outra pessoa.

**Guarda verificada duas vezes:** entre a criação e a aprovação, a fatura do lançamento pode ter sido conferida. A verificação na aprovação impede uma condição de corrida em que o estado mudou entre a pergunta e a resposta.

---

## 5. Adiantamento de dívida (RF12)

```mermaid
stateDiagram-v2
    [*] --> APLICADO: registrado na web
    [*] --> PENDENTE: registrado pelo bot
    PENDENTE --> APLICADO: usuário confirma na web
    PENDENTE --> DESCARTADO: usuário descarta na web
    APLICADO --> [*]
    DESCARTADO --> [*]
```

| Transição | Evento | Guarda | Efeito |
|---|---|---|---|
| — → APLICADO | Registro na web, com valor pago e valor abatido | Dívida ATIVA; valor abatido ≤ saldo devedor; valor pago ≤ valor abatido; nenhuma parcela afetada em fatura CONFERIDA ou PAGA (RN06) | Lançamento do pagamento e abatimento, na mesma transação |
| — → PENDENTE | Mensagem `abater` no bot (ERS 4.2, regra 8) | Dívida ATIVA; valor pago ≤ valor abatido | Só o lançamento do pagamento; aviso na tela inicial |
| PENDENTE → APLICADO | Usuário confirma na web | As guardas do registro na web, **verificadas novamente** | Abatimento, na mesma transação |
| PENDENTE → DESCARTADO | Usuário descarta na web | — | Nenhum abatimento. O lançamento do pagamento continua (o dinheiro saiu); se estiver errado, a correção segue o RF10 |

**Abatimento:** percorre as parcelas abatíveis da última para a primeira. Cada parcela que cabe inteira no valor restante sofre exclusão lógica (`excluido_em`); o que sobrar reduz o valor da parcela seguinte nesse percurso. O saldo devedor cai exatamente o valor abatido. Exemplo: parcelas de R$ 106,18 e abatimento de R$ 500,00 → quatro parcelas excluídas (R$ 424,72) e a quinta, de trás para a frente, reduzida em R$ 75,28.

**Por que o bot não aplica o abatimento (RN05):** o abatimento exclui e altera parcelas, e o bot não exclui nem altera registros. O bot registra só o fato novo (o pagamento) e deixa o efeito sobre registros existentes para a web, onde o usuário está autenticado e vê as parcelas afetadas.

**Sem validação cruzada:** como na quitação, o adiantamento é um acontecimento real, não a correção de um erro; a RN07 não se aplica. O Envers registra o autor e as parcelas afetadas.

**Guardas verificadas duas vezes:** entre o bot e a confirmação, outra parcela pode ter vencido ou uma fatura pode ter sido conferida, mudando o saldo devedor. A confirmação recalcula tudo antes de aplicar.

---

## Histórico

| Data | Alteração |
|---|---|
| 05/10/2026 | Máquinas da fatura e da dívida aprovadas (quitação antecipada sem validação cruzada) |
| 05/10/2026 | Máquinas do rascunho (com DESCARTADO) e da solicitação de aprovação (com CANCELADA) aprovadas; ERS v1.2 |
| 22/11/2026 | ERS v1.4 (issue #30): nova máquina do adiantamento de dívida (seção 5); a quitação antecipada passa a ser um adiantamento que zera o saldo |
