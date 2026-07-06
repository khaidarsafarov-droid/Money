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
    <SurfaceCard className="space-y-2 border border-milk-gold/20 bg-milk-bg/80">
      {state.streakCount > 0 ? (
        <p className="text-sm font-semibold text-milk-gold">
          Серия платежей: {state.streakCount}
        </p>
      ) : null}
      {state.lastWinMessage ? (
        <p className="text-sm text-milk-success">{state.lastWinMessage}</p>
      ) : null}
      {state.unlockedBadges.length > 0 ? (
        <p className="text-xs text-milk-muted">
          {state.unlockedBadges.map(badgeLabel).join(" · ")}
        </p>
      ) : null}
    </SurfaceCard>
  );
}
