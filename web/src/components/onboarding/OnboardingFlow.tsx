import { useState } from "react";
import {
  determinePsychotype,
  isValidAge,
  isValidDisplayName,
  PSYCHOTYPE_QUESTIONS,
  psychotypeLabelRu,
  psychotypeOnboardingDescription,
  resolveDisplayName,
  resolveGoalText,
  type PsychotypeAnswer,
  type UserEmotion,
  type UserFearId,
  type UserGender,
} from "@money/shared";
import { FinOrb } from "@/components/ui/FinOrb";
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

const inputClass =
  "w-full rounded-2xl border border-outline-variant bg-background px-3 py-2.5 text-sm text-on-surface outline-none focus:border-primary";

export function OnboardingFlow() {
  const { completeOnboarding } = useUserProfile();
  const [step, setStep] = useState(0);
  const [name, setName] = useState("");
  const [gender, setGender] = useState<UserGender>("UNSPECIFIED");
  const [ageText, setAgeText] = useState("");
  const [answers, setAnswers] = useState<PsychotypeAnswer[]>(["a", "a", "a", "a", "a"]);
  const [psychotypeQuestionIndex, setPsychotypeQuestionIndex] = useState(0);
  const [psychotypeResult, setPsychotypeResult] = useState<ReturnType<typeof determinePsychotype> | null>(null);
  const [fears, setFears] = useState<UserFearId[]>([]);
  const [customFear, setCustomFear] = useState("");
  const [goalText, setGoalText] = useState("");
  const [emotion, setEmotion] = useState<UserEmotion | null>(null);
  const [priority, setPriority] = useState("");

  const totalSteps = 6;
  const parsedAge = ageText ? Number(ageText) : null;
  const displayName = resolveDisplayName(name, navigator.language);
  const psychotypeQuestion = PSYCHOTYPE_QUESTIONS[psychotypeQuestionIndex];

  const finish = () => {
    const psychotype = psychotypeResult ?? determinePsychotype(answers);
    completeOnboarding({
      displayName: resolveDisplayName(name, navigator.language),
      gender,
      age: parsedAge && isValidAge(parsedAge) ? parsedAge : null,
      fears,
      customFear,
      goalText: resolveGoalText(goalText),
      emotion,
      priority,
      psychotype,
    });
  };

  const selectPsychotypeAnswer = (key: PsychotypeAnswer) => {
    setAnswers((current) => {
      const next = [...current];
      next[psychotypeQuestionIndex] = key;
      return next;
    });
  };

  const advancePsychotypeQuestion = () => {
    if (psychotypeQuestionIndex < PSYCHOTYPE_QUESTIONS.length - 1) {
      setPsychotypeQuestionIndex((index) => index + 1);
      return;
    }
    setPsychotypeResult(determinePsychotype(answers));
  };

  return (
    <div className="mx-auto flex min-h-dvh max-w-md flex-col p-4">
      <p className="text-sm font-medium text-on-surface-variant">
        Шаг {step + 1} из {totalSteps}
      </p>
      <div className="mb-4 h-1.5 overflow-hidden rounded-full bg-surface-variant">
        <div
          className="h-full rounded-full bg-primary transition-all"
          style={{ width: `${((step + 1) / totalSteps) * 100}%` }}
        />
      </div>

      <div className="flex-1 rounded-[28px] border border-outline-variant bg-surface-container-lowest p-5 shadow-[0_12px_32px_rgb(15_110_92_/_0.08)]">
        {step === 0 && (
          <>
            <FinOrb className="mb-5" />
            <h1 className="text-xl font-semibold text-on-surface">Добро пожаловать в Финансовый Контроль</h1>
            <p className="mt-3 text-sm text-on-surface-variant">
              Здесь вы не просто считаете долги — вы строите путь к финансовой свободе.
            </p>
            <button type="button" className="pill-btn-primary mt-6 w-full" onClick={() => setStep(1)}>
              Начать
            </button>
          </>
        )}

        {step === 1 && (
          <>
            <h2 className="text-lg font-medium text-on-surface">Как вас зовут?</h2>
            <input
              className={`${inputClass} mt-3`}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ваше имя"
            />
            <div className="mt-3 flex flex-wrap gap-1.5">
              {(["MALE", "FEMALE", "UNSPECIFIED"] as UserGender[]).map((g) => (
                <button
                  key={g}
                  type="button"
                  className={gender === g ? "pill-btn-primary text-sm" : "pill-btn-secondary text-sm"}
                  onClick={() => setGender(g)}
                >
                  {g === "MALE" ? "Мужской" : g === "FEMALE" ? "Женский" : "Не хочу указывать"}
                </button>
              ))}
            </div>
            <input
              className={`${inputClass} mt-3`}
              value={ageText}
              onChange={(e) => setAgeText(e.target.value.replace(/\D/g, "").slice(0, 2))}
              placeholder="Возраст (необязательно)"
            />
            {isValidDisplayName(name) && (
              <p className="mt-3 text-sm text-primary">Приятно познакомиться, {name.trim()}!</p>
            )}
            <button
              type="button"
              className="pill-btn-primary mt-6 w-full disabled:opacity-50"
              disabled={!isValidDisplayName(name) || !isValidAge(parsedAge)}
              onClick={() => setStep(2)}
            >
              Далее
            </button>
          </>
        )}

        {step === 2 && (
          <>
            <h2 className="text-lg font-medium text-on-surface">Финансовый психотип</h2>
            {!psychotypeResult ? (
              <>
                <p className="mt-1 text-sm text-on-surface-variant">
                  Вопрос {psychotypeQuestionIndex + 1} из {PSYCHOTYPE_QUESTIONS.length}
                </p>
                <p className="mt-3 text-sm text-on-surface">{psychotypeQuestion?.text}</p>
                <div className="mt-3 space-y-1.5">
                  {psychotypeQuestion?.options.map((option) => (
                    <label key={option.key} className="flex cursor-pointer items-start gap-2 rounded-md border border-outline-variant p-2.5 text-sm">
                      <input
                        type="radio"
                        name={`psychotype-q-${psychotypeQuestionIndex}`}
                        checked={answers[psychotypeQuestionIndex] === option.key}
                        onChange={() => selectPsychotypeAnswer(option.key)}
                        className="accent-on-surface"
                      />
                      <span>{option.label}</span>
                    </label>
                  ))}
                </div>
                <button
                  type="button"
                  className="pill-btn-primary mt-6 w-full"
                  onClick={advancePsychotypeQuestion}
                >
                  {psychotypeQuestionIndex < PSYCHOTYPE_QUESTIONS.length - 1 ? "Далее" : "Показать результат"}
                </button>
              </>
            ) : (
              <>
                <p className="mt-3 font-medium text-on-surface">{psychotypeLabelRu(psychotypeResult)}</p>
                <p className="mt-2 text-sm text-on-surface-variant">{psychotypeOnboardingDescription(psychotypeResult)}</p>
                <button type="button" className="pill-btn-primary mt-6 w-full" onClick={() => setStep(3)}>
                  Далее
                </button>
              </>
            )}
          </>
        )}

        {step === 3 && (
          <>
            <h2 className="text-lg font-medium text-on-surface">Ваши страхи и установки</h2>
            <div className="mt-3 space-y-2">
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
                    className="mt-0.5 accent-on-surface"
                  />
                  <span>{fear.label}</span>
                </label>
              ))}
            </div>
            <input
              className={`${inputClass} mt-3`}
              value={customFear}
              onChange={(e) => setCustomFear(e.target.value)}
              placeholder="Другое"
            />
            <p className="mt-3 text-sm text-on-surface-variant">Мы учтём это. Вы не одиноки в своих переживаниях.</p>
            <button type="button" className="pill-btn-primary mt-6 w-full" onClick={() => setStep(4)}>
              Далее
            </button>
          </>
        )}

        {step === 4 && (
          <>
            <h2 className="text-lg font-medium text-on-surface">Ваша главная цель</h2>
            <input
              className={`${inputClass} mt-3`}
              value={goalText}
              onChange={(e) => setGoalText(e.target.value)}
              placeholder="Например: поехать в путешествие"
            />
            <div className="mt-3 flex flex-wrap gap-1.5">
              {EMOTIONS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`rounded-md px-2.5 py-1.5 text-lg ${emotion === item.id ? "ring-1 ring-on-surface" : ""}`}
                  onClick={() => setEmotion(item.id)}
                >
                  {item.emoji}
                </button>
              ))}
            </div>
            <input
              className={`${inputClass} mt-3`}
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              placeholder="Что важнее всего? Спокойствие, свобода…"
            />
            <p className="mt-3 text-sm text-on-surface-variant">
              Спасибо, {displayName}! Мы запомнили вашу цель.
            </p>
            <button type="button" className="pill-btn-primary mt-6 w-full" onClick={() => setStep(5)}>
              Далее
            </button>
          </>
        )}

        {step === 5 && (
          <>
            <FinOrb className="mb-5" />
            <h2 className="text-lg font-semibold text-on-surface">{displayName}, здравствуйте!</h2>
            <p className="mt-3 text-sm text-on-surface-variant">
              Мы подготовили для вас план, основанный на вашем психотипе и целях.
            </p>
            <ul className="mt-3 space-y-1 text-sm text-on-surface-variant">
              <li>Психотип: {psychotypeLabelRu(psychotypeResult ?? "BALANCED")}</li>
              <li>Цель: {resolveGoalText(goalText)}</li>
            </ul>
            <button type="button" className="pill-btn-primary mt-6 w-full" onClick={finish}>
              Начать работу
            </button>
          </>
        )}
      </div>
    </div>
  );
}
