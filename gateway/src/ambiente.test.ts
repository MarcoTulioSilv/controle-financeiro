import { describe, expect, it } from "vitest";

describe("ambiente de testes", () => {
  it("executa testes em TypeScript", () => {
    const soma: number = 1 + 1;
    expect(soma).toBe(2);
  });
});