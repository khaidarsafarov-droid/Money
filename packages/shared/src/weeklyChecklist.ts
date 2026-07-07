export interface WeeklyChecklistState {
  weekKey: string;
  checkedDebts: boolean;
  madePayment: boolean;
  savedEmergency: boolean;
  pleasureOrRestraint: boolean;
  luckDiaryEntry: boolean;
}

export const WEEKLY_CHECKLIST_ITEMS = [
  { key: "checkedDebts" as const, label: "Проверил все свои долги" },
  { key: "madePayment" as const, label: "Внёс хотя бы один платёж" },
  { key: "savedEmergency" as const, label: "Отложил деньги на непредвиденные расходы" },
  { key: "pleasureOrRestraint" as const, label: "Потратил на удовольствие / не потратил лишнего" },
  { key: "luckDiaryEntry" as const, label: "Написал одну запись в Дневник удач" },
];

export function weekKeyFromDate(nowMs = Date.now()): string {
  const d = new Date(nowMs);
  const year = d.getUTCFullYear();
  const oneJan = new Date(Date.UTC(year, 0, 1));
  const week = Math.ceil(((d.getTime() - oneJan.getTime()) / 86_400_000 + oneJan.getUTCDay() + 1) / 7);
  return `${year}-W${week}`;
}

export function isChecklistComplete(state: WeeklyChecklistState): boolean {
  return (
    state.checkedDebts &&
    state.madePayment &&
    state.savedEmergency &&
    state.pleasureOrRestraint &&
    state.luckDiaryEntry
  );
}

export function emptyWeeklyChecklist(nowMs = Date.now()): WeeklyChecklistState {
  return {
    weekKey: weekKeyFromDate(nowMs),
    checkedDebts: false,
    madePayment: false,
    savedEmergency: false,
    pleasureOrRestraint: false,
    luckDiaryEntry: false,
  };
}
