import { mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { FinancialState } from "./types.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DB_PATH = join(__dirname, "..", "data", "db.json");

const DEFAULT_STATE: FinancialState = {
  baseCurrency: "USD",
  exchangeRates: { USD: 1, EUR: 0.92, RUB: 92.5, GBP: 0.79 },
  goal: {
    targetAmount: { amount: 10_000, currency: "USD", exchangeRate: 1 },
    updatedAt: new Date().toISOString(),
  },
  debtors: [],
  transfers: [],
  expenses: [],
};

function ensureDbFile(): void {
  mkdirSync(dirname(DB_PATH), { recursive: true });
  try {
    readFileSync(DB_PATH, "utf8");
  } catch {
    writeFileSync(DB_PATH, JSON.stringify(DEFAULT_STATE, null, 2), "utf8");
  }
}

export function readState(): FinancialState {
  ensureDbFile();
  const raw = readFileSync(DB_PATH, "utf8");
  try {
    const parsed = JSON.parse(raw) as FinancialState;
    return validateState(parsed);
  } catch (error) {
    if (error instanceof StateValidationError) throw error;
    throw new StateValidationError("Corrupt database file: invalid JSON");
  }
}

export function writeState(state: FinancialState): FinancialState {
  const validated = validateState(state);
  ensureDbFile();
  const tempPath = `${DB_PATH}.tmp`;
  writeFileSync(tempPath, JSON.stringify(validated, null, 2), "utf8");
  renameSync(tempPath, DB_PATH);
  return validated;
}

export function createId(): string {
  return crypto.randomUUID();
}

export class StateValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "StateValidationError";
  }
}

const ALLOWED_CURRENCIES = new Set(["USD", "EUR", "RUB", "GBP"]);

function validateState(state: unknown): FinancialState {
  if (!state || typeof state !== "object") {
    throw new StateValidationError("State must be an object");
  }

  const candidate = state as FinancialState;

  if (!ALLOWED_CURRENCIES.has(candidate.baseCurrency)) {
    throw new StateValidationError("Invalid baseCurrency");
  }

  if (!candidate.exchangeRates || typeof candidate.exchangeRates !== "object") {
    throw new StateValidationError("exchangeRates must be an object");
  }

  if (!Array.isArray(candidate.debtors)) {
    throw new StateValidationError("debtors must be an array");
  }
  if (!Array.isArray(candidate.transfers)) {
    throw new StateValidationError("transfers must be an array");
  }
  if (!Array.isArray(candidate.expenses)) {
    throw new StateValidationError("expenses must be an array");
  }

  for (const debtor of candidate.debtors) {
    if (!debtor.id || typeof debtor.name !== "string" || debtor.name.length === 0) {
      throw new StateValidationError("Invalid debtor record");
    }
    if (!debtor.amount || typeof debtor.amount.amount !== "number" || debtor.amount.amount < 0) {
      throw new StateValidationError("Invalid debtor amount");
    }
  }

  return candidate;
}
