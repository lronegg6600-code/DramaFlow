import { adminFetch, authFetch } from "@/lib/api/client";

describe("api client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("returns data payload for adminFetch", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ data: { id: "drama-1" } })
    }));

    await expect(adminFetch<{ id: string }>("/dramas")).resolves.toEqual({ id: "drama-1" });
  });

  it("throws upstream error message for adminFetch", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ error: { message: "boom" } })
    }));

    await expect(adminFetch("/dramas")).rejects.toThrow("boom");
  });

  it("throws default auth message when authFetch payload has no error body", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({})
    }));

    await expect(authFetch("/login")).rejects.toThrow("Auth request failed");
  });
});
