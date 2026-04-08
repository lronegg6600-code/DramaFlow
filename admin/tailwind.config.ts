import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./features/**/*.{ts,tsx}",
    "./lib/**/*.{ts,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        background: "#f4f6fb",
        foreground: "#0f172a",
        muted: "#64748b",
        surface: "#ffffff",
        accent: "#0f766e",
        accentSoft: "#ccfbf1",
        danger: "#b91c1c",
        border: "#dbe4f0"
      },
      boxShadow: {
        panel: "0 16px 40px rgba(15, 23, 42, 0.08)"
      }
    }
  },
  plugins: []
};

export default config;
