export type LoanCategory =
  | "CREDIT_CARD"
  | "CONSUMER"
  | "AUTO"
  | "MORTGAGE"
  | "EDUCATION"
  | "OTHER";

export const LOAN_CATEGORIES: LoanCategory[] = [
  "CREDIT_CARD",
  "CONSUMER",
  "AUTO",
  "MORTGAGE",
  "EDUCATION",
  "OTHER",
];

export function isHighPriorityLoanCategory(category: LoanCategory): boolean {
  return category === "CREDIT_CARD" || category === "CONSUMER";
}

export function loanCategoryLabelRu(category: LoanCategory): string {
  switch (category) {
    case "CREDIT_CARD":
      return "Кредитная карта";
    case "CONSUMER":
      return "Потребительский";
    case "AUTO":
      return "Авто";
    case "MORTGAGE":
      return "Ипотека";
    case "EDUCATION":
      return "Обучение";
    case "OTHER":
      return "Другое";
    default: {
      const _exhaustive: never = category;
      return String(_exhaustive);
    }
  }
}
