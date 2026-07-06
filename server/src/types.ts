export type CurrencyCode = "USD" | "EUR" | "RUB" | "GBP";

export interface MonetaryAmount {
  amount: number;
  currency: CurrencyCode;
  exchangeRate: number;
}

export interface Debtor {
  id: string;
  name: string;
  amount: MonetaryAmount;
  dateGiven: string;
  dateDue: string;
  isPaid: boolean;
}

export interface Transfer {
  id: string;
  amount: MonetaryAmount;
  date: string;
}

export interface Expense {
  id: string;
  amount: MonetaryAmount;
  category: string;
  date: string;
  note?: string;
}

export interface SavingsGoal {
  targetAmount: MonetaryAmount;
  updatedAt: string;
}

export interface FinancialState {
  baseCurrency: CurrencyCode;
  exchangeRates: Record<CurrencyCode, number>;
  goal: SavingsGoal | null;
  debtors: Debtor[];
  transfers: Transfer[];
  expenses: Expense[];
}

export type DebtorInput = Omit<Debtor, "id">;
export type TransferInput = Omit<Transfer, "id">;
export type ExpenseInput = Omit<Expense, "id">;
