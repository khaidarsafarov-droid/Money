export interface ReframingLabels {
  youOwe: string;
  remainingToPay: string;
  overdueDebts: string;
  myObligations: string;
  freedomProject: string;
}

const POSITIVE: ReframingLabels = {
  youOwe: "Мои обязательства",
  remainingToPay: "Осталось инвестировать в себя",
  overdueDebts: "Долги, требующие внимания",
  myObligations: "Мои обязательства",
  freedomProject: "Ваш проект «Свобода» требует",
};

const STANDARD: ReframingLabels = {
  youOwe: "Я должен",
  remainingToPay: "Осталось заплатить",
  overdueDebts: "Просроченные долги",
  myObligations: "Я должен",
  freedomProject: "Осталось погасить",
};

export function getReframingLabels(wordPowerEnabled: boolean): ReframingLabels {
  return wordPowerEnabled ? POSITIVE : STANDARD;
}

export function reframeRemaining(amountLabel: string, wordPowerEnabled: boolean): string {
  const labels = getReframingLabels(wordPowerEnabled);
  return `${labels.remainingToPay}: ${amountLabel}`;
}

export function reframeTotalDebt(amountLabel: string, wordPowerEnabled: boolean): string {
  const labels = getReframingLabels(wordPowerEnabled);
  return `${labels.freedomProject} ${amountLabel}`;
}
