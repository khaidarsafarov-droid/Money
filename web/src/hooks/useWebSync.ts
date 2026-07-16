import { useCallback, useRef, useState } from "react";
import {
  financeStateToSnapshot,
  getOrCreateDeviceId,
  mergeFinanceSnapshotsSymmetric,
  runSyncAsInitiator,
  snapshotToFinanceState,
} from "@money/shared";
import type { FinanceState } from "@/types/finance";
import { enrichFinanceState } from "@/lib/globalFinanceEngine";

export type WebSyncStatus = "idle" | "connecting" | "synced" | "error";

export function useWebSync(
  onApply: (state: FinanceState) => void,
  getLatestState: () => FinanceState,
) {
  const [peerUrl, setPeerUrl] = useState("");
  const [status, setStatus] = useState<WebSyncStatus>("idle");
  const [lastError, setLastError] = useState<string | null>(null);
  const [lastSyncedAt, setLastSyncedAt] = useState<number | null>(null);
  const syncInFlight = useRef(false);

  const syncNow = useCallback(async (): Promise<boolean> => {
    const trimmed = peerUrl.trim();
    if (!trimmed) {
      setStatus("error");
      setLastError("Введите WebSocket URL телефона");
      return false;
    }

    if (syncInFlight.current) {
      setStatus("error");
      setLastError("Синхронизация уже выполняется");
      return false;
    }

    syncInFlight.current = true;
    setStatus("connecting");
    setLastError(null);

    const stateAtStart = getLatestState();

    try {
      const deviceId = getOrCreateDeviceId();
      const local = financeStateToSnapshot(stateAtStart, deviceId);
      const remoteMerged = await runSyncAsInitiator(local, trimmed);

      const latestState = getLatestState();
      const latestSnapshot = financeStateToSnapshot(latestState, deviceId);
      const finalMerged = mergeFinanceSnapshotsSymmetric(latestSnapshot, remoteMerged);

      const nextState = enrichFinanceState(snapshotToFinanceState(finalMerged));
      onApply(nextState);
      setStatus("synced");
      setLastSyncedAt(Date.now());
      return true;
    } catch (error) {
      setStatus("error");
      setLastError(error instanceof Error ? error.message : "Ошибка синхронизации");
      return false;
    } finally {
      syncInFlight.current = false;
    }
  }, [getLatestState, onApply, peerUrl]);

  return {
    peerUrl,
    setPeerUrl,
    status,
    lastError,
    lastSyncedAt,
    syncNow,
  };
}
