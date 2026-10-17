import { RefreshCw, Wifi } from "lucide-react";
import type { WebSyncStatus } from "@/hooks/useWebSync";
import { AppIcon, SurfaceCard } from "@/components/ui/SurfaceCard";

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
    <SurfaceCard className="space-y-3">
      <div className="flex items-center gap-2 text-sm text-milk-text">
        <AppIcon icon={Wifi} size={14} />
        <span>{statusLabel(status, lastSyncedAt)}</span>
        <span
          className={`ml-auto size-1.5 rounded-full ${
            status === "synced" ? "bg-finance-positive" : "bg-milk-surface"
          }`}
          aria-hidden
        />
      </div>

      <p className="text-xs text-milk-muted">
        URL на экране синхронизации Android.
      </p>

      <div className="flex flex-col gap-2 sm:flex-row">
        <input
          type="url"
          value={peerUrl}
          onChange={(event) => onPeerUrlChange(event.target.value)}
          placeholder="ws://192.168.1.10:54321/sync"
          className="min-w-0 flex-1 rounded-md border border-milk-surface bg-milk-glass px-3 py-2 text-sm text-milk-text outline-none focus:border-milk-accent"
          disabled={isBusy}
        />
        <button
          type="button"
          onClick={onSync}
          disabled={isBusy}
          className="inline-flex items-center justify-center gap-1.5 rounded-md border border-milk-surface bg-milk-glass px-3 py-2 text-sm text-milk-text transition hover:bg-milk-bg disabled:opacity-50"
        >
          <RefreshCw className={`size-3.5 ${isBusy ? "animate-spin" : ""}`} strokeWidth={1.25} aria-hidden />
          Синхронизировать
        </button>
      </div>

      {lastError ? <p className="text-xs text-finance-negative">{lastError}</p> : null}
    </SurfaceCard>
  );
}
