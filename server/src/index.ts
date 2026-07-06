import cors from "cors";
import express from "express";
import { createId, readState, writeState } from "./store.js";
import type {
  DebtorInput,
  ExpenseInput,
  FinancialState,
  TransferInput,
} from "./types.js";

const app = express();
const PORT = Number(process.env.PORT ?? 3001);

app.use(cors());
app.use(express.json());

app.get("/api/health", (_req, res) => {
  res.json({ ok: true, syncedAt: new Date().toISOString() });
});

app.get("/api/finance/state", (_req, res) => {
  res.json(readState());
});

app.put("/api/finance/state", (req, res) => {
  const state = req.body as FinancialState;
  writeState(state);
  res.json(state);
});

app.patch("/api/finance/settings", (req, res) => {
  const state = readState();
  const { baseCurrency, exchangeRates } = req.body as Partial<FinancialState>;
  const next = {
    ...state,
    ...(baseCurrency ? { baseCurrency } : {}),
    ...(exchangeRates ? { exchangeRates: { ...state.exchangeRates, ...exchangeRates } } : {}),
  };
  res.json(writeState(next));
});

app.put("/api/finance/goal", (req, res) => {
  const state = readState();
  const { targetAmount } = req.body as { targetAmount: NonNullable<FinancialState["goal"]>["targetAmount"] };
  const next = {
    ...state,
    goal: {
      targetAmount,
      updatedAt: new Date().toISOString(),
    },
  };
  res.json(writeState(next));
});

app.post("/api/finance/debtors", (req, res) => {
  const state = readState();
  const input = req.body as DebtorInput;
  const debtor = { ...input, id: createId() };
  const next = { ...state, debtors: [debtor, ...state.debtors] };
  res.status(201).json({ debtor, state: writeState(next) });
});

app.patch("/api/finance/debtors/:id", (req, res) => {
  const state = readState();
  const { id } = req.params;
  const updates = req.body as Partial<DebtorInput>;
  const next = {
    ...state,
    debtors: state.debtors.map((debtor) =>
      debtor.id === id ? { ...debtor, ...updates } : debtor,
    ),
  };
  res.json(writeState(next));
});

app.delete("/api/finance/debtors/:id", (req, res) => {
  const state = readState();
  const { id } = req.params;
  const next = {
    ...state,
    debtors: state.debtors.filter((debtor) => debtor.id !== id),
  };
  res.json(writeState(next));
});

app.post("/api/finance/transfers", (req, res) => {
  const state = readState();
  const input = req.body as TransferInput;
  const transfer = { ...input, id: createId() };
  const next = { ...state, transfers: [transfer, ...state.transfers] };
  res.status(201).json({ transfer, state: writeState(next) });
});

app.delete("/api/finance/transfers/:id", (req, res) => {
  const state = readState();
  const { id } = req.params;
  const next = {
    ...state,
    transfers: state.transfers.filter((transfer) => transfer.id !== id),
  };
  res.json(writeState(next));
});

app.post("/api/finance/expenses", (req, res) => {
  const state = readState();
  const input = req.body as ExpenseInput;
  const expense = { ...input, id: createId() };
  const next = { ...state, expenses: [expense, ...state.expenses] };
  res.status(201).json({ expense, state: writeState(next) });
});

app.delete("/api/finance/expenses/:id", (req, res) => {
  const state = readState();
  const { id } = req.params;
  const next = {
    ...state,
    expenses: state.expenses.filter((expense) => expense.id !== id),
  };
  res.json(writeState(next));
});

app.listen(PORT, () => {
  console.log(`Money API running on http://localhost:${PORT}`);
});
