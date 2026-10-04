import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import SignupForm from "./SignupForm";

export const metadata: Metadata = { title: "Antimony Sign Up" };

export default function SignupPage() {
  return (
    <AuthShell>
      <SignupForm />
    </AuthShell>
  );
}
