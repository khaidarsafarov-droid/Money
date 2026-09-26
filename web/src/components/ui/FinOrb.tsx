interface FinOrbProps {
  className?: string;
}

export function FinOrb({ className = "" }: FinOrbProps) {
  return (
    <div className={`relative mx-auto size-28 ${className}`} aria-hidden>
      <div className="absolute inset-0 rounded-full bg-[#b7f5d8] blur-md" />
      <div className="absolute inset-3 rounded-full bg-gradient-to-br from-[#f3fff8] via-[#5ad7a2] to-[#0c6b56] shadow-[inset_0_-12px_24px_rgb(8_70_56_/_0.35)]" />
      <div className="absolute left-1/2 top-[30%] size-7 -translate-x-1/2 rounded-full bg-white/80 blur-[1px]" />
    </div>
  );
}
