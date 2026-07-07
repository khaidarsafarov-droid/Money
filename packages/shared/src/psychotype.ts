export type PsychotypeAnswer = "a" | "b" | "c" | "d";

export type Psychotype =
  | "SPENDER"      // Транжира
  | "SAVER"        // Эконом
  | "INDIFFERENT"  // Равнодушный
  | "PLAYER"       // Игрок
  | "BALANCED";    // Сбалансированный

export interface PsychotypeQuestion {
  id: string;
  text: string;
  options: { key: PsychotypeAnswer; label: string; mapsTo: Psychotype }[];
}

export const PSYCHOTYPE_QUESTIONS: PsychotypeQuestion[] = [
  {
    id: "impulse",
    text: "Когда вы видите интересную вещь, вы обычно…",
    options: [
      { key: "a", label: "Сразу покупаете, если есть деньги", mapsTo: "SPENDER" },
      { key: "b", label: "Думаете неделю, ищете скидки", mapsTo: "SAVER" },
      { key: "c", label: "Проходите мимо, это неважно", mapsTo: "INDIFFERENT" },
      { key: "d", label: "Проверяете, сможете ли выгодно продать позже", mapsTo: "PLAYER" },
    ],
  },
  {
    id: "windfall",
    text: "Как вы реагируете на неожиданный доход (премия, подарок)?",
    options: [
      { key: "a", label: "Трачу на удовольствия", mapsTo: "SPENDER" },
      { key: "b", label: "Откладываю 90% на счёт", mapsTo: "SAVER" },
      { key: "c", label: "Не придаю значения", mapsTo: "INDIFFERENT" },
      { key: "d", label: "Ищу, куда выгоднее вложить", mapsTo: "PLAYER" },
    ],
  },
  {
    id: "credit",
    text: "Ваше отношение к кредитам:",
    options: [
      { key: "a", label: "Нормально, если хочется что-то купить", mapsTo: "SPENDER" },
      { key: "b", label: "Кредиты — зло, стараюсь избегать", mapsTo: "SAVER" },
      { key: "c", label: "Не думаю об этом", mapsTo: "INDIFFERENT" },
      { key: "d", label: "Могу взять, если есть риск и прибыль", mapsTo: "PLAYER" },
    ],
  },
  {
    id: "debts_attitude",
    text: "Когда у вас есть долги, вы…",
    options: [
      { key: "a", label: "Стараюсь не думать о них", mapsTo: "SPENDER" },
      { key: "b", label: "Постоянно переживаю и проверяю счета", mapsTo: "SAVER" },
      { key: "c", label: "Спокоен, у меня есть план", mapsTo: "BALANCED" },
      { key: "d", label: "Ищу способ заработать быстрее", mapsTo: "PLAYER" },
    ],
  },
  {
    id: "money_mindset",
    text: "Ваша главная мысль о деньгах:",
    options: [
      { key: "a", label: "Деньги созданы для трат", mapsTo: "SPENDER" },
      { key: "b", label: "Деньги нужно копить на чёрный день", mapsTo: "SAVER" },
      { key: "c", label: "Деньги не главное в жизни", mapsTo: "INDIFFERENT" },
      { key: "d", label: "Деньги — инструмент для достижения целей", mapsTo: "BALANCED" },
    ],
  },
];

export function determinePsychotype(answers: PsychotypeAnswer[]): Psychotype {
  if (answers.length === 0) return "BALANCED";

  const counts: Record<Psychotype, number> = {
    SPENDER: 0,
    SAVER: 0,
    INDIFFERENT: 0,
    PLAYER: 0,
    BALANCED: 0,
  };

  answers.forEach((answer, index) => {
    const question = PSYCHOTYPE_QUESTIONS[index];
    const option = question?.options.find((o) => o.key === answer);
    if (option) counts[option.mapsTo] += 1;
  });

  const max = Math.max(...Object.values(counts));
  const winners = (Object.entries(counts) as [Psychotype, number][])
    .filter(([, count]) => count === max)
    .map(([type]) => type);

  if (winners.length !== 1) return "BALANCED";
  return winners[0]!;
}

export function psychotypeLabelRu(type: Psychotype): string {
  switch (type) {
    case "SPENDER":
      return "Транжира";
    case "SAVER":
      return "Эконом";
    case "INDIFFERENT":
      return "Равнодушный";
    case "PLAYER":
      return "Игрок";
    case "BALANCED":
      return "Сбалансированный";
    default: {
      const _exhaustive: never = type;
      return String(_exhaustive);
    }
  }
}

export function psychotypeTaglineRu(type: Psychotype): string {
  switch (type) {
    case "SPENDER":
      return "Деньги любят счёт — и уют 🔥";
    case "SAVER":
      return "Деньги — это энергия, дайте ей течь";
    case "INDIFFERENT":
      return "План важнее эмоций";
    case "PLAYER":
      return "Каждый платёж — очко к победе";
    case "BALANCED":
      return "Шаг за шагом к свободе";
    default: {
      const _exhaustive: never = type;
      return String(_exhaustive);
    }
  }
}

export interface PsychotypeUiFlags {
  showPaydayCountdown: boolean;
  showAutoSave: boolean;
  showOverspendWarning: boolean;
  showTreatYourself: boolean;
  showFreedomDate: boolean;
  showPersonalWhy: boolean;
  showChallenges: boolean;
  showPoints: boolean;
  tone: "energetic" | "gentle" | "calm" | "playful" | "neutral";
}

export function psychotypeUiFlags(type: Psychotype): PsychotypeUiFlags {
  switch (type) {
    case "SPENDER":
      return {
        showPaydayCountdown: true,
        showAutoSave: true,
        showOverspendWarning: true,
        showTreatYourself: false,
        showFreedomDate: true,
        showPersonalWhy: false,
        showChallenges: false,
        showPoints: false,
        tone: "energetic",
      };
    case "SAVER":
      return {
        showPaydayCountdown: false,
        showAutoSave: false,
        showOverspendWarning: false,
        showTreatYourself: true,
        showFreedomDate: true,
        showPersonalWhy: false,
        showChallenges: false,
        showPoints: false,
        tone: "gentle",
      };
    case "INDIFFERENT":
      return {
        showPaydayCountdown: false,
        showAutoSave: false,
        showOverspendWarning: false,
        showTreatYourself: false,
        showFreedomDate: true,
        showPersonalWhy: true,
        showChallenges: false,
        showPoints: false,
        tone: "calm",
      };
    case "PLAYER":
      return {
        showPaydayCountdown: false,
        showAutoSave: false,
        showOverspendWarning: false,
        showTreatYourself: false,
        showFreedomDate: true,
        showPersonalWhy: false,
        showChallenges: true,
        showPoints: true,
        tone: "playful",
      };
    case "BALANCED":
      return {
        showPaydayCountdown: true,
        showAutoSave: true,
        showOverspendWarning: true,
        showTreatYourself: true,
        showFreedomDate: true,
        showPersonalWhy: true,
        showChallenges: true,
        showPoints: true,
        tone: "neutral",
      };
    default: {
      const _exhaustive: never = type;
      return psychotypeUiFlags("BALANCED");
    }
  }
}
