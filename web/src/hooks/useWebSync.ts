import { useCallback, useState } from "react";
import {
  financeStateToSnapshot,
  getOrCreateDeviceId,
  runSyncAsInitiator,
  snapshotToFinanceState,
} from "@money/shared";
import type { FinanceState } from "@/types/finance";
import { enrichFinanceState } from "@/lib/globalFinanceEngine";

export type WebSyncStatus = "idle" | "connecting" | "synced" | "error";

export function useWebSync(onApply: (state: FinanceState) => void) {
  const [peerUrl, setPeerUrl] = useState("");
  const [status, setStatus] = useState<WebSyncStatus>("idle");
  const [lastError, setLastError] = useState<string | null>(null);
  const [lastSyncedAt, setLastSyncedAt] = useState<number | null>(null);

  const syncNow = useCallback(
    async (state: FinanceState): Promise<boolean> => {
      const trimmed = peerUrl.trim();
      if (!trimmed) {
        setStatus("error");
        setLastError("Введите WebSocket URL телефона");
        return false;
      }

      setStatus("connecting");
      setLastError(null);

      try {
        const deviceId = getOrCreateDeviceId();
        const local = financeStateToSnapshot(state, deviceId);
        const merged = await runSyncAsInitiator(local, trimmed);
        const nextState = enrichFinanceState(snapshotToFinanceState(merged));
        onApply(nextState);
        setStatus("synced");
        setLastSyncedAt(Date.now());
        return true;
      } catch (error) {
        setStatus("error");
        setLastError(error instanceof Error ? error.message : "Ошибка синхронизации");
        return false;
      }
    },
    [onApply, peerUrl],
  );

  return {
    peerUrl,
    setPeerUrl,
    status,
    lastError,
    lastSyncedAt,
    syncNow,
  };
}
