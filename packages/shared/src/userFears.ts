export type UserFearId =
  | "wont_escape"
  | "earn_more_than_parents"
  | "ashamed_of_debt"
  | "money_corrupts"
  | "rich_are_bad"
  | "afraid_of_numbers";

export interface UserFearOption {
  id: UserFearId;
  label: string;
  affirmation: string;
}

export const USER_FEAR_OPTIONS: UserFearOption[] = [
  {
    id: "wont_escape",
    label: "Боюсь, что у меня не получится выбраться из долгов",
    affirmation: "Каждый платёж приближает вас к цели. Вы уже сделали первый шаг.",
  },
  {
    id: "earn_more_than_parents",
    label: "Чувствую вину, когда зарабатываю больше родителей",
    affirmation: "Ваш успех — это благодарность, а не предательство.",
  },
  {
    id: "ashamed_of_debt",
    label: "Мне стыдно, что я в долгах",
    affirmation: "Долги — это опыт, а не приговор. Вы справитесь.",
  },
  {
    id: "money_corrupts",
    label: "Боюсь, что деньги меня испортят",
    affirmation: "Вы решаете, каким человеком быть — деньги лишь инструмент.",
  },
  {
    id: "rich_are_bad",
    label: "Думаю, что богатые — это плохие люди",
    affirmation: "Деньги могут служить вам и близким.",
  },
  {
    id: "afraid_of_numbers",
    label: "Мне страшно даже смотреть на цифры",
    affirmation: "Сегодня посмотрите на свой прогресс — вы стали ближе к цели.",
  },
];

export function affirmationsForFears(
  selectedFearIds: UserFearId[],
  progressPercent = 0,
): string[] {
  const base = USER_FEAR_OPTIONS
    .filter((f) => selectedFearIds.includes(f.id))
    .map((f) => {
      if (f.id === "afraid_of_numbers" && progressPercent > 0) {
        return `Сегодня посмотрите на свой прогресс. Вы стали ближе на ${progressPercent}%.`;
      }
      return f.affirmation;
    });

  if (base.length === 0) {
    return ["Каждый шаг имеет значение."];
  }
  return base;
}
