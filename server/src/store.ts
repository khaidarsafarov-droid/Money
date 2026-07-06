import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
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
  return JSON.parse(raw) as FinancialState;
}

export function writeState(state: FinancialState): FinancialState {
  ensureDbFile();
  writeFileSync(DB_PATH, JSON.stringify(state, null, 2), "utf8");
  return state;
}

export function createId(): string {
  return crypto.randomUUID();
}
