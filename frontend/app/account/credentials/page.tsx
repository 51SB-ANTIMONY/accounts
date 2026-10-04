import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import CredentialsForm from "./CredentialsForm";

export const metadata: Metadata = { title: "Antimony Set Credentials" };

export default function CredentialsPage() {
  return (
    <AuthShell>
      <CredentialsForm />
    </AuthShell>
  );
}
