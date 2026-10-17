interface ScoreRingProps {
  value: number;
  label: string;
  caption: string;
}

export function ScoreRing({ value, label, caption }: ScoreRingProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(value)));
  const radius = 34;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (clamped / 100) * circumference;

  return (
    <section className="flex items-center gap-4 rounded-[28px] bg-gradient-to-br from-[#127863] to-[#0a4a3e] p-5 text-white shadow-[0_18px_40px_rgb(11_83_70_/_0.22)]">
      <div className="min-w-0 flex-1">
        <p className="text-sm text-[#E7F6EF]">{label}</p>
        <p className="mt-1 text-lg font-semibold leading-snug">{caption}</p>
      </div>
      <svg width="92" height="92" viewBox="0 0 92 92" role="img" aria-label={`${clamped} из 100`}>
        <circle cx="46" cy="46" r={radius} fill="none" stroke="rgb(255 255 255 / 0.2)" strokeWidth="8" />
        <circle
          cx="46"
          cy="46"
          r={radius}
          fill="none"
          stroke="#b7f5d4"
          strokeWidth="8"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          transform="rotate(-90 46 46)"
        />
        <text
          x="46"
          y="52"
          textAnchor="middle"
          fill="white"
          fontSize="22"
          fontWeight="600"
          fontFamily="Plus Jakarta Sans, sans-serif"
        >
          {clamped}
        </text>
      </svg>
    </section>
  );
}
