import type { Metadata } from "next";
import { Suspense } from "react";
import AuthShell from "@/components/AuthShell";
import AccountView from "./AccountView";

export const metadata: Metadata = { title: "Antimony My Account" };

export default function AccountPage() {
  return (
    <AuthShell>
      <Suspense>
        <AccountView />
      </Suspense>
    </AuthShell>
  );
}
