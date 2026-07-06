import type { FinanceSnapshot } from "./sync";
import { mergeFinanceSnapshotsSymmetric } from "./syncEngine";
import { decodeFinanceSnapshot, encodeFinanceSnapshot } from "./snapshotCodec";

export const SYNC_ACK = "ACK";

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
    await waitForAck(socket);
    return merged;
  } finally {
    socket.close();
  }
}

function openWebSocket(url: string): Promise<WebSocket> {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    socket.addEventListener("open", () => resolve(socket), { once: true });
    socket.addEventListener("error", () => reject(new Error("WebSocket connection failed")), {
      once: true,
    });
  });
}

function readTextFrame(socket: WebSocket): Promise<string> {
  return new Promise((resolve, reject) => {
    socket.addEventListener(
      "message",
      (event) => {
        if (typeof event.data === "string") {
          resolve(event.data);
          return;
        }
        reject(new Error("Unexpected binary frame during sync"));
      },
      { once: true },
    );
    socket.addEventListener("error", () => reject(new Error("WebSocket read failed")), {
      once: true,
    });
  });
}

function waitForAck(socket: WebSocket): Promise<void> {
  return new Promise((resolve, reject) => {
    socket.addEventListener(
      "message",
      (event) => {
        if (event.data === SYNC_ACK) {
          resolve();
          return;
        }
        reject(new Error("Unexpected message while waiting for ACK"));
      },
      { once: true },
    );
    socket.addEventListener("error", () => reject(new Error("WebSocket ACK failed")), {
      once: true,
    });
  });
}
