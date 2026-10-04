"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Banner from "@/components/Banner";
import NewPasswordFields from "@/components/NewPasswordFields";
import PasswordInput from "@/components/PasswordInput";
import { api, ApiError } from "@/lib/api";
import { passwordConfirmError, passwordError } from "@/lib/validation";

// 비밀번호가 있는 회원이 로그인한 채로 비밀번호를 바꾼다. 현재 비밀번호를 확인한다.
export default function PasswordForm() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [current, setCurrent] = useState("");
  const [currentShown, setCurrentShown] = useState(false);
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [tried, setTried] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});
  const [errorText, setErrorText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 로그인하지 않았으면 로그인 화면, 비밀번호가 없으면 아이디·비밀번호 설정 화면으로
  useEffect(() => {
    api.me().then(
      (me) => {
        if (!me.hasPassword) router.replace("/account/credentials");
        else setReady(true);
      },
      (e) => {
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setErrorText(e instanceof ApiError ? e.message : "계정 정보를 불러오지 못했어요.");
      },
    );
  }, [router]);

  const currentErrText = serverErrors.currentPassword || (!current ? "현재 비밀번호를 입력해 주세요." : "");
  const currentErr = (tried || !!serverErrors.currentPassword) && !!currentErrText;
  const newErrText = serverErrors.newPassword || (pw && pw === current ? "지금 비밀번호와 다른 비밀번호로 정해 주세요." : "");
  const valid = !currentErrText && !newErrText && !passwordError(pw) && !passwordConfirmError(pw, pw2);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTried(true);
    setErrorText("");
    if (!valid) return;
    setSubmitting(true);
    try {
      await api.changePassword(current, pw);
      router.push("/account?done=password");
    } catch (err) {
      setSubmitting(false);
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setServerErrors(err.fieldErrors);
      } else {
        setErrorText(err instanceof ApiError ? err.message : "비밀번호를 바꾸지 못했어요.");
      }
    }
  };

  return (
    <>
      <div className="stack-8">
        <h1 className="title">비밀번호 변경</h1>
        <p className="desc">지금 비밀번호를 확인한 뒤 새 비밀번호로 바꿔요.</p>
      </div>
      {errorText && <Banner kind="error">{errorText}</Banner>}
      <form className="stack-28" onSubmit={submit} noValidate>
        <div className="stack-18">
          <div className="stack-8">
            <label htmlFor="pc-cur" className="label">
              현재 비밀번호
            </label>
            <PasswordInput
              id="pc-cur"
              value={current}
              onChange={(value) => {
                setCurrent(value);
                setServerErrors({ ...serverErrors, currentPassword: "" });
              }}
              shown={currentShown}
              onToggle={() => setCurrentShown(!currentShown)}
              invalid={currentErr}
              autoComplete="current-password"
              placeholder="현재 비밀번호 입력"
            />
            {currentErr && (
              <p role="alert" className="field-error">
                {currentErrText}
              </p>
            )}
          </div>
          <p className="field-help">
            비밀번호가 기억나지 않나요? <Link href="/reset-password">비밀번호 찾기</Link>
          </p>
          <NewPasswordFields
            idPrefix="pc"
            labels={["새 비밀번호", "새 비밀번호 확인"]}
            password={pw}
            confirm={pw2}
            onPassword={(value) => {
              setPw(value);
              setServerErrors({ ...serverErrors, newPassword: "" });
            }}
            onConfirm={setPw2}
            tried={tried}
            emptyMessage="새 비밀번호를 입력해 주세요."
            serverError={newErrText}
          />
        </div>
        <button type="submit" className="btn-primary" disabled={submitting || !ready}>
          비밀번호 변경
        </button>
      </form>
      <p className="footer-text">
        <Link href="/account">내 계정으로 돌아가기</Link>
      </p>
    </>
  );
}
