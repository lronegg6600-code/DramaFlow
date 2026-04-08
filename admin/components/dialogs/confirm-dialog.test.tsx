import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

  function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  }

  return { Wrapper, invalidateSpy };
}

describe("ConfirmDialog", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("does nothing when user cancels confirm", () => {
    const onConfirm = vi.fn();
    const { Wrapper, invalidateSpy } = createWrapper();
    vi.spyOn(window, "confirm").mockReturnValue(false);

    render(<ConfirmDialog description="desc" label="删除" onConfirm={onConfirm} />, {
      wrapper: Wrapper
    });

    fireEvent.click(screen.getByRole("button", { name: "删除" }));

    expect(onConfirm).not.toHaveBeenCalled();
    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it("runs confirm action, invalidates queries and alerts success", async () => {
    const onConfirm = vi.fn().mockResolvedValue(undefined);
    const { Wrapper, invalidateSpy } = createWrapper();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => undefined);

    render(
      <ConfirmDialog
        description="desc"
        invalidateKeys={[["purchases"]]}
        label="重算"
        onConfirm={onConfirm}
        successMessage="ok"
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByRole("button", { name: "重算" }));

    await waitFor(() => {
      expect(onConfirm).toHaveBeenCalled();
    });
    expect(invalidateSpy).toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith("ok");
  });
});
