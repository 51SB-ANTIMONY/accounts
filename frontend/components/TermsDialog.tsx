"use client";

import { useEffect, useRef } from "react";
import { CloseIcon } from "./icons";

export type TermId = "tos" | "privacy" | "marketing";

type Term = {
  tag: "[필수]" | "[선택]";
  title: string;
  body: React.ReactNode;
};

function Article({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="term-article">
      <h3>{title}</h3>
      {children}
    </section>
  );
}

function CollectTable({ item, purpose, period }: { item: React.ReactNode; purpose: string; period: string }) {
  return (
    <div className="term-table">
      <table>
        <thead>
          <tr>
            <th scope="col">항목</th>
            <th scope="col">목적</th>
            <th scope="col">보유 기간</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{item}</td>
            <td>{purpose}</td>
            <td>{period}</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

export const TERMS: Record<TermId, Term> = {
  tos: {
    tag: "[필수]",
    title: "이용약관",
    body: (
      <>
        <Article title="제1조 (목적)">
          <p>이 약관은 ANTIMONY(이하 “서비스”)가 제공하는 서비스의 이용 조건과 절차, 서비스와 회원의 권리·의무 및 책임 사항을 정하는 것을 목적으로 합니다.</p>
        </Article>
        <Article title="제2조 (정의)">
          <p>“회원”이란 이 약관에 동의하고 서비스에 가입하여 아이디를 발급받은 사람을 말합니다.</p>
          <p>“아이디”란 회원 식별과 서비스 이용을 위해 회원이 정하고 서비스가 승인한 영문 소문자·숫자 조합을 말합니다.</p>
        </Article>
        <Article title="제3조 (약관의 효력과 변경)">
          <p>이 약관은 서비스 화면에 게시함으로써 효력이 생깁니다. 약관을 변경할 때는 시행일 7일 전부터, 회원에게 불리한 변경은 30일 전부터 서비스 화면에 공지합니다.</p>
        </Article>
        <Article title="제4조 (회원가입)">
          <p>회원가입은 이용자가 약관에 동의하고 가입 정보를 입력한 뒤 이메일 인증을 마치면 완료됩니다. 타인의 정보를 도용하거나 허위 정보를 입력한 경우 가입을 거절하거나 취소할 수 있습니다.</p>
        </Article>
        <Article title="제5조 (회원의 의무)">
          <p>회원은 아이디와 비밀번호를 직접 관리해야 하며, 이를 타인에게 양도하거나 대여할 수 없습니다.</p>
          <p>회원은 관계 법령, 이 약관, 서비스가 공지한 이용 수칙을 지켜야 합니다.</p>
        </Article>
        <Article title="제6조 (이용 제한)">
          <p>회원이 이 약관을 위반하거나 서비스의 정상적인 운영을 방해하면 경고, 일시 정지, 영구 이용 정지 순으로 이용을 제한할 수 있습니다.</p>
        </Article>
        <Article title="제7조 (회원 탈퇴)">
          <p>회원은 언제든지 설정 화면에서 탈퇴를 요청할 수 있으며, 서비스는 즉시 처리합니다.</p>
        </Article>
        <Article title="제8조 (면책)">
          <p>천재지변, 정전, 통신 장애 등 불가항력으로 서비스를 제공할 수 없는 경우 서비스는 책임을 지지 않습니다.</p>
        </Article>
        <Article title="부칙">
          <p>이 약관은 2026년 10월 24일부터 시행합니다.</p>
        </Article>
      </>
    ),
  },
  privacy: {
    tag: "[필수]",
    title: "개인정보 수집·이용 동의",
    body: (
      <>
        <p>ANTIMONY는 회원가입과 서비스 제공을 위해 아래와 같이 개인정보를 수집·이용합니다.</p>
        <CollectTable
          item={
            <>
              <strong>[필수]</strong> 아이디, 비밀번호, 닉네임, 이메일
            </>
          }
          purpose="회원 식별 및 가입 의사 확인, 아이디·비밀번호 찾기, 서비스 관련 공지 전달"
          period="회원 탈퇴 시까지"
        />
        <Article title="비밀번호 보관">
          <p>비밀번호는 복호화할 수 없는 방식으로 암호화하여 저장하며, 운영자도 원래 비밀번호를 알 수 없습니다.</p>
        </Article>
        <Article title="파기">
          <p>보유 기간이 끝나거나 회원이 탈퇴하면 해당 개인정보를 지체 없이 파기합니다. 다만 관계 법령에 따라 보관해야 하는 정보는 그 기간 동안 별도로 보관합니다.</p>
        </Article>
        <Article title="동의를 거부할 권리">
          <p>개인정보 수집·이용 동의를 거부할 수 있습니다. 다만 위 항목은 서비스 제공에 꼭 필요하므로, 동의하지 않으면 회원가입을 할 수 없습니다.</p>
        </Article>
      </>
    ),
  },
  marketing: {
    tag: "[선택]",
    title: "이벤트·소식 수신 동의",
    body: (
      <>
        <p>ANTIMONY의 새 기능, 이벤트, 소식을 이메일로 받아보실 수 있습니다.</p>
        <CollectTable item="이메일" purpose="새 기능 안내, 이벤트·혜택 정보, 서비스 소식 전달" period="동의 철회 또는 회원 탈퇴 시까지" />
        <Article title="수신 거부">
          <p>동의하지 않아도 서비스 이용에는 아무런 제한이 없습니다. 동의한 뒤에도 설정 화면이나 메일 하단의 수신 거부 링크로 언제든 철회할 수 있습니다.</p>
        </Article>
        <Article title="표시 방법">
          <p>광고성 정보를 담은 메일은 제목 앞에 “(광고)”를 표시합니다.</p>
        </Article>
      </>
    ),
  },
};

type Props = {
  term: TermId;
  onClose: () => void;
  onAgree: () => void;
};

// 약관 전문. PC는 가운데 창, 모바일은 아래에서 올라오는 시트로 보여 준다.
export default function TermsDialog({ term, onClose, onAgree }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const { tag, title, body } = TERMS[term];

  useEffect(() => {
    const dialog = ref.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  return (
    <dialog
      ref={ref}
      className="term-dialog"
      aria-labelledby="term-dialog-title"
      onClose={onClose}
      onClick={(e) => {
        // 바깥 어두운 영역을 누르면 닫는다.
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="term-head">
        <h2 id="term-dialog-title">
          <span>{tag}</span> {title}
        </h2>
        <button type="button" className="input-icon-btn term-close" onClick={onClose} aria-label="닫기">
          <CloseIcon />
        </button>
      </div>
      <div className="term-body">{body}</div>
      <div className="term-foot">
        <button type="button" className="btn-ghost" onClick={onClose}>
          닫기
        </button>
        <button type="button" className="btn-primary" onClick={onAgree}>
          동의
        </button>
      </div>
    </dialog>
  );
}
