import type { NextConfig } from "next";

// Spring(accounts backend)으로 넘기는 경로는 proxy.ts에 있다.
const nextConfig: NextConfig = {
  async redirects() {
    return [{ source: "/", destination: "/account", permanent: false }];
  },
};

export default nextConfig;
