"use client";

import { useMutation, useQueryClient, type QueryKey } from "@tanstack/react-query";
import { useState } from "react";

type NoticeState =
  | { tone: "success"; message: string }
  | { tone: "error"; message: string }
  | null;

type MessageFactory<TData, TVariables> =
  | string
  | ((payload: { data?: TData; variables: TVariables; error?: unknown }) => string);

function resolveMessage<TData, TVariables>(
  factory: MessageFactory<TData, TVariables> | undefined,
  fallback: string,
  payload: { data?: TData; variables: TVariables; error?: unknown }
) {
  if (!factory) {
    return fallback;
  }
  return typeof factory === "function" ? factory(payload) : factory;
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

export function useActionMutation<TData = unknown, TVariables = void>({
  action,
  successMessage,
  errorMessage,
  invalidateKeys = [],
  onSuccess
}: {
  action: (variables: TVariables) => Promise<TData>;
  successMessage?: MessageFactory<TData, TVariables>;
  errorMessage?: MessageFactory<TData, TVariables>;
  invalidateKeys?: QueryKey[];
  onSuccess?: (data: TData, variables: TVariables) => Promise<void> | void;
}) {
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<NoticeState>(null);

  const mutation = useMutation({
    mutationFn: action,
    retry: false
  });

  async function executeAction(variables: TVariables) {
    setNotice(null);

    try {
      const data = await mutation.mutateAsync(variables);

      await Promise.all(
        invalidateKeys.map((queryKey) =>
          queryClient.invalidateQueries({
            queryKey
          })
        )
      );

      if (onSuccess) {
        await onSuccess(data, variables);
      }

      if (successMessage) {
        setNotice({
          tone: "success",
          message: resolveMessage(successMessage, "操作成功。", { data, variables })
        });
      }

      return data;
    } catch (error) {
      setNotice({
        tone: "error",
        message: resolveMessage(
          errorMessage,
          getErrorMessage(error, "操作失败，请稍后重试。"),
          { error, variables }
        )
      });
      throw error;
    }
  }

  return {
    executeAction,
    isSubmitting: mutation.isPending,
    notice,
    clearNotice: () => setNotice(null)
  };
}
