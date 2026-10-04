"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Banner from "@/components/Banner";
import NewPasswordFields from "@/components/NewPasswordFields";
import { CheckIcon } from "@/components/icons";
import { api, ApiError } from "@/lib/api";
import { ADMIN_UID_PATTERN, passwordConfirmError, passwordError, UID_PATTERN } from "@/lib/validation";

type IdStatus = "empty" | "invalid" | "taken" | "ok";

const ID_MESSAGES: Record<IdStatus, string> = {
  empty: "아이디를 입력해 주세요.",
  invalid: "", // 회원 종류에 따라 다르다 (idRule)
  taken: "이미 사용 중인 아이디예요.",
  ok: "사용 가능한 아이디예요.",
};

// 소셜로만 가입한 회원이 아이디와 비밀번호를 처음 정한다. 아이디는 이때 한 번만 정할 수 있다.
export default function CredentialsForm() {
  const router = useRouter();
  const [currentUid, setCurrentUid] = useState<string | null>(null);
  const [admin, setAdmin] = useState(false);
  const [uid, setUid] = useState("");
  // 중복확인 결과는 확인한 그 아이디에만 유효하다.
  const [idCheck, setIdCheck] = useState<{ uid: string; status: IdStatus } | null>(null);
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [tried, setTried] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});
  const [errorText, setErrorText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 로그인하지 않았으면 로그인 화면, 이미 비밀번호가 있으면 비밀번호 변경 화면으로
  useEffect(() => {
    api.me().then(
      (me) => {
        if (me.hasPassword) router.replace("/account/password");
        else {
          setCurrentUid(me.uid);
          setAdmin(me.adminRole !== null);
        }
      },
      (e) => {
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setErrorText(e instanceof ApiError ? e.message : "계정 정보를 불러오지 못했어요.");
      },
    );
  }, [router]);

  const trimmedUid = uid.trim();
  const idFormatOk = (admin ? ADMIN_UID_PATTERN : UID_PATTERN).test(trimmedUid);
  const idRule = admin ? "영문 소문자·숫자 1–16자 (관리자)" : "영문 소문자·숫자 4–16자";
  const idStatus = idCheck && idCheck.uid === uid ? idCheck.status : null;
  const uidErrText =
    serverErrors.uid ||
    (!trimmedUid
      ? ID_MESSAGES.empty
      : !idFormatOk
        ? `${idRule}로 입력해 주세요.`
        : idStatus === "taken"
          ? ID_MESSAGES.taken
          : idStatus !== "ok"
            ? "아이디 중복확인을 해 주세요."
            : "");
  const idShowErr = !!serverErrors.uid || (!!idStatus && idStatus !== "ok") || (tried && !!uidErrText);
  const idMsg = serverErrors.uid
    ? serverErrors.uid
    : idStatus === "ok"
      ? ID_MESSAGES.ok
      : idStatus
        ? idStatus === "invalid"
          ? `${idRule}로 입력해 주세요.`
          : ID_MESSAGES[idStatus]
        : tried && uidErrText
          ? uidErrText
          : idRule;
  const valid = !uidErrText && !(serverErrors.password || passwordError(pw)) && !passwordConfirmError(pw, pw2);

  const checkId = async () => {
    setErrorText("");
    setServerErrors({ ...serverErrors, uid: "" });
    if (!trimmedUid) return setIdCheck({ uid, status: "empty" });
    if (!idFormatOk) return setIdCheck({ uid, status: "invalid" });
    try {
      const result = await api.checkMyUid(trimmedUid);
      setIdCheck({ uid, status: result.status });
    } catch (e) {
      setErrorText(e instanceof ApiError ? e.message : "중복확인을 하지 못했어요.");
    }
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTried(true);
    setErrorText("");
    if (!valid) return;
    setSubmitting(true);
    try {
      await api.setCredentials(trimmedUid, pw);
      router.push("/account?done=credentials");
    } catch (err) {
      setSubmitting(false);
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setServerErrors(err.fieldErrors);
        if (err.fieldErrors.uid) setIdCheck(null);
      } else {
        setErrorText(err instanceof ApiError ? err.message : "아이디와 비밀번호를 설정하지 못했어요.");
      }
    }
  };

  return (
    <>
      <div className="stack-8">
        <h1 className="title">아이디·비밀번호 설정</h1>
        <p className="desc">아이디와 비밀번호를 정하면 소셜 계정 없이도 로그인할 수 있어요.</p>
      </div>
      {currentUid && (
        <div className="note-box">
          <p>
            지금 아이디 <strong>{currentUid}</strong> (자동으로 만든 아이디)
          </p>
          <p className="hint">아이디는 지금 한 번만 정할 수 있어요. 설정한 뒤에는 바꿀 수 없어요.</p>
        </div>
      )}
      {errorText && <Banner kind="error">{errorText}</Banner>}
      <form className="stack-28" onSubmit={submit} noValidate>
        <div className="stack-18">
          <div className="stack-8">
            <label htmlFor="cr-id" className="label">
              새 아이디
            </label>
            <div className="input-row">
              <input
                id="cr-id"
                className="input"
                type="text"
                autoComplete="username"
                placeholder="아이디 입력"
                value={uid}
                onChange={(e) => {
                  setUid(e.target.value);
                  setServerErrors({ ...serverErrors, uid: "" });
                }}
                aria-invalid={idShowErr}
                aria-describedby="cr-id-msg"
              />
              <button type="button" className="btn-accent" onClick={checkId}>
                중복확인
              </button>
            </div>
            <p id="cr-id-msg" aria-live="polite" className={`field-msg${idStatus === "ok" && !serverErrors.uid ? " is-ok" : idShowErr ? " is-error" : ""}`}>
              {idStatus === "ok" && !serverErrors.uid && <CheckIcon />}
              {idMsg}
            </p>
          </div>
          <NewPasswordFields
            idPrefix="cr"
            labels={["비밀번호", "비밀번호 확인"]}
            password={pw}
            confirm={pw2}
            onPassword={(value) => {
              setPw(value);
              setServerErrors({ ...serverErrors, password: "" });
            }}
            onConfirm={setPw2}
            tried={tried}
            serverError={serverErrors.password}
          />
        </div>
        <button type="submit" className="btn-primary" disabled={submitting || !currentUid}>
          설정 완료
        </button>
      </form>
      <p className="footer-text">
        <Link href="/account">내 계정으로 돌아가기</Link>
      </p>
    </>
  );
}
