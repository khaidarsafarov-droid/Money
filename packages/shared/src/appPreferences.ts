export interface PrivacyPreferences {
  hideAmounts: boolean;
  hideOnWidget: boolean;
}

export const DEFAULT_PRIVACY: PrivacyPreferences = {
  hideAmounts: false,
  hideOnWidget: false,
};
