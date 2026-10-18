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
      <div className="flex items-center gap-2 text-sm text-on-surface">
        <AppIcon icon={Wifi} size={14} />
        <span>{statusLabel(status, lastSyncedAt)}</span>
        <span
          className={`ml-auto size-1.5 rounded-full ${
            status === "synced" ? "bg-primary" : "bg-surface-variant"
          }`}
          aria-hidden
        />
      </div>

      <label className="block space-y-1.5">
        <span className="text-sm text-on-surface-variant">Адрес телефона</span>
        <div className="flex flex-col gap-2 sm:flex-row">
        <input
          type="url"
          name="peer-url"
          autoComplete="off"
          value={peerUrl}
          onChange={(event) => onPeerUrlChange(event.target.value)}
          placeholder="ws://192.168.1.10:54321/sync"
          className="field-control min-w-0 flex-1 bg-surface-container-lowest"
          disabled={isBusy}
        />
        <button
          type="button"
          onClick={onSync}
          disabled={isBusy}
          className="inline-flex min-h-12 items-center justify-center gap-1.5 rounded-full border border-outline-variant bg-surface-container-lowest px-4 text-sm text-on-surface transition hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-50"
        >
          <RefreshCw className={`size-3.5 ${isBusy ? "animate-spin" : ""}`} strokeWidth={1.25} aria-hidden />
          Синхронизировать
        </button>
        </div>
        {lastError ? (
          <span className="block text-sm text-error" role="alert">
            {lastError}
          </span>
        ) : null}
      </label>
    </SurfaceCard>
  );
}
