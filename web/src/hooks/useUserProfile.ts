import { useCallback, useEffect, useState } from "react";
import {
  DEFAULT_USER_PROFILE,
  type UserProfile,
} from "@money/shared";
import { loadUserProfile, saveUserProfile } from "@/lib/appUserProfileStorage";

const profileListeners = new Set<(profile: UserProfile) => void>();

function publishProfile(profile: UserProfile) {
  saveUserProfile(profile);
  queueMicrotask(() => {
    for (const listener of profileListeners) listener(profile);
  });
}

export function useUserProfile() {
  const [profile, setProfile] = useState<UserProfile>(() => loadUserProfile());

  useEffect(() => {
    const listener = (next: UserProfile) => setProfile(next);
    profileListeners.add(listener);
    return () => {
      profileListeners.delete(listener);
    };
  }, []);

  const updateProfile = useCallback((patch: Partial<UserProfile>) => {
    setProfile((current) => {
      const next = { ...current, ...patch };
      publishProfile(next);
      return next;
    });
  }, []);

  const completeOnboarding = useCallback((finalProfile: Partial<UserProfile>) => {
    setProfile((current) => {
      const next: UserProfile = {
        ...DEFAULT_USER_PROFILE,
        ...current,
        ...finalProfile,
        onboardingCompleted: true,
      };
      publishProfile(next);
      return next;
    });
  }, []);

  return {
    profile,
    updateProfile,
    completeOnboarding,
  };
}
