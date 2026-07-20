import cors from "cors";
import express from "express";
import { createId, readState, StateValidationError, writeState } from "./store.js";
import type {
  DebtorInput,
  ExpenseInput,
  FinancialState,
  TransferInput,
} from "./types.js";

const app = express();
const PORT = Number(process.env.PORT ?? 3001);
const HOST = process.env.HOST ?? "127.0.0.1";

app.use(cors({ origin: process.env.CORS_ORIGIN ?? "http://localhost:5173" }));
app.use(express.json({ limit: "1mb" }));

function asyncHandler(
  handler: (req: express.Request, res: express.Response) => unknown,
): express.RequestHandler {
  return (req, res, next) => {
    try {
      const result = handler(req, res);
      if (result instanceof Promise) {
        result.catch(next);
      }
    } catch (error) {
      next(error);
    }
  };
}

app.get("/api/health", (_req, res) => {
  res.json({ ok: true, syncedAt: new Date().toISOString() });
});

app.get(
  "/api/finance/state",
  asyncHandler((_req, res) => {
    res.json(readState());
  }),
);

app.put(
  "/api/finance/state",
  asyncHandler((req, res) => {
    const state = writeState(req.body as FinancialState);
    res.json(state);
  }),
);

app.patch(
  "/api/finance/settings",
  asyncHandler((req, res) => {
    const state = readState();
    const { baseCurrency, exchangeRates } = req.body as Partial<FinancialState>;
    const next = {
      ...state,
      ...(baseCurrency ? { baseCurrency } : {}),
      ...(exchangeRates ? { exchangeRates: { ...state.exchangeRates, ...exchangeRates } } : {}),
    };
    res.json(writeState(next));
  }),
);

app.put(
  "/api/finance/goal",
  asyncHandler((req, res) => {
    const state = readState();
    const { targetAmount } = req.body as { targetAmount: NonNullable<FinancialState["goal"]>["targetAmount"] };
    if (!targetAmount || typeof targetAmount.amount !== "number" || targetAmount.amount < 0) {
      res.status(400).json({ error: "Invalid targetAmount" });
      return;
    }
    const next = {
      ...state,
      goal: {
        targetAmount,
        updatedAt: new Date().toISOString(),
      },
    };
    res.json(writeState(next));
  }),
);

app.post(
  "/api/finance/debtors",
  asyncHandler((req, res) => {
    const state = readState();
    const input = req.body as DebtorInput;
    if (!input.name || typeof input.name !== "string") {
      res.status(400).json({ error: "name is required" });
      return;
    }
    const debtor = { ...input, id: createId() };
    const next = { ...state, debtors: [debtor, ...state.debtors] };
    res.status(201).json({ debtor, state: writeState(next) });
  }),
);

app.patch(
  "/api/finance/debtors/:id",
  asyncHandler((req, res) => {
    const state = readState();
    const { id } = req.params;
    const updates = req.body as Partial<DebtorInput>;
    const exists = state.debtors.some((debtor) => debtor.id === id);
    if (!exists) {
      res.status(404).json({ error: "Debtor not found" });
      return;
    }
    const { id: _ignored, ...safeUpdates } = updates as Partial<DebtorInput> & { id?: string };
    const next = {
      ...state,
      debtors: state.debtors.map((debtor) =>
        debtor.id === id ? { ...debtor, ...safeUpdates } : debtor,
      ),
    };
    res.json(writeState(next));
  }),
);

app.delete(
  "/api/finance/debtors/:id",
  asyncHandler((req, res) => {
    const state = readState();
    const { id } = req.params;
    const next = {
      ...state,
      debtors: state.debtors.filter((debtor) => debtor.id !== id),
    };
    res.json(writeState(next));
  }),
);

app.post(
  "/api/finance/transfers",
  asyncHandler((req, res) => {
    const state = readState();
    const input = req.body as TransferInput;
    if (!input.amount || typeof input.amount.amount !== "number" || input.amount.amount < 0) {
      res.status(400).json({ error: "Invalid transfer amount" });
      return;
    }
    const transfer = { ...input, id: createId() };
    const next = { ...state, transfers: [transfer, ...state.transfers] };
    res.status(201).json({ transfer, state: writeState(next) });
  }),
);

app.delete(
  "/api/finance/transfers/:id",
  asyncHandler((req, res) => {
    const state = readState();
    const { id } = req.params;
    const next = {
      ...state,
      transfers: state.transfers.filter((transfer) => transfer.id !== id),
    };
    res.json(writeState(next));
  }),
);

app.post(
  "/api/finance/expenses",
  asyncHandler((req, res) => {
    const state = readState();
    const input = req.body as ExpenseInput;
    if (!input.amount || typeof input.amount.amount !== "number" || input.amount.amount < 0) {
      res.status(400).json({ error: "Invalid expense amount" });
      return;
    }
    if (!input.category || typeof input.category !== "string") {
      res.status(400).json({ error: "category is required" });
      return;
    }
    const expense = { ...input, id: createId() };
    const next = { ...state, expenses: [expense, ...state.expenses] };
    res.status(201).json({ expense, state: writeState(next) });
  }),
);

app.delete(
  "/api/finance/expenses/:id",
  asyncHandler((req, res) => {
    const state = readState();
    const { id } = req.params;
    const next = {
      ...state,
      expenses: state.expenses.filter((expense) => expense.id !== id),
    };
    res.json(writeState(next));
  }),
);

app.use(
  (
    error: Error,
    _req: express.Request,
    res: express.Response,
    _next: express.NextFunction,
  ) => {
    if (error instanceof StateValidationError) {
      res.status(400).json({ error: error.message });
      return;
    }
    console.error("Unhandled API error:", error);
    res.status(500).json({ error: "Internal server error" });
  },
);

app.listen(PORT, HOST, () => {
  console.log(`Money API running on http://${HOST}:${PORT}`);
});
