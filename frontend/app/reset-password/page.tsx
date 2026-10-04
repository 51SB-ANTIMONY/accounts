import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import ResetForm from "./ResetForm";

export const metadata: Metadata = { title: "Antimony Reset Password" };

export default function ResetPasswordPage() {
  return (
    <AuthShell>
      <ResetForm />
    </AuthShell>
  );
}
