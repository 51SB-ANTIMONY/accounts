import { NextResponse, type NextRequest } from "next/server";

// 화면은 Next.js가, 로그인 처리·API·OIDC는 Spring(accounts backend)이 맡는다.
// 브라우저는 이 주소(localhost:3001, accounts.antimony.kr) 하나만 보고, 아래 matcher의 경로는 Spring으로 넘긴다.
//
// next.config의 rewrites 대신 여기서 넘기는 이유: Spring은 X-Forwarded-Host로 "사용자가 접속한 주소"를 알아
// 리다이렉트 주소(/login 등)를 만든다. Vercel의 rewrites는 이 헤더를 넘기지 않아서, 직접 붙여 보낸다.
const backend = process.env.ACCOUNTS_BACKEND_URL ?? "http://localhost:9000";

export function proxy(request: NextRequest) {
  const { pathname, search, host, protocol } = request.nextUrl;
  const headers = new Headers(request.headers);
  headers.set("x-forwarded-host", request.headers.get("host") ?? host);
  headers.set("x-forwarded-proto", protocol.replace(":", ""));
  // 포트는 Host에 들어 있다(localhost:3001). 따로 붙은 포트(Next.js 서버의 포트)가 섞이지 않게 지운다.
  headers.delete("x-forwarded-port");
  return NextResponse.rewrite(new URL(pathname + search, backend), { request: { headers } });
}

export const config = {
  matcher: [
    "/api/:path*", // 회원가입·계정 찾기·내 계정 API
    "/login/password", // 아이디·비밀번호 로그인 처리
    "/login/oauth2/:path*", // 소셜 로그인 콜백
    "/oauth2/:path*", // 소셜 로그인 시작, OIDC authorize·token·jwks 등
    "/logout", // accounts 로그아웃
    "/connect/:path*", // OIDC 로그아웃
    "/userinfo", // OIDC userinfo
    "/.well-known/:path*", // OIDC discovery
  ],
};
