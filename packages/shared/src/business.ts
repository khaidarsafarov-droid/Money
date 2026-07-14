export type FinanceBranch = 'PERSONAL' | 'BUSINESS';

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE';

export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED';

export type BusinessTransactionType = 'INCOME' | 'EXPENSE';

export type BusinessLedgerType = 'OPERATING' | 'INVESTMENT' | 'TAX' | 'PERSONAL';

export interface BusinessClient {
  id: string;
  name: string;
  contactInfo?: string;
  email?: string;
  phone?: string;
  isActive: boolean;
}

export interface BusinessProject {
  id: string;
  clientId: string;
  name: string;
  status: ProjectStatus;
  budgetCents: number;
}

export interface BusinessInvoice {
  id: string;
  clientId: string;
  projectId?: string;
  issueDate: number;
  dueDate: number;
  amountCents: number;
  status: InvoiceStatus;
}

export interface BusinessTransaction {
  id: string;
  type: BusinessTransactionType;
  amountCents: number;
  category: string;
  ledgerType: BusinessLedgerType;
  clientId?: string;
  projectId?: string;
  invoiceId?: string;
  date: number;
  note?: string;
}

export const BUSINESS_REVENUE_CATEGORIES = [
  'Revenue',
  'Services',
  'Products',
  'Consulting',
  'Other income',
] as const;

export const BUSINESS_EXPENSE_CATEGORIES = [
  'COGS',
  'Marketing',
  'Software',
  'Travel',
  'Taxes',
  'Rent',
  'Payroll',
  'Utilities',
  'Other expense',
] as const;
