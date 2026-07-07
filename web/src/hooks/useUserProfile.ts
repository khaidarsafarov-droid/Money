import { useCallback, useState } from "react";
import {
  DEFAULT_USER_PROFILE,
  type UserProfile,
} from "@money/shared";
import { loadUserProfile, saveUserProfile } from "@/lib/appUserProfileStorage";

export function useUserProfile() {
  const [profile, setProfile] = useState<UserProfile>(() => loadUserProfile());

  const updateProfile = useCallback((patch: Partial<UserProfile>) => {
    setProfile((current) => {
      const next = { ...current, ...patch };
      saveUserProfile(next);
      return next;
    });
  }, []);

  const completeOnboarding = useCallback((finalProfile: Partial<UserProfile>) => {
    const next: UserProfile = {
      ...DEFAULT_USER_PROFILE,
      ...profile,
      ...finalProfile,
      onboardingCompleted: true,
    };
    saveUserProfile(next);
    setProfile(next);
  }, [profile]);

  return {
    profile,
    updateProfile,
    completeOnboarding,
  };
}
