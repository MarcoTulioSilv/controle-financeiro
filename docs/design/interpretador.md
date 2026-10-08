# Design do Interpretador de Mensagens

| Campo | Valor |
|---|---|
| Base | ERS v1.2 (seção 4, RF07, RF12, RF13, RF14, RN02), `contrato-gateway-core.md` |
| Situação | Aprovado |

## 1. Princípio: núcleo funcional, casca imperativa

O interpretador é uma **função pura**: não acessa banco, relógio nem rede, e não grava nada.

```
interpretar(texto, dicionario, hoje) → ResultadoInterpretacao
```

| Parâmetro | Conteúdo |
|---|---|
| texto | Mensagem recebida, como veio do gateway |
| dicionario | Fotografia de palavras-chave (→ categoria), apelidos de cartão e marcações de destinatário, carregada pelo serviço antes da chamada |
| hoje | Data atual no fuso America/Sao_Paulo, passada como parâmetro |

A casca (serviço) carrega o dicionário, obtém a data, chama o interpretador e age sobre o resultado (gravar, criar rascunho, responder). Assim o núcleo é testável com entradas fixas e resultados determinísticos.

## 2. Pipeline

```
"Mercado 1.500 nupjmt @casa"
1. Normalizar   → "mercado 1.500 nupjmt @casa"
2. Tokenizar    → [mercado] [1.500] [nupjmt] [@casa]
3. Classificar  → DESCRICAO, VALOR, CARTAO, DESTINATARIO
4. Montar       → Despesa, R$ 1.500,00, Alimentação, cartão nupjmt, @casa
```

**Normalização:** minúsculas, espaços extras removidos, expressões compostas convertidas em token único (`próximo mês` → `prox-mes`). As comparações com o dicionário ignoram acentos (`java.text.Normalizer`, forma NFD, com remoção das marcas diacríticas), coerente com a collation do banco (C3). A descrição preserva o texto original do usuário.

## 3. Ordem de classificação

Cada token é testado nesta ordem; a primeira classe que o reconhece vence.

| Ordem | Classe | Reconhece | Exemplos |
|---|---|---|---|
| 1 | Comando | A mensagem inteira é um comando | `ajuda`, `desfazer` |
| 2 | Receita | `+` no início da mensagem | `+ pacote`, `+pacote` |
| 3 | Parcelamento | Número seguido de `x` | `16x` |
| 4 | Data | `dd/mm`, `dd/mm/aaaa`, `hoje`, `ontem` | `27/09` |
| 5 | Mês de referência | Nome do mês (extenso ou abreviado), `prox-mes`, `mmm/aa` | `outubro`, `out/27` |
| 6 | Destinatário | `@` + marcação cadastrada | `@casa` |
| 7 | Cartão | Apelido cadastrado | `nupjmt` |
| 8 | Valor | Número; vale o **último** da mensagem | `150`, `65,34` |
| 9 | Descrição | Tudo o que sobrou, na ordem original | `mercado` |

Regras derivadas:
- Números que não são o último ficam na descrição (`iphone 15 4000` → descrição "iphone 15", valor 4000).
- Com parcelamento, a categoria é **Dívidas**, prevalecendo sobre as palavras-chave (ERS 4.2, regra 6).
- A categoria é deduzida pela primeira palavra da descrição que exista no dicionário.

## 4. Valores: ponto e vírgula

| Entrada | Interpretação | Regra |
|---|---|---|
| `1.500` | 1.500,00 | Ponto seguido de exatamente 3 dígitos é separador de milhar |
| `1,50` | 1,50 | Vírgula é sempre decimal |
| `1.50` | 1,50 | Ponto seguido de 1 ou 2 dígitos é decimal |
| `1.500,50` | 1.500,50 | Formato brasileiro completo |

O valor é convertido para `BigDecimal` a partir do texto normalizado, com escala 2 (ADR-005). Mais de duas casas decimais ou valor zero resultam em `Invalido`.

## 5. Resultado

```java
sealed interface ResultadoInterpretacao
    permits Interpretado, Incompleto, Invalido, Comando { }
```

Cada desfecho é um `record` imutável. Num `switch` sobre a interface selada, o compilador exige o tratamento dos quatro casos.

| Desfecho | Quando | Ação do serviço |
|---|---|---|
| `Interpretado` | Todos os campos obrigatórios identificados | Calcula a fatura (se houver cartão) e grava o lançamento |
| `Incompleto` | Falta o "para quem" ou a categoria é desconhecida | Cria rascunho e faz a pergunta (ERS 4.4) |
| `Invalido` | Sem valor, data impossível, mês de referência anterior ao da compra (RN02) | Responde com a explicação e um exemplo correto |
| `Comando` | `ajuda` ou `desfazer` | Executa o comando |

O caso RN02 com distância superior a dois meses não é `Invalido`: gera uma confirmação, tratada pelo serviço.

## 6. Separação do cálculo da fatura

O interpretador apenas identifica o apelido do cartão. O mês de referência da compra no cartão é calculado pela `CalculadoraFatura`, outra função pura:

```
calcular(cartao, dataCompra, feriados) → (mesReferencia, fechamento, vencimentoNominal, vencimentoEfetivo)
```

Os casos de valor limite do ERS (RF14) são testados nela, isoladamente.

## 7. Casos de teste mínimos (TDD, Sprint 1)

| Entrada (hoje = 05/10/2026) | Resultado esperado |
|---|---|
| `mercado 150 @casa` | Interpretado: despesa, 150,00, Alimentação, @casa, compra 05/10 |
| `Mercado 1.500 nupjmt @casa` | Interpretado: 1.500,00, cartão nupjmt |
| `farmacia 80 @jo` (palavra-chave "farmácia") | Interpretado: Saúde/Farmácia |
| `+ pacote 100 @mt` | Interpretado: receita, Big Rock |
| `iphone 15 4000 @mt` | Interpretado: descrição "iphone 15", valor 4000,00 |
| `celular 16x 106,18 nupjmt @mt` | Interpretado: Dívidas, 16 parcelas de 106,18 |
| `gás 27/09 outubro 130 @casa` | Interpretado: compra 27/09, referência out/2026 |
| `mercado próximo mês 250 @casa` | Interpretado: referência nov/2026 |
| `mercado 150` | Incompleto: falta destinatário |
| `xpto 50 @casa` | Incompleto: categoria desconhecida |
| `mercado @casa` | Invalido: sem valor |
| `mercado 31/02 50 @casa` | Invalido: data impossível |
| `mercado 0 @casa` | Invalido: valor zero |
| `desfazer` | Comando |

## Histórico

| Data | Alteração |
|---|---|
| 08/10/2026 | Design do interpretador aprovado |
