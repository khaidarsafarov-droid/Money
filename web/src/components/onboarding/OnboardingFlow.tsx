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

const EMOTIONS: { id: UserEmotion; label: string }[] = [
  { id: "HAPPY", label: "Радость" },
  { id: "NEUTRAL", label: "Спокойно" },
  { id: "SAD", label: "Грусть" },
  { id: "ANGRY", label: "Злость" },
  { id: "THINKING", label: "Думаю" },
];

const inputClass = "field-control w-full";

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
  const displayName = resolveDisplayName(name, "ru");
  const psychotypeQuestion = PSYCHOTYPE_QUESTIONS[psychotypeQuestionIndex];

  const finish = () => {
    const psychotype = psychotypeResult ?? determinePsychotype(answers);
    completeOnboarding({
      displayName: resolveDisplayName(name, "ru"),
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
    <div className="mx-auto flex min-h-dvh max-w-md flex-col p-4 min-[600px]:max-w-2xl">
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
            <label className="mt-3 block space-y-1.5">
              <span className="text-sm text-on-surface-variant">Имя</span>
              <input
                className={inputClass}
                name="name"
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ваше имя"
              />
            </label>
            <div className="mt-3 flex flex-wrap gap-2">
              {(["MALE", "FEMALE", "UNSPECIFIED"] as UserGender[]).map((g) => (
                <button
                  key={g}
                  type="button"
                  aria-pressed={gender === g}
                  className={gender === g ? "pill-btn-primary text-sm" : "pill-btn-secondary text-sm"}
                  onClick={() => setGender(g)}
                >
                  {g === "MALE" ? "Мужской" : g === "FEMALE" ? "Женский" : "Не хочу указывать"}
                </button>
              ))}
            </div>
            <label className="mt-3 block space-y-1.5">
              <span className="text-sm text-on-surface-variant">Возраст (необязательно)</span>
              <input
                className={inputClass}
                name="age"
                inputMode="numeric"
                autoComplete="off"
                value={ageText}
                onChange={(e) => setAgeText(e.target.value.replace(/\D/g, "").slice(0, 2))}
                placeholder="Возраст"
              />
            </label>
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
            <label className="mt-3 block space-y-1.5">
              <span className="text-sm text-on-surface-variant">Другое</span>
              <input
                className={inputClass}
                name="custom-fear"
                autoComplete="off"
                value={customFear}
                onChange={(e) => setCustomFear(e.target.value)}
                placeholder="Своими словами"
              />
            </label>
            <p className="mt-3 text-sm text-on-surface-variant">Мы учтём это. Вы не одиноки в своих переживаниях.</p>
            <button type="button" className="pill-btn-primary mt-6 w-full" onClick={() => setStep(4)}>
              Далее
            </button>
          </>
        )}

        {step === 4 && (
          <>
            <h2 className="text-lg font-medium text-on-surface">Ваша главная цель</h2>
            <label className="mt-3 block space-y-1.5">
              <span className="text-sm text-on-surface-variant">Цель</span>
              <input
                className={inputClass}
                name="goal"
                autoComplete="off"
                value={goalText}
                onChange={(e) => setGoalText(e.target.value)}
                placeholder="Например: поехать в путешествие"
              />
            </label>
            <p className="mt-3 text-sm text-on-surface-variant">Как вы себя чувствуете</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {EMOTIONS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  aria-pressed={emotion === item.id}
                  className={emotion === item.id ? "pill-btn-primary" : "pill-btn-secondary"}
                  onClick={() => setEmotion(item.id)}
                >
                  {item.label}
                </button>
              ))}
            </div>
            <label className="mt-3 block space-y-1.5">
              <span className="text-sm text-on-surface-variant">Что важнее всего</span>
              <input
                className={inputClass}
                name="priority"
                autoComplete="off"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                placeholder="Спокойствие, свобода…"
              />
            </label>
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
