import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import EmailForm from "./EmailForm";

export const metadata: Metadata = { title: "Antimony Change Email" };

export default function EmailPage() {
  return (
    <AuthShell>
      <EmailForm />
    </AuthShell>
  );
}
