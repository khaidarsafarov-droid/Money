import { badgeLabel, type GamificationState } from "@money/shared";
import { SurfaceCard } from "@/components/ui/SurfaceCard";

interface GamificationBannerProps {
  state: GamificationState;
}

export function GamificationBanner({ state }: GamificationBannerProps) {
  if (state.streakCount <= 0 && state.unlockedBadges.length === 0 && !state.lastWinMessage) {
    return null;
  }

  return (
    <SurfaceCard className="space-y-1 text-sm">
      {state.streakCount > 0 ? (
        <p className="text-milk-text">Серия платежей: {state.streakCount}</p>
      ) : null}
      {state.lastWinMessage ? (
        <p className="text-milk-success">{state.lastWinMessage}</p>
      ) : null}
      {state.unlockedBadges.length > 0 ? (
        <p className="text-xs text-milk-muted">
          {state.unlockedBadges.map(badgeLabel).join(" · ")}
        </p>
      ) : null}
    </SurfaceCard>
  );
}
