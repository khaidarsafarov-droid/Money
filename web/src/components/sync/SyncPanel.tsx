import { RefreshCw, Wifi } from "lucide-react";
import type { WebSyncStatus } from "@/hooks/useWebSync";

interface SyncPanelProps {
  peerUrl: string;
  onPeerUrlChange: (value: string) => void;
  status: WebSyncStatus;
  lastError: string | null;
  lastSyncedAt: number | null;
  onSync: () => void;
}

function statusLabel(status: WebSyncStatus, lastSyncedAt: number | null): string {
  switch (status) {
    case "connecting":
      return "Синхронизация…";
    case "synced":
      return lastSyncedAt
        ? `Синхронизировано ${new Date(lastSyncedAt).toLocaleTimeString()}`
        : "Синхронизировано";
    case "error":
      return "Ошибка синхронизации";
    default:
      return "P2P с Android (WebSocket)";
  }
}

export function SyncPanel({
  peerUrl,
  onPeerUrlChange,
  status,
  lastError,
  lastSyncedAt,
  onSync,
}: SyncPanelProps) {
  const isBusy = status === "connecting";

  return (
    <section className="rounded-2xl border border-milk-surface bg-white/70 p-4 shadow-sm backdrop-blur-sm">
      <div className="mb-3 flex items-center gap-2 text-sm font-medium text-milk-text">
        <Wifi className="size-4 text-milk-accent" aria-hidden />
        <span>{statusLabel(status, lastSyncedAt)}</span>
        <span
          className={`ml-auto size-2 rounded-full ${
            status === "synced" ? "bg-finance-positive" : "bg-milk-muted/50"
          }`}
          aria-hidden
        />
      </div>

      <p className="mb-3 text-xs text-milk-muted">
        Скопируйте URL из меню Android-приложения (⋮ → «URL для веб-синхронизации») и нажмите
        «Синхронизировать».
      </p>

      <div className="flex flex-col gap-2 sm:flex-row">
        <input
          type="url"
          value={peerUrl}
          onChange={(event) => onPeerUrlChange(event.target.value)}
          placeholder="ws://192.168.1.10:54321/sync"
          className="min-w-0 flex-1 rounded-xl border border-milk-surface bg-milk-bg px-3 py-2 text-sm text-milk-text outline-none ring-milk-accent/30 focus:ring-2"
          disabled={isBusy}
        />
        <button
          type="button"
          onClick={onSync}
          disabled={isBusy}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-milk-accent px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
        >
          <RefreshCw className={`size-4 ${isBusy ? "animate-spin" : ""}`} aria-hidden />
          Синхронизировать
        </button>
      </div>

      {lastError ? <p className="mt-2 text-xs text-finance-negative">{lastError}</p> : null}
    </section>
  );
}
