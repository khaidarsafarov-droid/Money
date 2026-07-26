/** Cross-platform Project Money constants (Master Blueprint). */

export const SYNC_SCHEMA_VERSION = 3;

export const CURRENCY_CODE = "USD" as const;

/** Milk-Rich + semantic finance colors. */
export const PROJECT_MONEY_THEME = {
  background: "#F9F7F2",
  surface: "#E2DCD0",
  text: "#2D2A26",
  accentGold: "#C9A24D",
  financePositive: "#4A7C59",
  financeNegative: "#B35D5D",
} as const;

/** NSD service type for Android P2P (trailing dot required by NSD API). */
export const NSD_SERVICE_TYPE = "_money._tcp.";

/** WebSocket path on Ktor server. Port is dynamic (0) and published via NSD. */
export const SYNC_WEBSOCKET_PATH = "/sync";
