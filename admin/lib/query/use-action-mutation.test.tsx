import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, act } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { useActionMutation } from "@/lib/query/use-action-mutation";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useActionMutation", () => {
  it("stores success notice and runs onSuccess", async () => {
    const onSuccess = vi.fn();
    const wrapper = createWrapper();
    const { result } = renderHook(
      () =>
        useActionMutation({
          action: async (value: string) => ({ value }),
          successMessage: ({ variables }) => `${variables} done`,
          onSuccess
        }),
      { wrapper }
    );

    await act(async () => {
      await result.current.executeAction("save");
    });

    expect(onSuccess).toHaveBeenCalled();
    expect(result.current.notice).toEqual({
      tone: "success",
      message: "save done"
    });
  });

  it("stores error notice and rethrows when action fails", async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(
      () =>
        useActionMutation({
          action: async () => {
            throw new Error("boom");
          }
        }),
      { wrapper }
    );

    await act(async () => {
      await expect(result.current.executeAction(undefined)).rejects.toThrow("boom");
    });

    expect(result.current.notice).toEqual({
      tone: "error",
      message: "boom"
    });
  });
});
