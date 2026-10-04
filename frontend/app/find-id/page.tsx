import type { Metadata } from "next";
import AuthShell from "@/components/AuthShell";
import FindIdForm from "./FindIdForm";

export const metadata: Metadata = { title: "Antimony Find ID" };

export default function FindIdPage() {
  return (
    <AuthShell>
      <FindIdForm />
    </AuthShell>
  );
}
