"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Banner from "@/components/Banner";
import CodeField, { EMPTY_CODE_MESSAGE, EXPIRED_MESSAGE } from "@/components/CodeField";
import { api, ApiError } from "@/lib/api";
import { useCountdown } from "@/lib/useCountdown";
import { emailError, maskEmail } from "@/lib/validation";

const CODE_SECONDS = 180;

// 이메일 변경. 1단계 새 이메일로 인증번호 → 2단계 인증번호 확인. 언제든 바꿀 수 있다.
export default function EmailForm() {
  const router = useRouter();
  const [currentEmail, setCurrentEmail] = useState<string | null>(null);
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState("");
  const [tried, setTried] = useState(false);
  const [emailServerError, setEmailServerError] = useState("");
  const [expiresAt, setExpiresAt] = useState<number | null>(null);
  const [code, setCode] = useState("");
  const [codeError, setCodeError] = useState("");
  const [resent, setResent] = useState(false);
  const [errorText, setErrorText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const left = useCountdown(expiresAt, CODE_SECONDS);

  // 로그인하지 않았으면 로그인 화면으로
  useEffect(() => {
    api.me().then(
      (me) => setCurrentEmail(me.email ?? ""),
      (e) => {
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setErrorText(e instanceof ApiError ? e.message : "계정 정보를 불러오지 못했어요.");
      },
    );
  }, [router]);

  const emailErrText = emailServerError || emailError(email);
  const emailErr = (tried || !!emailServerError) && !!emailErrText;

  const sendCode = async (isResend: boolean) => {
    setErrorText("");
    setSubmitting(true);
    try {
      const result = await api.sendEmailChangeCode(email.trim());
      setExpiresAt(Date.now() + result.expiresIn * 1000);
      setCode("");
      setCodeError("");
      setResent(isResend);
      setStep(2);
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors.email) {
        setEmailServerError(e.fieldErrors.email);
        setStep(1);
      } else {
        setErrorText(e instanceof ApiError ? e.message : "인증번호를 보내지 못했어요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const submitEmail = (e: React.FormEvent) => {
    e.preventDefault();
    setTried(true);
    if (!emailErrText) sendCode(false);
  };

  const verify = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorText("");
    if (left === 0) return setCodeError(EXPIRED_MESSAGE);
    if (code.length < 6) return setCodeError(EMPTY_CODE_MESSAGE);
    setSubmitting(true);
    try {
      await api.changeEmail(code);
      router.push("/account?done=email");
    } catch (err) {
      setSubmitting(false);
      setResent(false);
      if (err instanceof ApiError && err.fieldErrors.code) setCodeError(err.fieldErrors.code);
      else if (err instanceof ApiError && err.fieldErrors.email) {
        setEmailServerError(err.fieldErrors.email);
        setStep(1);
      } else setErrorText(err instanceof ApiError ? err.message : "이메일을 바꾸지 못했어요.");
    }
  };

  return (
    <>
      {errorText && <Banner kind="error">{errorText}</Banner>}
      {step === 1 && (
        <form className="stack-28" onSubmit={submitEmail} noValidate>
          <div className="stack-8">
            <h1 className="title">이메일 변경</h1>
            <p className="desc">새 이메일로 인증번호를 보내드려요. 인증을 마치면 이메일이 바뀌어요.</p>
          </div>
          {currentEmail !== null && (
            <div className="note-box">
              <p>
                지금 이메일 <strong>{currentEmail}</strong>
              </p>
              <p className="hint">이메일을 바꾸면 지금 이메일로도 변경 안내를 보내드려요.</p>
            </div>
          )}
          <div className="stack-8">
            <label htmlFor="em-email" className="label">
              새 이메일
            </label>
            <input
              id="em-email"
              className="input"
              type="email"
              autoComplete="email"
              placeholder="name@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setEmailServerError("");
              }}
              aria-invalid={emailErr}
            />
            {emailErr && (
              <p role="alert" className="field-error">
                {emailErrText}
              </p>
            )}
          </div>
          <button type="submit" className="btn-primary" disabled={submitting || currentEmail === null}>
            인증번호 받기
          </button>
          <p className="footer-text">
            <Link href="/account">내 계정으로 돌아가기</Link>
          </p>
        </form>
      )}
      {step === 2 && (
        <form className="stack-28" onSubmit={verify} noValidate>
          <div className="stack-8">
            <h1 className="title">인증번호 입력</h1>
            <p className="desc">
              <strong>{maskEmail(email)}</strong>(으)로 보낸 6자리 인증번호를 입력해 주세요.
            </p>
          </div>
          <CodeField
            code={code}
            onCode={(value) => {
              setCode(value);
              setCodeError("");
              setResent(false);
            }}
            secondsLeft={left}
            error={codeError}
            resent={resent}
            onResend={() => sendCode(true)}
          />
          <button type="submit" className="btn-primary" disabled={submitting}>
            이메일 변경
          </button>
          <p className="footer-text">
            <button
              type="button"
              className="text-link-btn"
              onClick={() => {
                setStep(1);
                setCode("");
                setCodeError("");
                setResent(false);
              }}
            >
              다른 이메일 입력
            </button>
          </p>
        </form>
      )}
    </>
  );
}
