# ADR-006: Exclusão lógica com validação cruzada

- **Status:** Aceito
- **Data:** 2026-10
- **Requisitos relacionados:** RF10, RN05, RN07, RNF06

## Contexto
Todos os usuários do domicílio podem ver e editar todos os dados, mas exclusões não podem
depender da vontade de uma só pessoa. Além disso, o histórico precisa ser auditável
("quem lançou isso?") e um erro de exclusão não pode apagar dados para sempre.

O domicílio tem dois usuários com o mesmo nível de confiança, então não há motivo para
eleger um admin único.

## Decisão
- Nenhum registro de negócio é apagado do banco (`DELETE`). A exclusão é **lógica**: o registro
  recebe a data da exclusão, quem a solicitou e quem a aprovou.
- **Validação cruzada** (princípio dos quatro olhos): o que um usuário solicita, o outro aprova.
  1. **Solicitação:** um usuário (pela web) ou o comando `desfazer` (pelo bot) cria uma
     solicitação pendente para o registro.
  2. **Aprovação:** na plataforma web, um usuário **diferente do solicitante** aprova ou recusa,
     confirmando com a própria senha (reautenticação, mesmo já estando logado).
- A regra "solicitante ≠ aprovador" é verificada no core, no serviço que efetiva a aprovação —
  não apenas escondendo o botão na tela.
- Consultas e relatórios ignoram registros excluídos por padrão.
- Senhas nunca são solicitadas nem trafegam pelo WhatsApp.

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| Exclusão física com confirmação | Perde o histórico e impossibilita a auditoria |
| Exclusão livre para todos | Contraria o requisito RF10 |
| Admin único que aprova tudo | Concentra o poder em um usuário, e as exclusões do próprio admin ficariam sem nenhuma validação |
| Confirmar a exclusão pelo bot | Exigiria senha pelo WhatsApp |

## Consequências
- **Positivas:** nenhuma pessoa sozinha consegue apagar dados; nada se perde por engano;
  a auditoria registra as duas pessoas envolvidas; a exclusão pode ser revertida.
- **Negativas:** a exclusão depende da disponibilidade do outro usuário (aceitável: não há
  urgência em excluir um lançamento); toda consulta precisa filtrar os excluídos (mitigado por
  um filtro padrão no JPA e por testes); se os dois usuários compartilharem senhas, a regra
  perde o sentido — é um acordo de uso, não uma garantia técnica.
