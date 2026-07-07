export type LuckDiaryCategory =
  | "UNEXPECTED_INCOME"
  | "SAVINGS"
  | "LUCK"
  | "GIFT"
  | "OTHER";

export interface LuckDiaryEntry {
  id: string;
  dateMs: number;
  note: string;
  amountCents?: number;
  category: LuckDiaryCategory;
}

export function luckCategoryLabelRu(category: LuckDiaryCategory): string {
  switch (category) {
    case "UNEXPECTED_INCOME":
      return "Неожиданный доход";
    case "SAVINGS":
      return "Экономия";
    case "LUCK":
      return "Удача";
    case "GIFT":
      return "Подарок";
    case "OTHER":
      return "Другое";
    default: {
      const _exhaustive: never = category;
      return String(_exhaustive);
    }
  }
}

export function monthlyLuckSumCents(
  entries: LuckDiaryEntry[],
  nowMs = Date.now(),
): number {
  const now = new Date(nowMs);
  const month = now.getUTCMonth();
  const year = now.getUTCFullYear();

  return entries
    .filter((e) => {
      const d = new Date(e.dateMs);
      return d.getUTCMonth() === month && d.getUTCFullYear() === year;
    })
    .reduce((sum, e) => sum + (e.amountCents ?? 0), 0);
}

export function entriesThisMonth(
  entries: LuckDiaryEntry[],
  nowMs = Date.now(),
): LuckDiaryEntry[] {
  const now = new Date(nowMs);
  const month = now.getUTCMonth();
  const year = now.getUTCFullYear();

  return entries
    .filter((e) => {
      const d = new Date(e.dateMs);
      return d.getUTCMonth() === month && d.getUTCFullYear() === year;
    })
    .sort((a, b) => b.dateMs - a.dateMs);
}
