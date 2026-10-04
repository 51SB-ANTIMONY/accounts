import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import PasswordForm from "./PasswordForm";

export const metadata: Metadata = { title: "Antimony Change Password" };

export default function PasswordPage() {
  return (
    <AuthShell>
      <PasswordForm />
    </AuthShell>
  );
}
