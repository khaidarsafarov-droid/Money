import { useState } from "react";
import { isValidDisplayName } from "@money/shared";
import { useUserProfile } from "@/hooks/useUserProfile";

const inputClass = "field-control w-full";

export function OnboardingFlow() {
  const { completeOnboarding } = useUserProfile();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const canContinue = isValidDisplayName(firstName) && isValidDisplayName(lastName);

  const finish = (displayName?: string) => {
    completeOnboarding(displayName ? { displayName } : {});
  };

  return (
    <div className="mx-auto flex min-h-dvh max-w-md flex-col justify-center p-4">
      <div className="rounded-[28px] border border-outline-variant bg-surface-container-lowest p-5 shadow-[0_12px_32px_rgb(15_110_92_/_0.08)]">
        <h1 className="text-xl font-semibold text-on-surface">Как вас зовут?</h1>
        <label className="mt-4 block space-y-1.5">
          <span className="text-sm text-on-surface-variant">Имя</span>
          <input
            className={inputClass}
            name="given-name"
            autoComplete="given-name"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
          />
        </label>
        <label className="mt-3 block space-y-1.5">
          <span className="text-sm text-on-surface-variant">Фамилия</span>
          <input
            className={inputClass}
            name="family-name"
            autoComplete="family-name"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
          />
        </label>
        <button
          type="button"
          className="pill-btn-primary mt-6 w-full disabled:opacity-50"
          disabled={!canContinue}
          onClick={() => finish(`${firstName.trim()} ${lastName.trim()}`)}
        >
          Продолжить
        </button>
        <button
          type="button"
          className="pill-btn-secondary mt-2 w-full"
          onClick={() => finish()}
        >
          Пропустить
        </button>
      </div>
    </div>
  );
}
