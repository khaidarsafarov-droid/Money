const DEVICE_ID_KEY = "money-device-id";

export function getOrCreateDeviceId(storage: Storage = localStorage): string {
  const existing = storage.getItem(DEVICE_ID_KEY);
  if (existing) return existing;
  const created = crypto.randomUUID();
  storage.setItem(DEVICE_ID_KEY, created);
  return created;
}
