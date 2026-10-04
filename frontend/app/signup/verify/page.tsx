import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import VerifyForm from "./VerifyForm";

export const metadata: Metadata = { title: "Antimony Verify Email" };

export default function VerifyPage() {
  return (
    <AuthShell>
      <VerifyForm />
    </AuthShell>
  );
}
