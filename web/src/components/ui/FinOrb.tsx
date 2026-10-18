interface FinOrbProps {
  className?: string;
}

export function FinOrb({ className = "" }: FinOrbProps) {
  return (
    <div className={`relative mx-auto size-28 ${className}`} aria-hidden>
      <div className="absolute inset-0 rounded-full bg-primary-container blur-md" />
      <div className="absolute inset-3 rounded-full bg-gradient-to-br from-surface-container-lowest via-primary-container to-primary shadow-[inset_0_-12px_24px_rgb(0_56_45_/_0.35)]" />
      <div className="absolute left-1/2 top-[30%] size-7 -translate-x-1/2 rounded-full bg-surface-container-lowest/80 blur-[1px]" />
    </div>
  );
}
