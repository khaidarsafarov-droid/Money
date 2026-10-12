import { describe, expect, it } from "vitest";
import { noteTotalCents, type WebNote } from "./notesStore";

describe("noteTotalCents", () => {
  it("sums financial lines", () => {
    const note: WebNote = {
      id: "n",
      title: "Рынок",
      kind: "FINANCIAL",
      body: "",
      mark: "NONE",
      closed: false,
      createdAt: 1,
      lines: [
        { id: "a", label: "Хлеб", amountCents: 20_000 },
        { id: "b", label: "Молоко", amountCents: 30_000 },
      ],
    };
    expect(noteTotalCents(note)).toBe(50_000);
  });
});
