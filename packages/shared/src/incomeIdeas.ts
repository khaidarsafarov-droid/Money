export interface IncomeResource {
  title: string;
  description: string;
  url: string;
}

export const INCOME_RESOURCES: IncomeResource[] = [
  {
    title: "Upwork",
    description: "Фриланс: дизайн, разработка, тексты",
    url: "https://www.upwork.com",
  },
  {
    title: "Fiverr",
    description: "Небольшие заказы и микроуслуги",
    url: "https://www.fiverr.com",
  },
  {
    title: "Prolific",
    description: "Оплачиваемые исследования и опросы",
    url: "https://www.prolific.com",
  },
  {
    title: "Kwork",
    description: "Русскоязычная биржа микрозадач",
    url: "https://kwork.ru",
  },
];

export function calculateExtraIncomeNeeded(
  totalRemainingCents: number,
  monthsToAccelerate: number,
): number {
  if (totalRemainingCents <= 0 || monthsToAccelerate <= 0) return 0;
  return Math.ceil(totalRemainingCents / monthsToAccelerate);
}

export function formatExtraIncomeHint(
  totalRemainingCents: number,
  monthsToAccelerate: number,
): string {
  const needed = calculateExtraIncomeNeeded(totalRemainingCents, monthsToAccelerate);
  const amount = (needed / 100).toFixed(0);
  return `Чтобы закрыть все долги на ${monthsToAccelerate} мес. быстрее, нужно ~$${amount}/мес дополнительного дохода.`;
}
