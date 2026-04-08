import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  // 2026-04-08:
  // UI 层统一走 cn()，不要在组件里手搓字符串拼接。
  // 原因很实际：Tailwind 类名一多，条件分支一多，最容易出的是样式互相覆盖但肉眼不容易看出来。
  // 先 clsx 收敛真假分支，再 twMerge 消掉冲突类，维护成本最低。
  return twMerge(clsx(inputs));
}
