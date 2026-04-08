"use client";

import React from "react";
import { cn } from "@/lib/utils";

export function InlineNotice({
  tone,
  message
}: {
  tone: "success" | "error";
  message: string;
}) {
  return (
    <div
      className={cn(
        "rounded-xl border px-3 py-2 text-sm",
        tone === "success"
          ? "border-emerald-200 bg-emerald-50 text-emerald-700"
          : "border-rose-200 bg-rose-50 text-rose-700"
      )}
    >
      {message}
    </div>
  );
}
