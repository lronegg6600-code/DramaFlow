"use client";

import React, { useState } from "react";
import { useQueryClient, type QueryKey } from "@tanstack/react-query";

export function ConfirmDialog({
  label,
  description,
  successMessage,
  invalidateKeys = [],
  onConfirm
}: {
  label: string;
  description: string;
  successMessage?: string;
  invalidateKeys?: QueryKey[];
  onConfirm: () => Promise<void>;
}) {
  const queryClient = useQueryClient();
  const [isSubmitting, setIsSubmitting] = useState(false);

  return (
    <button
      className="button-secondary"
      disabled={isSubmitting}
      onClick={async () => {
        // 2026-04-08:
        // 这里暂时继续用浏览器原生 confirm，不额外包一层复杂弹窗组件。
        // 原因不是偷懒，而是后台当前最重要的是先把高风险操作拦住，
        // 而不是为了一个确认框引入更多状态同步和可访问性回归风险。
        if (!window.confirm(`${label}\n\n${description}`)) {
          return;
        }

        setIsSubmitting(true);
        try {
          await onConfirm();

          await Promise.all(
            invalidateKeys.map((queryKey) =>
              queryClient.invalidateQueries({
                queryKey
              })
            )
          );

          if (successMessage) {
            window.alert(successMessage);
          }
        } catch (error) {
          window.alert(error instanceof Error ? error.message : `${label}失败，请稍后重试。`);
        } finally {
          setIsSubmitting(false);
        }
      }}
      type="button"
    >
      {isSubmitting ? "处理中..." : label}
    </button>
  );
}
