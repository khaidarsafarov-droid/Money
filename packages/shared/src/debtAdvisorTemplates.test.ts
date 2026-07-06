import { describe, expect, it } from "vitest";
import { buildAdvisorReply, pickSnowballDebt } from "./debtAdvisorTemplates";

describe("debtAdvisorTemplates", () => {
  const debts = [
    {
      id: "1",
      name: "Small",
      remainingCents: 5_000,
      targetCents: 10_000,
      currentCents: 5_000,
      isCompleted: false,
    },
    {
      id: "2",
      name: "Large",
      remainingCents: 50_000,
      targetCents: 100_000,
      currentCents: 50_000,
      isCompleted: false,
    },
  ];

  it("picks smallest debt for snowball", () => {
    expect(pickSnowballDebt(debts)?.name).toBe("Small");
  });

  it("answers which debt first", () => {
    const reply = buildAdvisorReply("which_debt_first", debts);
    expect(reply).toContain("Small");
  });

  it("answers motivation with freedom date", () => {
    const reply = buildAdvisorReply("motivation", debts);
    expect(reply.length).toBeGreaterThan(10);
  });
});
