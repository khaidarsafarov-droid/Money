import type { FinanceSnapshot } from "./sync";
import { mergeFinanceSnapshotsSymmetric } from "./syncEngine";
import { decodeFinanceSnapshot, encodeFinanceSnapshot } from "./snapshotCodec";

export const SYNC_ACK = "ACK";

const WS_CONNECT_TIMEOUT_MS = 15_000;
const WS_READ_TIMEOUT_MS = 30_000;

export async function runSyncAsInitiator(
  local: FinanceSnapshot,
  webSocketUrl: string,
): Promise<FinanceSnapshot> {
  const socket = await openWebSocket(webSocketUrl);
  try {
    socket.send(encodeFinanceSnapshot(local));
    const remoteRaw = await readTextFrame(socket);
    const remote = decodeFinanceSnapshot(remoteRaw);
    return mergeFinanceSnapshotsSymmetric(local, remote);
  } finally {
    socket.close();
  }
}

export async function runSyncAsAcceptor(
  local: FinanceSnapshot,
  webSocketUrl: string,
): Promise<FinanceSnapshot> {
  const socket = await openWebSocket(webSocketUrl);
  try {
    const remoteRaw = await readTextFrame(socket);
    const remote = decodeFinanceSnapshot(remoteRaw);
    socket.send(encodeFinanceSnapshot(local));
    const merged = mergeFinanceSnapshotsSymmetric(local, remote);
    socket.send(SYNC_ACK);
    return merged;
  } finally {
    socket.close();
  }
}

function openWebSocket(url: string): Promise<WebSocket> {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    const timer = window.setTimeout(() => {
      socket.close();
      reject(new Error("WebSocket connection timed out"));
    }, WS_CONNECT_TIMEOUT_MS);

    socket.addEventListener(
      "open",
      () => {
        window.clearTimeout(timer);
        resolve(socket);
      },
      { once: true },
    );
    socket.addEventListener(
      "error",
      () => {
        window.clearTimeout(timer);
        reject(new Error("WebSocket connection failed"));
      },
      { once: true },
    );
  });
}

function readTextFrame(socket: WebSocket): Promise<string> {
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => {
      reject(new Error("WebSocket read timed out"));
    }, WS_READ_TIMEOUT_MS);

    const cleanup = () => window.clearTimeout(timer);

    socket.addEventListener(
      "message",
      (event) => {
        cleanup();
        if (typeof event.data === "string") {
          resolve(event.data);
          return;
        }
        reject(new Error("Unexpected binary frame during sync"));
      },
      { once: true },
    );
    socket.addEventListener(
      "error",
      () => {
        cleanup();
        reject(new Error("WebSocket read failed"));
      },
      { once: true },
    );
  });
}
