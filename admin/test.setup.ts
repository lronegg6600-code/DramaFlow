import React from "react";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// 2026-04-08:
// Vitest 跑页面级测试时，仓库里仍有少量组件沿用 classic JSX runtime。
// 这里显式把 React 挂到全局，先保证测试执行稳定，再逐步把组件收口到统一写法。
(globalThis as typeof globalThis & { React: typeof React }).React = React;

afterEach(() => {
  cleanup();
});
