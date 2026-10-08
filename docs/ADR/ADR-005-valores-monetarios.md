# ADR-005: Representação de valores monetários

- **Status:** Aceito
- **Data:** 2026-10
- **Requisitos relacionados:** RNF05, RF12

## Contexto
Números de ponto flutuante (`double`, `float`) não representam frações decimais com
exatidão: em Java, `0.1 + 0.2` resulta em `0.30000000000000004`. Em um sistema financeiro,
isso gera centavos "fantasmas" em totais e relatórios.

## Decisão
- No código: `BigDecimal` com escala 2, **sempre construído a partir de texto**
  (`new BigDecimal("150.00")`). Construir a partir de `double` herda a imprecisão.
- Arredondamento: `RoundingMode.HALF_EVEN` (arredondamento bancário).
- No banco: `DECIMAL(12,2)`, mapeado diretamente para `BigDecimal` pelo JPA.
- Compras parceladas: o valor é dividido igualmente e a diferença de arredondamento vai para
  a **primeira** parcela, garantindo que a soma das parcelas seja exatamente o total.
- Comparações de valores com `compareTo`, nunca com `equals` (em `BigDecimal`, `equals`
  considera a escala: `150.0` e `150.00` seriam diferentes).

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
|---|---|
| `double` | Impreciso |
| Centavos em `long` | Exato, mas exige conversão em toda entrada e saída e fica ilegível no Workbench |

## Consequências
- **Positivas:** exatidão garantida do banco à tela; valores legíveis no MySQL Workbench.
- **Negativas:** `BigDecimal` é imutável e mais verboso (`a.add(b)` em vez de `a + b`).
  As regras acima precisam de testes dedicados.
