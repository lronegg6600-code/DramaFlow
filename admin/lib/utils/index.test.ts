import { cn } from "@/lib/utils";

describe("cn", () => {
  it("keeps truthy classes and removes falsy ones", () => {
    expect(cn("px-4", false && "hidden", "py-2")).toBe("px-4 py-2");
  });

  it("merges conflicting tailwind classes", () => {
    expect(cn("px-2", "px-4", "text-sm", "text-lg")).toBe("px-4 text-lg");
  });
});
