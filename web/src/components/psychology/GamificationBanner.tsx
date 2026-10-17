import { badgeLabel, resolveWinMessage, streakLabel, type GamificationState } from "@money/shared";
import { SurfaceCard } from "@/components/ui/SurfaceCard";

interface GamificationBannerProps {
  state: GamificationState;
}

export function GamificationBanner({ state }: GamificationBannerProps) {
  if (state.streakCount <= 0 && state.unlockedBadges.length === 0 && !state.lastWinMessage) {
    return null;
  }

  const locale = navigator.language;
  const winMessage = resolveWinMessage(state.lastWinMessage, locale);
  return (
    <SurfaceCard className="space-y-1 text-sm">
      {state.streakCount > 0 ? (
        <p className="text-on-surface">{streakLabel(state.streakCount, locale)}</p>
      ) : null}
      {winMessage ? <p className="text-primary">{winMessage}</p> : null}
      {state.unlockedBadges.length > 0 ? (
        <p className="text-xs text-on-surface-variant">
          {state.unlockedBadges.map((badge) => badgeLabel(badge, locale)).join(" · ")}
        </p>
      ) : null}
    </SurfaceCard>
  );
}
