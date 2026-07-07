import { useState } from "react";
import {
  DEFAULT_DISPLAY_NAME,
  DEFAULT_GOAL_TEXT,
  determinePsychotype,
  isValidAge,
  isValidDisplayName,
  psychotypeLabelRu,
  psychotypeOnboardingDescription,
  resolveDisplayName,
  resolveGoalText,
  type PsychotypeAnswer,
  type UserEmotion,
  type UserFearId,
  type UserGender,
} from "@money/shared";
import { useUserProfile } from "@/hooks/useUserProfile";

const FEAR_OPTIONS: { id: UserFearId; label: string }[] = [
  { id: "wont_escape", label: "Боюсь, что у меня не получится выбраться из долгов" },
  { id: "earn_more_than_parents", label: "Чувствую вину, когда зарабатываю больше родителей" },
  { id: "ashamed_of_debt", label: "Мне стыдно, что я в долгах" },
  { id: "money_corrupts", label: "Боюсь, что деньги меня испортят" },
  { id: "rich_are_bad", label: "Думаю, что богатые — это плохие люди" },
  { id: "afraid_of_numbers", label: "Мне страшно даже смотреть на цифры" },
];

const EMOTIONS: { id: UserEmotion; emoji: string }[] = [
  { id: "HAPPY", emoji: "😊" },
  { id: "NEUTRAL", emoji: "😐" },
  { id: "SAD", emoji: "😔" },
  { id: "ANGRY", emoji: "😡" },
  { id: "THINKING", emoji: "🤔" },
];

interface OnboardingFlowProps {
  onComplete: () => void;
}

export function OnboardingFlow({ onComplete }: OnboardingFlowProps) {
  const { completeOnboarding } = useUserProfile();
  const [step, setStep] = useState(0);
  const [name, setName] = useState("");
  const [gender, setGender] = useState<UserGender>("UNSPECIFIED");
  const [ageText, setAgeText] = useState("");
  const [answers, setAnswers] = useState<PsychotypeAnswer[]>(["a", "a", "a", "a", "a"]);
  const [psychotypeResult, setPsychotypeResult] = useState<ReturnType<typeof determinePsychotype> | null>(null);
  const [fears, setFears] = useState<UserFearId[]>([]);
  const [customFear, setCustomFear] = useState("");
  const [goalText, setGoalText] = useState("");
  const [emotion, setEmotion] = useState<UserEmotion | null>(null);
  const [priority, setPriority] = useState("");

  const totalSteps = 6;
  const parsedAge = ageText ? Number(ageText) : null;
  const displayName = resolveDisplayName(name);

  const finish = () => {
    const psychotype = psychotypeResult ?? determinePsychotype(answers);
    completeOnboarding({
      displayName: resolveDisplayName(name),
      gender,
      age: parsedAge && isValidAge(parsedAge) ? parsedAge : null,
      fears,
      customFear,
      goalText: resolveGoalText(goalText),
      emotion,
      priority,
      psychotype,
    });
    onComplete();
  };

  return (
    <div className="mx-auto flex min-h-screen max-w-lg flex-col bg-[#f7f3ea] p-6">
      <p className="text-sm font-medium text-amber-700">
        Шаг {step + 1} из {totalSteps}
      </p>
      <div className="mb-6 h-2 overflow-hidden rounded-full bg-white">
        <div
          className="h-full bg-emerald-600 transition-all"
          style={{ width: `${((step + 1) / totalSteps) * 100}%` }}
        />
      </div>

      <div className="flex-1 rounded-2xl bg-white p-6 shadow-sm">
        {step === 0 && (
          <>
            <h1 className="text-2xl font-bold">Добро пожаловать в Финансовый Контроль!</h1>
            <p className="mt-4 text-gray-700">
              Здесь вы не просто считаете долги — вы строите путь к финансовой свободе. Давайте познакомимся.
            </p>
            <button type="button" className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white" onClick={() => setStep(1)}>
              Начать знакомство
            </button>
          </>
        )}

        {step === 1 && (
          <>
            <h2 className="text-xl font-bold">Как вас зовут?</h2>
            <input
              className="mt-4 w-full rounded-lg border p-3"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ваше имя"
            />
            <div className="mt-4 flex flex-wrap gap-2">
              {(["MALE", "FEMALE", "UNSPECIFIED"] as UserGender[]).map((g) => (
                <button
                  key={g}
                  type="button"
                  className={`rounded-full px-3 py-1 text-sm ${gender === g ? "bg-emerald-600 text-white" : "bg-gray-100"}`}
                  onClick={() => setGender(g)}
                >
                  {g === "MALE" ? "Мужской" : g === "FEMALE" ? "Женский" : "Не хочу указывать"}
                </button>
              ))}
            </div>
            <input
              className="mt-4 w-full rounded-lg border p-3"
              value={ageText}
              onChange={(e) => setAgeText(e.target.value.replace(/\D/g, "").slice(0, 2))}
              placeholder="Возраст (необязательно)"
            />
            {isValidDisplayName(name) && (
              <p className="mt-4 text-emerald-700">Приятно познакомиться, {name.trim()}!</p>
            )}
            <button
              type="button"
              className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white disabled:opacity-50"
              disabled={!isValidDisplayName(name) || !isValidAge(parsedAge)}
              onClick={() => setStep(2)}
            >
              Далее
            </button>
          </>
        )}

        {step === 2 && (
          <>
            <h2 className="text-xl font-bold">Финансовый психотип</h2>
            {!psychotypeResult ? (
              <>
                <p className="mt-4 text-gray-600">Ответьте на 5 вопросов — результат появится автоматически.</p>
                <button
                  type="button"
                  className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white"
                  onClick={() => {
                    setPsychotypeResult(determinePsychotype(answers));
                  }}
                >
                  Показать результат (демо)
                </button>
              </>
            ) : (
              <>
                <p className="mt-4 font-semibold text-amber-700">{psychotypeLabelRu(psychotypeResult)}</p>
                <p className="mt-2 text-gray-700">{psychotypeOnboardingDescription(psychotypeResult)}</p>
                <button type="button" className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white" onClick={() => setStep(3)}>
                  Далее
                </button>
              </>
            )}
          </>
        )}

        {step === 3 && (
          <>
            <h2 className="text-xl font-bold">Ваши страхи и установки</h2>
            <div className="mt-4 space-y-2">
              {FEAR_OPTIONS.map((fear) => (
                <label key={fear.id} className="flex items-start gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={fears.includes(fear.id)}
                    onChange={(e) => {
                      setFears((current) =>
                        e.target.checked ? [...current, fear.id] : current.filter((id) => id !== fear.id),
                      );
                    }}
                  />
                  <span>{fear.label}</span>
                </label>
              ))}
            </div>
            <input
              className="mt-4 w-full rounded-lg border p-3"
              value={customFear}
              onChange={(e) => setCustomFear(e.target.value)}
              placeholder="Другое"
            />
            <p className="mt-4 text-emerald-700">Мы учтём это. Вы не одиноки в своих переживаниях.</p>
            <button type="button" className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white" onClick={() => setStep(4)}>
              Далее
            </button>
          </>
        )}

        {step === 4 && (
          <>
            <h2 className="text-xl font-bold">Ваша главная цель</h2>
            <input
              className="mt-4 w-full rounded-lg border p-3"
              value={goalText}
              onChange={(e) => setGoalText(e.target.value)}
              placeholder="Например: поехать в путешествие"
            />
            <div className="mt-4 flex flex-wrap gap-2">
              {EMOTIONS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`rounded-full px-3 py-2 text-xl ${emotion === item.id ? "ring-2 ring-emerald-600" : ""}`}
                  onClick={() => setEmotion(item.id)}
                >
                  {item.emoji}
                </button>
              ))}
            </div>
            <input
              className="mt-4 w-full rounded-lg border p-3"
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              placeholder="Что важнее всего? Спокойствие, свобода…"
            />
            <p className="mt-4 text-emerald-700">
              Спасибо, {displayName}! Мы запомнили вашу цель и поможем к ней прийти.
            </p>
            <button type="button" className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white" onClick={() => setStep(5)}>
              Далее
            </button>
          </>
        )}

        {step === 5 && (
          <>
            <h2 className="text-xl font-bold">{displayName}, здравствуйте!</h2>
            <p className="mt-4 text-gray-700">
              Мы подготовили для вас план, основанный на вашем психотипе и целях.
            </p>
            <ul className="mt-4 space-y-2 text-sm text-gray-700">
              <li>Психотип: {psychotypeLabelRu(psychotypeResult ?? "BALANCED")}</li>
              <li>Цель: {resolveGoalText(goalText)}</li>
            </ul>
            <button type="button" className="mt-8 w-full rounded-xl bg-emerald-600 py-3 text-white" onClick={finish}>
              Начать работу
            </button>
          </>
        )}
      </div>
    </div>
  );
}
