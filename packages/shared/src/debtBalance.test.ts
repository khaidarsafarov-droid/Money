import { describe, expect, it } from "vitest";
import { debtRemainingCents } from "./finance";

describe("debtRemainingCents", () => {
  it("subtracts payments from every disbursement", () => {
    expect(debtRemainingCents([50_000, 50_000, 50_000], [20_000])).toBe(130_000);
  });

  it("grows the same debt when another amount is given", () => {
    expect(debtRemainingCents([50_000, 50_000, 50_000, 20_000], [])).toBe(170_000);
  });

  it("does not go below zero", () => {
    expect(debtRemainingCents([10_000], [25_000])).toBe(0);
  });
});
