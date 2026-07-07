import { describe, expect, it } from "vitest";
import { determinePsychotype, psychotypeLabelRu } from "./psychotype";
import { affirmationsForFears } from "./userFears";
import { buildPayoffPlan } from "./goalHubEngine";
import { isChecklistComplete, emptyWeeklyChecklist } from "./weeklyChecklist";

describe("psychotype", () => {
  it("detects spender majority", () => {
    const type = determinePsychotype(["a", "a", "a", "a", "a"]);
    expect(type).toBe("SPENDER");
    expect(psychotypeLabelRu(type)).toBe("Транжира");
  });

  it("returns balanced on tie", () => {
    expect(determinePsychotype(["a", "b", "c", "a", "b"])).toBe("BALANCED");
  });
});

describe("userFears", () => {
  it("returns affirmations for selected fears", () => {
    const lines = affirmationsForFears(["ashamed_of_debt"], 10);
    expect(lines[0]).toContain("опыт");
  });
});

describe("goalHubEngine", () => {
  it("builds snowball payoff plan", () => {
    const plan = buildPayoffPlan(
      [
        {
          id: "1",
          name: "Big",
          remainingCents: 10_000,
          targetCents: 10_000,
          currentCents: 0,
          isCompleted: false,
          payments: [],
          plannedMonthlyCents: 5_000,
        },
        {
          id: "2",
          name: "Small",
          remainingCents: 2_000,
          targetCents: 2_000,
          currentCents: 0,
          isCompleted: false,
          payments: [],
          plannedMonthlyCents: 1_000,
        },
      ],
      "snowball",
      Date.UTC(2026, 6, 1),
    );
    expect(plan[0]?.name).toBe("Small");
  });
});

describe("weeklyChecklist", () => {
  it("detects complete week", () => {
    const state = {
      ...emptyWeeklyChecklist(),
      checkedDebts: true,
      madePayment: true,
      savedEmergency: true,
      pleasureOrRestraint: true,
      luckDiaryEntry: true,
    };
    expect(isChecklistComplete(state)).toBe(true);
  });
});
